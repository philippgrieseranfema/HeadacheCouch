package com.happyheadache.home;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.JsonWriter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.happyheadache.R;
import com.happyheadache.data.GoogleFitManager;
import com.happyheadache.data.SHealthManager;
import com.happyheadache.models.Exercise;
import com.happyheadache.models.Food;
import com.happyheadache.models.GoogleFitData;
import com.happyheadache.models.SHealthData;
import com.happyheadache.models.SensorData;
import com.happyheadache.models.Sleep;
import com.happyheadache.models.WeatherLibData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.happyheadache.Constants.APP_TAG;
import static com.happyheadache.Constants.PREFERENCE_GOOGLE_FIT_DISABLED;
import static com.happyheadache.Constants.PREFERENCE_S_HEALTH_DISABLED;

public class StatsFragment extends Fragment {

    private boolean isZoomedIn;
    private WebView mWebView;
    private FragmentActivity mActivity;
    private View mView;
    private FloatingActionButton mZoomButton;
    private FirebaseAnalytics mFirebaseAnalytics;

    public StatsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mView = inflater.inflate(R.layout.fragment_stats, container, false);

        mActivity = getActivity();

        // Send info button click to javascript file
        FloatingActionButton infoButton = (FloatingActionButton) mView.findViewById(R.id.floatingactionbutton_home_info);
        infoButton.setOnClickListener(infoButtonListener);

        // Send zoom button click to javascript file
        mZoomButton = (FloatingActionButton) mView.findViewById(R.id.floatingactionbutton_home_zoom);
        mZoomButton.setOnClickListener(zoomButtonListener);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(getContext());

        return mView;
    }

    @Override
    public void onResume() {
        getData();

        // Reset zoom button
        if (isZoomedIn) {
            mZoomButton.performClick();
        }

        super.onResume();
    }

    private final View.OnClickListener infoButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "info_button");
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

            mWebView.loadUrl("javascript:infoButtonClick();");
        }
    };

    private final View.OnClickListener zoomButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "zoom_button");
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

            isZoomedIn = !isZoomedIn;
            mWebView.loadUrl("javascript:zoomButtonClick();");
            if (!isZoomedIn) {
                ((FloatingActionButton) v).setImageResource(R.drawable.ic_zoom_in_accent_24dp);
            } else {
                ((FloatingActionButton) v).setImageResource(R.drawable.ic_zoom_out_accent_24dp);
            }
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    private void getData() {
        mWebView = (WebView) mView.findViewById(R.id.webview_home);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        // Make web view fit screen
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setDefaultFontSize(30);

        // Load external html file with hGraph view & files
        mWebView.loadUrl("file:///android_asset/hGraph/index.html");

        new Thread(new Runnable() {
            public void run() {
                try {
                    CountDownLatch sHealthManagerCountDownLatch = null, googleFitManagerCountDownLatch = null;

                    SHealthManager sHealthManager = null;
                    GoogleFitManager googleFitManager = null;

                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

                    // Only start data collection job if user didn't manually disable s health
                    boolean sHealthDisabled = sharedPreferences.getBoolean(PREFERENCE_S_HEALTH_DISABLED, false);
                    if (!sHealthDisabled) {
                        if (Looper.myLooper() == null) {
                            Looper.prepare();
                        }

                        sHealthManager = new SHealthManager();
                        sHealthManagerCountDownLatch = new CountDownLatch(sHealthManager.mKeySet.size());
                        sHealthManager.startGetDataForToday(getContext(), sHealthManagerCountDownLatch);
                    }

                    // Only start data collection job if user didn't manually disable google fit
                    boolean googleFitDisabled = sharedPreferences.getBoolean(PREFERENCE_GOOGLE_FIT_DISABLED, false);
                    if (!googleFitDisabled) {
                        if (Looper.myLooper() == null) {
                            Looper.prepare();
                        }

                        googleFitManager = new GoogleFitManager();
                        googleFitManagerCountDownLatch = new CountDownLatch(5);
                        googleFitManager.startGetDataForToday(getContext(), googleFitManagerCountDownLatch);
                    }

                    // Wait until both jobs are finished
                    SHealthData sHealthData = null;
                    if (sHealthManager != null) {
                        if (sHealthManagerCountDownLatch.getCount() > 0) {
                            sHealthManagerCountDownLatch.await();
                        }

                        Log.d(APP_TAG, "S Health Manager data collection done.");

                        // Save data to database
                        sHealthData = sHealthManager.getSHealthData();
                        sHealthManager.disconnect();
                    }

                    GoogleFitData googleFitData = null;
                    if (googleFitManager != null) {
                        if (googleFitManagerCountDownLatch.getCount() > 0) {
                            googleFitManagerCountDownLatch.await();
                        }
                        googleFitManagerCountDownLatch.await();

                        Log.d(APP_TAG, "Google Fit Manager data collection done.");

                        // Save data to database
                        googleFitData = googleFitManager.getGoogleFitData();
                        googleFitManager.disconnect();
                    }


                    Log.e(APP_TAG, "Testing method");
                    final File file = new File(getContext().getFilesDir(), "test.json");

                    // Create json file based on today's data
                    OutputStream fileOutputStream = new FileOutputStream(file);
                    createJson(fileOutputStream, sHealthData, googleFitData, null, null);

                    mActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            mWebView.loadUrl("javascript:reloadData(\"" + file.getAbsolutePath() + "\");");
                        }
                    });

                } catch (InterruptedException | IOException ignored) {
                    Log.d(APP_TAG, ignored.getMessage());
                }
            }
        }).start();
    }

    private void createJson(OutputStream out, SHealthData sHealthData, GoogleFitData googleFitData, WeatherLibData weatherLibData, SensorData sensorData) throws IOException {
        JsonWriter writer = new JsonWriter(new OutputStreamWriter(out, "UTF-8"));
        writer.setIndent("  ");
        writer.beginArray();
        writer.beginObject();
        writer.name("gender").value("male");
        writer.name("metrics");
        writer.beginArray();

        writeHealthData(writer, sHealthData, googleFitData);
        // TODO: Include sensor, weather, and other health center data

        writer.endArray();
        writer.endObject();
        writer.endArray();
        writer.close();
    }

    public void writeHealthData(JsonWriter writer, SHealthData sHealthData, GoogleFitData googleFitData) throws IOException {
        if (sHealthData == null) {
            sHealthData = new SHealthData();
        }
        if (googleFitData == null) {
            googleFitData = new GoogleFitData();
        }

        // TODO: Find a reliable source for min/max information + do calculations based on gender, weight, size, dietary goal, etc.

        // Set calories eaten - Source for recommended values: https://ods.od.nih.gov/Health_Information/Dietary_Reference_Intakes.aspx
        if (sHealthData.getFoods() == null) {
            sHealthData.setFoods(new ArrayList<Food>());
        }
        int caloriesEaten = 0;
        int fat = 0;
        int carbohydrate = 0;
        int fiber = 0;
        int sugar = 0;
        int protein = 0;
        int sodium = 0;
        int potassium = 0;
        int vitaminA = 0;
        int vitaminC = 0;
        int calcium = 0;
        int iron = 0;
        for (Food food : sHealthData.getFoods()) {
            caloriesEaten += food.getCalories();
            fat += food.getTotalFat();
            carbohydrate += food.getCalories();
            fiber += food.getDietaryFiber();
            sugar += food.getSugar();
            protein += food.getProtein();
            sodium += food.getSodium();
            potassium += food.getPotassium();
            vitaminA += food.getVitaminA();
            vitaminC += food.getVitaminC();
            calcium += food.getCalcium();
            iron += food.getIron();
        }
        startWriteSuperEntry(writer, getString(R.string.stats_food), caloriesEaten, getString(R.string.stats_foodunit));
        writeEntry(writer, getString(R.string.stats_calories), caloriesEaten, 2000, 3000, 0, 5000, 10, getString(R.string.stats_caloriesunit));
        writeEntry(writer, getString(R.string.stats_fat), fat, 50, 80, 0, 1000, 10, getString(R.string.stats_fatunit));
        writeEntry(writer, getString(R.string.stats_carbohydrate), carbohydrate, 250, 400, 0, 1000, 10, getString(R.string.stats_carbohydrateunit));
        writeEntry(writer, getString(R.string.stats_fiber), fiber, 25, 40, 0, 100, 10, getString(R.string.stats_fiberunit));
        writeEntry(writer, getString(R.string.stats_sugar), sugar, 70, 100, 0, 1000, 10, getString(R.string.stats_sugarunit));
        writeEntry(writer, getString(R.string.stats_protein), protein, 45, 60, 0, 100, 10, getString(R.string.stats_proteinunit));
        writeEntry(writer, getString(R.string.stats_sodium), sodium, 1400, 2200, 0, 5000, 10, getString(R.string.stats_sodiumunit));
        writeEntry(writer, getString(R.string.stats_potassium), potassium, 4500, 5000, 0, 10000, 10, getString(R.string.stats_potassiumunit));
        stopWriteSuperEntry(writer);

        // Set step count
        int steps = Math.max(sHealthData.getSteps(), googleFitData.getSteps());
        writeEntry(writer, getString(R.string.stats_steps), steps, 8000, 20000, 0, 50000, 10, getString(R.string.stats_stepsunit));

        // Set caffeine intake
        int caffeine = sHealthData.getCaffeine();
        writeEntry(writer, getString(R.string.stats_caffeine), caffeine, 0, 400, 0, 2000, 10, getString(R.string.stats_caffeinunit));

        // Set water intake - https://www.ncbi.nlm.nih.gov/books/NBK56068/table/summarytables.t4/?report=objectonly
        int water = sHealthData.getWater();
        writeEntry(writer, getString(R.string.stats_water), water, 2000, 4000, 0, 6000, 10, getString(R.string.stats_waterunit));

        if (sHealthData.getExercises() == null) {
            sHealthData.setExercises(new ArrayList<Exercise>());
        }

        // Set minutes of exercise calories expended
        int minutesExercise = 0;
        int caloriesExercise = 0;
        int distanceExercise = 0;
        for (Exercise exercise : sHealthData.getExercises()) {
            minutesExercise += TimeUnit.MILLISECONDS.toMinutes(exercise.getDuration());
            caloriesExercise += exercise.getCalories();
            distanceExercise += exercise.getDistance();
        }
        startWriteSuperEntry(writer, getString(R.string.stats_exercise), minutesExercise, getString(R.string.stats_exerciseunit));
        writeEntry(writer, getString(R.string.stats_duration), minutesExercise, 60, 240, 0, 500, 10, getString(R.string.stats_duratiounit));
        writeEntry(writer, getString(R.string.stats_burnedcalories), caloriesExercise, 300, 500, 0, 1000, 10, getString(R.string.stats_burnedcaloriesunit));
        writeEntry(writer, getString(R.string.stats_distance), distanceExercise, 1000, 3000, 0, 100000, 10, getString(R.string.stats_distanceunit));
        stopWriteSuperEntry(writer);

        if (sHealthData.getSleep() == null) {
            sHealthData.setSleep(new Sleep());
        }
        // Set sleep duration
        int sleep = Long.valueOf(TimeUnit.MILLISECONDS.toHours(sHealthData.getSleep().getEnd() - sHealthData.getSleep().getStart())).intValue();
        writeEntry(writer, getString(R.string.stats_sleep), sleep, 7, 9, 0, 12, 10, getString(R.string.stats_sleepunit));
    }

    public void startWriteSuperEntry(JsonWriter writer, String name, int value, String unitLabel) throws IOException {
        writer.beginObject();
        writer.name("name").value(name);
        writer.name("value").value(value);

        writer.name("features");
        writer.beginObject();
        writer.name("unitlabel").value(unitLabel);
        writer.endObject();

        writer.name("details");
        writer.beginArray();
    }

    public void stopWriteSuperEntry(JsonWriter writer) throws IOException {
        writer.endArray();

        writer.endObject();
    }

    public void writeEntry(JsonWriter writer, String name, int value, int healthyRangeMin, int healthyRangeMax, int totalRangeMin, int totalRangeMax, int weight, String unitLabel) throws IOException {
        writer.beginObject();
        writer.name("name").value(name);
        writer.name("value").value(value);
        writer.name("features");
        writer.beginObject();

        writer.name("healthyrange");
        writer.beginArray();
        writer.value(healthyRangeMin);
        writer.value(healthyRangeMax);
        writer.endArray();

        writer.name("totalrange");
        writer.beginArray();
        writer.value(totalRangeMin);
        writer.value(totalRangeMax);
        writer.endArray();

        writer.name("boundayflags");
        writer.beginArray();
        writer.value(false);
        writer.value(true);
        writer.endArray();

        writer.name("weight").value(weight);
        writer.name("unitlabel").value(unitLabel);
        writer.endObject();

        writer.endObject();
    }
}
