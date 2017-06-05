package com.happyheadache.data;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.happyheadache.models.AmbientTemperature;
import com.happyheadache.models.Exercise;
import com.happyheadache.models.Food;
import com.happyheadache.models.SHealthData;
import com.happyheadache.models.Sleep;
import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult;
import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthDataResolver;
import com.samsung.android.sdk.healthdata.HealthDataService;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthPermissionManager;
import com.samsung.android.sdk.healthdata.HealthResultHolder;
import com.samsung.android.sdk.healthdata.HealthUserProfile;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static com.happyheadache.Constants.APP_TAG;

/**
 * Created by Alexandra Fritzen on 17/10/2016.
 */

public class SHealthManager {

    public interface SHealthDataListener {
        void sHealthConnectedAndPermissionsAcquired();

        void sHealthPermissionFailed();
    }

    private HealthDataStore mStore;
    private HealthConnectionErrorResult mConnError;
    public final Set<HealthPermissionManager.PermissionKey> mKeySet;
    private SHealthDataListener mListener;
    private boolean mShowPermissionDialogue;
    private boolean mAlwaysShowPermissionDialogue;
    private CountDownLatch mCountDownLatch;
    private HealthDataService mHealthDataService;
    private SHealthData mSHealthData;
    private long mStartTime;
    private long mEndTime;

    public SHealthManager() {
        // Set up permissions needed
        mKeySet = new HashSet<>();
        mKeySet.add(new HealthPermissionManager.PermissionKey(HealthConstants.StepCount.HEALTH_DATA_TYPE, HealthPermissionManager.PermissionType.READ));
        mKeySet.add(new HealthPermissionManager.PermissionKey(HealthConstants.Exercise.HEALTH_DATA_TYPE, HealthPermissionManager.PermissionType.READ));
        mKeySet.add(new HealthPermissionManager.PermissionKey(HealthConstants.Sleep.HEALTH_DATA_TYPE, HealthPermissionManager.PermissionType.READ));
        mKeySet.add(new HealthPermissionManager.PermissionKey(HealthConstants.FoodInfo.HEALTH_DATA_TYPE, HealthPermissionManager.PermissionType.READ));
        mKeySet.add(new HealthPermissionManager.PermissionKey(HealthConstants.FoodIntake.HEALTH_DATA_TYPE, HealthPermissionManager.PermissionType.READ));
        mKeySet.add(new HealthPermissionManager.PermissionKey(HealthConstants.WaterIntake.HEALTH_DATA_TYPE, HealthPermissionManager.PermissionType.READ));
        mKeySet.add(new HealthPermissionManager.PermissionKey(HealthConstants.CaffeineIntake.HEALTH_DATA_TYPE, HealthPermissionManager.PermissionType.READ));
        mKeySet.add(new HealthPermissionManager.PermissionKey(HealthConstants.USER_PROFILE_DATA_TYPE, HealthPermissionManager.PermissionType.READ));

        // TODO: Include ambient temperature, sleep stage, heart rate, blood glucose, and blood pressure?
        //mKeySet.add(new HealthPermissionManager.PermissionKey(HealthConstants.AmbientTemperature.HEALTH_DATA_TYPE, HealthPermissionManager.PermissionType.READ));
        //mKeySet.add(new HealthPermissionManager.PermissionKey(HealthConstants.SleepStage.HEALTH_DATA_TYPE, HealthPermissionManager.PermissionType.READ));
        //mKeySet.add(new HealthPermissionManager.PermissionKey(HealthConstants.HeartRate.HEALTH_DATA_TYPE, HealthPermissionManager.PermissionType.READ));
        //mKeySet.add(new HealthPermissionManager.PermissionKey(HealthConstants.BloodGlucose.HEALTH_DATA_TYPE, HealthPermissionManager.PermissionType.READ));
        //mKeySet.add(new HealthPermissionManager.PermissionKey(HealthConstants.BloodPressure.HEALTH_DATA_TYPE, HealthPermissionManager.PermissionType.READ));
    }

    private void init(Context context) {
        // Initialize s health

        if (mHealthDataService == null) {
            mHealthDataService = new HealthDataService();
            try {
                mHealthDataService.initialize(context);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        mStore = new HealthDataStore(context, mConnectionListener);

        // Request the connection to the health data store
        mStore.connectService();
    }

    public void attach(Context context, boolean showPermissionDialogue, boolean alwaysShowPermissionDialogue) {
        if (context instanceof SHealthDataListener) {
            mListener = (SHealthDataListener) context;

            mShowPermissionDialogue = showPermissionDialogue;
            mAlwaysShowPermissionDialogue = alwaysShowPermissionDialogue;

            init(context);
        } else {
            throw new RuntimeException(context.toString() + " must implement SHealthDataListener");
        }
    }

    public void attach(Context context, boolean showPermissionDialogue) {
        attach(context, showPermissionDialogue, false);
    }

    // Helper methods
    private long getStartTimeOfYesterday() {
        Calendar yesterday = Calendar.getInstance();

        yesterday.set(Calendar.DATE, yesterday.get(Calendar.DATE) - 1);
        yesterday.set(Calendar.HOUR_OF_DAY, 0);
        yesterday.set(Calendar.MINUTE, 0);
        yesterday.set(Calendar.SECOND, 0);
        yesterday.set(Calendar.MILLISECOND, 0);

        return yesterday.getTimeInMillis();
    }

    private long getEndTimeOfYesterday() {
        Calendar yesterday = Calendar.getInstance();

        yesterday.set(Calendar.DATE, yesterday.get(Calendar.DATE) - 1);
        yesterday.set(Calendar.HOUR_OF_DAY, 23);
        yesterday.set(Calendar.MINUTE, 59);
        yesterday.set(Calendar.SECOND, 59);
        yesterday.set(Calendar.MILLISECOND, 999);

        return yesterday.getTimeInMillis();
    }

    private long getStartTimeOfToday() {
        Calendar today = Calendar.getInstance();

        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        return today.getTimeInMillis();
    }

    private void printCalendar(Calendar cal) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Log.d(APP_TAG, dateFormat.format(cal.getTime()));
    }

    public void disconnect() {
        mStore.disconnectService();
    }

    public void startGetDataForYesterday(Context context, CountDownLatch countDownLatch) {
        mSHealthData = new SHealthData();
        mShowPermissionDialogue = false;
        mCountDownLatch = countDownLatch;
        mStartTime = getStartTimeOfYesterday();
        mEndTime = getEndTimeOfYesterday();
        init(context);
    }

    public void startGetDataForToday(Context context, CountDownLatch countDownLatch) {
        mSHealthData = new SHealthData();
        mShowPermissionDialogue = false;
        mCountDownLatch = countDownLatch;
        mStartTime = getStartTimeOfToday();
        mEndTime = System.currentTimeMillis();
        init(context);
    }

    public SHealthData getSHealthData() {
        return mSHealthData;
    }

    public boolean arePermissionsGranted() {
        Log.d(APP_TAG, "Checking if all permissions are granted.");

        mShowPermissionDialogue = false;

        HealthPermissionManager pmsManager = new HealthPermissionManager(mStore);

        boolean granted = false;

        try {
            // Check whether the permissions that this application needs are acquired
            Map<HealthPermissionManager.PermissionKey, Boolean> resultMap = pmsManager.isPermissionAcquired(mKeySet);
            granted = !resultMap.containsValue(Boolean.FALSE);
        } catch (Exception e) {
            Log.d(APP_TAG, e.getClass().getName() + " - " + e.getMessage());
            Log.d(APP_TAG, "Permission check failed.");
        }
        return granted;
    }

    // Getting permission from user to connect app to s health
    private final HealthDataStore.ConnectionListener mConnectionListener = new HealthDataStore.ConnectionListener() {
        @Override
        public void onConnected() {
            Log.d(APP_TAG, "Health data service is connected.");
            handleConnection();
        }

        @Override
        public void onConnectionFailed(HealthConnectionErrorResult error) {
            Log.d(APP_TAG, "Health data service is not available.");
            showConnectionFailureDialog(error);
            mListener.sHealthPermissionFailed();
        }

        @Override
        public void onDisconnected() {
            Log.d(APP_TAG, "Health data service is disconnected.");
        }
    };

    private void handleConnection() {
        HealthPermissionManager pmsManager = new HealthPermissionManager(mStore);

        try {
            // Check whether the permissions that this application needs are acquired
            Map<HealthPermissionManager.PermissionKey, Boolean> resultMap = pmsManager.isPermissionAcquired(mKeySet);
            if (resultMap.containsValue(Boolean.FALSE) || mAlwaysShowPermissionDialogue) {
                // TODO: Work with permissions we've got
                if (mShowPermissionDialogue) {
                    // Request the permission for reading values if it is not acquired
                    Log.d(APP_TAG, "Requesting permissions...");
                    pmsManager.requestPermissions(mKeySet, ((Activity) mListener)).setResultListener(mPermissionListener);
                } else {
                    Log.d(APP_TAG, "Can't request permissions right now.");
                    if (mCountDownLatch != null) {
                        // Count down to notify job that we're done
                        long count = mCountDownLatch.getCount();
                        for (int i = 0; i < count; i++) {
                            mCountDownLatch.countDown();
                        }
                        mSHealthData = null;
                    }
                }
            } else {
                // Get the current data
                Log.d(APP_TAG, "Permissions are acquired.");
                if (mCountDownLatch != null) {
                    getData();
                } else {
                    mListener.sHealthConnectedAndPermissionsAcquired();
                }
            }
        } catch (Exception e) {
            Log.d(APP_TAG, e.getClass().getName() + " - " + e.getMessage());
            Log.d(APP_TAG, "Permission setting failed.");
        }
    }

    // Show error message if getting permission or initiating connection failed
    private void showConnectionFailureDialog(HealthConnectionErrorResult error) {
        if (mShowPermissionDialogue) {
            AlertDialog.Builder alert = new AlertDialog.Builder(((Context) mListener));
            mConnError = error;
            String message = "Connection with S Health is not available";
            if (mConnError.hasResolution()) {
                switch (error.getErrorCode()) {
                    case HealthConnectionErrorResult.PLATFORM_NOT_INSTALLED:
                        message = "Please install S Health";
                        break;
                    case HealthConnectionErrorResult.OLD_VERSION_PLATFORM:
                        message = "Please upgrade S Health";
                        break;
                    case HealthConnectionErrorResult.PLATFORM_DISABLED:
                        message = "Please enable S Health";
                        break;
                    case HealthConnectionErrorResult.USER_AGREEMENT_NEEDED:
                        message = "Please agree with S Health policy";
                        break;
                    default:
                        message = "Please make S Health available";
                        break;
                }
            }
            alert.setMessage(message);
            alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    if (mConnError.hasResolution()) {
                        mConnError.resolve(((Activity) mListener));
                    }
                }
            });
            if (error.hasResolution()) {
                alert.setNegativeButton("Cancel", null);
            }
            alert.show();
        }
    }

    // Listening for permission result
    private final HealthResultHolder.ResultListener<HealthPermissionManager.PermissionResult> mPermissionListener = new HealthResultHolder.ResultListener<HealthPermissionManager.PermissionResult>() {
        @Override
        public void onResult(HealthPermissionManager.PermissionResult result) {
            Log.d(APP_TAG, "Permission callback is received.");
            Map<HealthPermissionManager.PermissionKey, Boolean> resultMap = result.getResultMap();
            if (resultMap.containsValue(Boolean.FALSE)) {
                // Requesting permission
                // TODO: Work with permissions we've got
                Log.d(APP_TAG, "Permission denied.");

                if (mListener != null) {
                    mListener.sHealthPermissionFailed();
                } else if (mCountDownLatch != null) {
                    // Count down to notify job that we're done
                    long count = mCountDownLatch.getCount();
                    for (int i = 0; i < count; i++) {
                        mCountDownLatch.countDown();
                    }
                    mSHealthData = null;
                }
            } else {
                // Get current data
                mListener.sHealthConnectedAndPermissionsAcquired();
            }
        }
    };

    private void getData() {
        // Set time range to all day yesterday

        mSHealthData.setDateTime(mEndTime);

        readUserProfile();
        readYesterdayStepCount(mStartTime, mEndTime);
        readYesterdayCaffeineIntake(mStartTime, mEndTime);
        readYesterdayWaterIntake(mStartTime, mEndTime);
        readYesterdayFoodIntake(mStartTime, mEndTime);
        readYesterdayExercise(mStartTime, mEndTime);
        readYesterdaySleep(mStartTime, mEndTime);

        // TODO: Reactivate if needed
        //readYesterdayAmbientTemperature(mStartTime, mEndTime);
        //readYesterdayHeartRate(mStartTime, mEndTime);
        //readYesterdayBloodGlucose(mStartTime, mEndTime);
        //readYesterdayBloodPressure(mStartTime, mEndTime);
    }


    // User profile
    private void readUserProfile() {
        Log.d(APP_TAG, "Getting user profile...");
        HealthUserProfile healthUserProfile = HealthUserProfile.getProfile(mStore);

        Log.d(APP_TAG, "Hi " + healthUserProfile.getUserName()
                + ", born on " + healthUserProfile.getBirthDate()
                + ", gender " + (healthUserProfile.getGender() == 1 ? "male" : (healthUserProfile.getGender() == 2 ? "female" : "unknown"))
                + ", height " + healthUserProfile.getHeight() + "cm"
                + ", weight " + healthUserProfile.getWeight() + "kg"
        );


        mCountDownLatch.countDown();
        // TODO: Save user profile to DB
    }

    // Step count
    private void readYesterdayStepCount(long startTime, long endTime) {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);

        HealthDataResolver.Filter filter = HealthDataResolver.Filter.and(HealthDataResolver.Filter.greaterThanEquals(HealthConstants.StepCount.START_TIME, startTime), HealthDataResolver.Filter.lessThanEquals(HealthConstants.StepCount.START_TIME, endTime));

        HealthDataResolver.ReadRequest request = new HealthDataResolver.ReadRequest.Builder()
                .setDataType(HealthConstants.StepCount.HEALTH_DATA_TYPE)
                .setProperties(new String[]{HealthConstants.StepCount.COUNT})
                .setFilter(filter)
                .build();

        try {
            resolver.read(request).setResultListener(new HealthResultHolder.ResultListener<HealthDataResolver.ReadResult>() {
                @Override
                public void onResult(HealthDataResolver.ReadResult result) {
                    Log.d(APP_TAG, "Calculating step count...");

                    int steps = 0;
                    Cursor c = null;
                    try {
                        c = result.getResultCursor();
                        if (c != null) {
                            if (c.getCount() == 0) {
                                Log.d(APP_TAG, "No step count entry found.");
                            }
                            while (c.moveToNext()) {
                                steps += c.getInt(c.getColumnIndex(HealthConstants.StepCount.COUNT));
                            }
                        }
                    } finally {
                        if (c != null) {
                            c.close();
                        }
                    }

                    Log.d(APP_TAG, steps + " steps");

                    // Save to s health data object
                    if (steps != 0) {
                        mSHealthData.setSteps(steps);
                    }
                    mCountDownLatch.countDown();
                }
            });
        } catch (Exception e) {
            Log.d(APP_TAG, e.getClass().getName() + " - " + e.getMessage());
            Log.d(APP_TAG, "Getting step count failed.");
        }
    }

    // Caffeine intake
    private void readYesterdayCaffeineIntake(long startTime, long endTime) {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);

        // Set time range to all day yesterday
        HealthDataResolver.Filter filter = HealthDataResolver.Filter.and(HealthDataResolver.Filter.greaterThanEquals(HealthConstants.CaffeineIntake.START_TIME, startTime), HealthDataResolver.Filter.lessThanEquals(HealthConstants.CaffeineIntake.START_TIME, endTime));

        HealthDataResolver.ReadRequest request = new HealthDataResolver.ReadRequest.Builder()
                .setDataType(HealthConstants.CaffeineIntake.HEALTH_DATA_TYPE)
                .setProperties(new String[]{HealthConstants.CaffeineIntake.AMOUNT})
                .setFilter(filter)
                .build();

        try {
            resolver.read(request).setResultListener(new HealthResultHolder.ResultListener<HealthDataResolver.ReadResult>() {
                @Override
                public void onResult(HealthDataResolver.ReadResult result) {
                    Log.d(APP_TAG, "Getting caffeine intake...");

                    int caffeine = 0;
                    Cursor c = null;
                    try {
                        c = result.getResultCursor();
                        if (c != null) {
                            if (c.getCount() == 0) {
                                Log.d(APP_TAG, "No caffeine entry found.");
                            }
                            while (c.moveToNext()) {
                                caffeine += c.getInt(c.getColumnIndex(HealthConstants.CaffeineIntake.AMOUNT));
                            }
                        }
                    } finally {
                        if (c != null) {
                            c.close();
                        }
                    }

                    Log.d(APP_TAG, caffeine + "mg caffeine");

                    // Save to s health data object
                    if (caffeine != 0) {
                        mSHealthData.setCaffeine(caffeine);
                    }
                    mCountDownLatch.countDown();
                }
            });
        } catch (Exception e) {
            Log.d(APP_TAG, e.getClass().getName() + " - " + e.getMessage());
            Log.d(APP_TAG, "Getting caffeine intake failed.");
        }
    }

    // Water intake
    private void readYesterdayWaterIntake(long startTime, long endTime) {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);

        // Set time range to all day yesterday
        HealthDataResolver.Filter filter = HealthDataResolver.Filter.and(HealthDataResolver.Filter.greaterThanEquals(HealthConstants.WaterIntake.START_TIME, startTime), HealthDataResolver.Filter.lessThanEquals(HealthConstants.WaterIntake.START_TIME, endTime));

        HealthDataResolver.ReadRequest request = new HealthDataResolver.ReadRequest.Builder()
                .setDataType(HealthConstants.WaterIntake.HEALTH_DATA_TYPE)
                .setProperties(new String[]{HealthConstants.WaterIntake.AMOUNT})
                .setFilter(filter)
                .build();

        try {
            resolver.read(request).setResultListener(new HealthResultHolder.ResultListener<HealthDataResolver.ReadResult>() {
                @Override
                public void onResult(HealthDataResolver.ReadResult result) {
                    Log.d(APP_TAG, "Getting water intake...");

                    int water = 0;
                    Cursor c = null;
                    try {
                        c = result.getResultCursor();
                        if (c != null) {
                            if (c.getCount() == 0) {
                                Log.d(APP_TAG, "No water intake entry found.");
                            }
                            while (c.moveToNext()) {
                                water += c.getInt(c.getColumnIndex(HealthConstants.WaterIntake.AMOUNT));
                            }
                        }
                    } finally {
                        if (c != null) {
                            c.close();
                        }
                    }

                    Log.d(APP_TAG, water + "ml water");

                    // Save to s health data object
                    if (water != 0) {
                        mSHealthData.setWater(water);
                    }
                    mCountDownLatch.countDown();
                }
            });
        } catch (Exception e) {
            Log.d(APP_TAG, e.getClass().getName() + " - " + e.getMessage());
            Log.d(APP_TAG, "Getting water intake failed.");
        }
    }

    // Food intake
    private void readYesterdayFoodIntake(long startTime, long endTime) {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);

        // Set time range to all day yesterday
        HealthDataResolver.Filter filter = HealthDataResolver.Filter.and(HealthDataResolver.Filter.greaterThanEquals(HealthConstants.FoodIntake.START_TIME, startTime), HealthDataResolver.Filter.lessThanEquals(HealthConstants.FoodIntake.START_TIME, endTime));

        HealthDataResolver.ReadRequest request = new HealthDataResolver.ReadRequest.Builder()
                .setDataType(HealthConstants.FoodIntake.HEALTH_DATA_TYPE)
                .setProperties(new String[]{
                        HealthConstants.FoodIntake.NAME,
                        HealthConstants.FoodIntake.CALORIE,
                        HealthConstants.FoodIntake.FOOD_INFO_ID,
                        HealthConstants.FoodIntake.MEAL_TYPE
                })
                .setFilter(filter)
                .build();

        try {
            resolver.read(request).setResultListener(new HealthResultHolder.ResultListener<HealthDataResolver.ReadResult>() {
                @Override
                public void onResult(HealthDataResolver.ReadResult result) {
                    Log.d(APP_TAG, "Getting food intake...");
                    ArrayList<String> foodInfoIdsList = new ArrayList<>();
                    int count, mealType;
                    String foodInfoId, name, mealTypeString;
                    Cursor c = null;
                    try {
                        c = result.getResultCursor();
                        if (c != null) {
                            if (c.getCount() == 0) {
                                Log.d(APP_TAG, "No food intake entry found.");
                            }
                            while (c.moveToNext()) {
                                name = c.getString(c.getColumnIndex(HealthConstants.FoodIntake.NAME));
                                count = c.getInt(c.getColumnIndex(HealthConstants.FoodIntake.CALORIE));
                                foodInfoId = c.getString(c.getColumnIndex(HealthConstants.FoodIntake.FOOD_INFO_ID));
                                mealType = c.getInt(c.getColumnIndex(HealthConstants.FoodIntake.MEAL_TYPE));
                                foodInfoIdsList.add(foodInfoId);

                                switch (mealType) {
                                    case HealthConstants.FoodIntake.MEAL_TYPE_BREAKFAST:
                                        mealTypeString = " for breakfast";
                                        break;
                                    case HealthConstants.FoodIntake.MEAL_TYPE_LUNCH:
                                        mealTypeString = " for lunch";
                                        break;
                                    case HealthConstants.FoodIntake.MEAL_TYPE_DINNER:
                                        mealTypeString = " for dinner";
                                        break;
                                    case HealthConstants.FoodIntake.MEAL_TYPE_MORNING_SNACK:
                                        mealTypeString = " as morning snack";
                                        break;
                                    case HealthConstants.FoodIntake.MEAL_TYPE_AFTERNOON_SNACK:
                                        mealTypeString = " as afternoon snack";
                                        break;
                                    case HealthConstants.FoodIntake.MEAL_TYPE_EVENING_SNACK:
                                        mealTypeString = " as evening snack";
                                        break;
                                    default:
                                        mealTypeString = "";
                                        break;
                                }

                                Log.d(APP_TAG, name + " with " + count + " calories" + mealTypeString);
                            }
                        }
                    } finally {
                        if (c != null) {
                            c.close();
                        }
                    }

                    // TODO: Save food intake to DB
                    mCountDownLatch.countDown();

                    String[] foodInfoIdsArray = new String[foodInfoIdsList.size()];
                    foodInfoIdsArray = foodInfoIdsList.toArray(foodInfoIdsArray);
                    if (foodInfoIdsArray.length > 0) {
                        readYesterdayFoodInfo(foodInfoIdsArray);
                    } else {
                        mCountDownLatch.countDown();
                    }
                }
            });
        } catch (Exception e) {
            Log.d(APP_TAG, e.getClass().getName() + " - " + e.getMessage());
            Log.d(APP_TAG, "Getting food intake failed.");
        }
    }

    // Food info
    private void readYesterdayFoodInfo(String[] foodInfoIds) {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);

        HealthDataResolver.Filter filter = HealthDataResolver.Filter.in(HealthConstants.FoodInfo.UUID, foodInfoIds);

        HealthDataResolver.ReadRequest request = new HealthDataResolver.ReadRequest.Builder()
                .setDataType(HealthConstants.FoodInfo.HEALTH_DATA_TYPE)
                .setProperties(new String[]{
                        HealthConstants.FoodIntake.NAME,
                        HealthConstants.FoodInfo.CALORIE,
                        HealthConstants.FoodInfo.TOTAL_FAT,
                        HealthConstants.FoodInfo.SATURATED_FAT,
                        HealthConstants.FoodInfo.POLYSATURATED_FAT,
                        HealthConstants.FoodInfo.MONOSATURATED_FAT,
                        HealthConstants.FoodInfo.TRANS_FAT,
                        HealthConstants.FoodInfo.CARBOHYDRATE,
                        HealthConstants.FoodInfo.DIETARY_FIBER,
                        HealthConstants.FoodInfo.SUGAR,
                        HealthConstants.FoodInfo.PROTEIN,
                        HealthConstants.FoodInfo.CHOLESTEROL,
                        HealthConstants.FoodInfo.SODIUM,
                        HealthConstants.FoodInfo.POTASSIUM,
                        HealthConstants.FoodInfo.VITAMIN_A,
                        HealthConstants.FoodInfo.VITAMIN_C,
                        HealthConstants.FoodInfo.CALCIUM,
                        HealthConstants.FoodInfo.IRON
                })
                .setFilter(filter)
                .build();

        try {
            resolver.read(request).setResultListener(new HealthResultHolder.ResultListener<HealthDataResolver.ReadResult>() {
                @Override
                public void onResult(HealthDataResolver.ReadResult result) {
                    Log.d(APP_TAG, "Getting food info...");
                    float calories, totalFat, saturatedFat, polySaturatedFat, monoSaturatedFat, transFat, carbohydrate, dietaryFiber, sugar, protein, cholesterol, sodium, potassium, vitaminA, vitaminC, calcium, iron;
                    String name;
                    Cursor c = null;

                    List<Food> foods = new ArrayList<>();
                    try {
                        c = result.getResultCursor();
                        if (c != null) {
                            if (c.getCount() == 0) {
                                Log.d(APP_TAG, "No food info entry found.");
                            }
                            while (c.moveToNext()) {
                                name = c.getString(c.getColumnIndex(HealthConstants.FoodInfo.NAME));
                                calories = c.getFloat(c.getColumnIndex(HealthConstants.FoodInfo.CALORIE));
                                totalFat = c.getFloat(c.getColumnIndex(HealthConstants.FoodInfo.TOTAL_FAT));
                                saturatedFat = c.getFloat(c.getColumnIndex(HealthConstants.FoodInfo.SATURATED_FAT));
                                polySaturatedFat = c.getFloat(c.getColumnIndex(HealthConstants.FoodInfo.POLYSATURATED_FAT));
                                monoSaturatedFat = c.getFloat(c.getColumnIndex(HealthConstants.FoodInfo.MONOSATURATED_FAT));
                                transFat = c.getFloat(c.getColumnIndex(HealthConstants.FoodInfo.TRANS_FAT));
                                carbohydrate = c.getFloat(c.getColumnIndex(HealthConstants.FoodInfo.CARBOHYDRATE));
                                dietaryFiber = c.getFloat(c.getColumnIndex(HealthConstants.FoodInfo.DIETARY_FIBER));
                                sugar = c.getFloat(c.getColumnIndex(HealthConstants.FoodInfo.SUGAR));
                                protein = c.getFloat(c.getColumnIndex(HealthConstants.FoodInfo.PROTEIN));
                                cholesterol = c.getFloat(c.getColumnIndex(HealthConstants.FoodInfo.CHOLESTEROL));
                                sodium = c.getFloat(c.getColumnIndex(HealthConstants.FoodInfo.SODIUM));
                                potassium = c.getFloat(c.getColumnIndex(HealthConstants.FoodInfo.POTASSIUM));
                                vitaminA = c.getFloat(c.getColumnIndex(HealthConstants.FoodInfo.VITAMIN_A));
                                vitaminC = c.getFloat(c.getColumnIndex(HealthConstants.FoodInfo.VITAMIN_C));
                                calcium = c.getFloat(c.getColumnIndex(HealthConstants.FoodInfo.CALCIUM));
                                iron = c.getFloat(c.getColumnIndex(HealthConstants.FoodInfo.IRON));

                                Food newFood = new Food(name, calories, totalFat, saturatedFat, polySaturatedFat, monoSaturatedFat, transFat, carbohydrate, dietaryFiber, sugar, protein, cholesterol, sodium, potassium, vitaminA, vitaminC, calcium, iron);
                                Log.d(APP_TAG, newFood.toString());
                                foods.add(newFood);
                            }
                        }
                    } finally {
                        if (c != null) {
                            c.close();
                        }
                    }

                    // Save to s health data object
                    mSHealthData.setFoods(foods);
                    mCountDownLatch.countDown();
                }
            });
        } catch (Exception e) {
            Log.d(APP_TAG, e.getClass().getName() + " - " + e.getMessage());
            Log.d(APP_TAG, "Getting food info failed.");
        }
    }

    // Exercise
    private void readYesterdayExercise(long startTime, long endTime) {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);

        // Set time range to all day yesterday
        HealthDataResolver.Filter filter = HealthDataResolver.Filter.and(HealthDataResolver.Filter.greaterThanEquals(HealthConstants.Exercise.START_TIME, startTime), HealthDataResolver.Filter.lessThanEquals(HealthConstants.Exercise.START_TIME, endTime));

        HealthDataResolver.ReadRequest request = new HealthDataResolver.ReadRequest.Builder()
                .setDataType(HealthConstants.Exercise.HEALTH_DATA_TYPE)
                .setProperties(new String[]{
                        HealthConstants.Exercise.CALORIE,
                        HealthConstants.Exercise.DURATION,
                        HealthConstants.Exercise.DISTANCE,
                        HealthConstants.Exercise.EXERCISE_TYPE,
                        HealthConstants.Exercise.EXERCISE_CUSTOM_TYPE
                })
                .setFilter(filter)
                .build();

        try {
            resolver.read(request).setResultListener(new HealthResultHolder.ResultListener<HealthDataResolver.ReadResult>() {
                @Override
                public void onResult(HealthDataResolver.ReadResult result) {
                    Log.d(APP_TAG, "Getting exercise...");

                    List<Exercise> exercises = new ArrayList<>();

                    float calories;
                    long duration;
                    int exerciseType;
                    float distance;
                    String customExerciseType;
                    Cursor c = null;
                    try {
                        c = result.getResultCursor();
                        if (c != null) {
                            if (c.getCount() == 0) {
                                Log.d(APP_TAG, "No exercise entry found.");
                            }
                            while (c.moveToNext()) {
                                calories = c.getFloat(c.getColumnIndex(HealthConstants.Exercise.CALORIE));
                                duration = c.getLong(c.getColumnIndex(HealthConstants.Exercise.DURATION));
                                distance = c.getFloat(c.getColumnIndex(HealthConstants.Exercise.DISTANCE));
                                exerciseType = c.getInt(c.getColumnIndex(HealthConstants.Exercise.EXERCISE_TYPE));
                                customExerciseType = c.getString(c.getColumnIndex(HealthConstants.Exercise.EXERCISE_CUSTOM_TYPE));

                                Exercise newExercise = new Exercise(calories, duration, exerciseType, distance, customExerciseType);
                                exercises.add(newExercise);

                                Log.d(APP_TAG, newExercise.toString());
                            }
                        }
                    } finally {
                        if (c != null) {
                            c.close();
                        }
                    }

                    // Save to s health data object
                    mSHealthData.setExercises(exercises);
                    mCountDownLatch.countDown();
                }
            });
        } catch (Exception e) {
            Log.d(APP_TAG, e.getClass().getName() + " - " + e.getMessage());
            Log.d(APP_TAG, "Getting exercise failed.");
        }
    }

    // Sleep
    private void readYesterdaySleep(long startTime, long endTime) {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);

        // Set time range to all day yesterday
        HealthDataResolver.Filter filter = HealthDataResolver.Filter.and(HealthDataResolver.Filter.greaterThanEquals(HealthConstants.Sleep.END_TIME, startTime), HealthDataResolver.Filter.lessThanEquals(HealthConstants.Sleep.END_TIME, endTime));

        HealthDataResolver.ReadRequest request = new HealthDataResolver.ReadRequest.Builder()
                .setDataType(HealthConstants.Sleep.HEALTH_DATA_TYPE)
                .setProperties(new String[]{HealthConstants.Sleep.START_TIME, HealthConstants.Sleep.END_TIME, HealthConstants.Sleep.TIME_OFFSET})
                .setFilter(filter)
                .build();

        try {
            resolver.read(request).setResultListener(new HealthResultHolder.ResultListener<HealthDataResolver.ReadResult>() {
                @Override
                public void onResult(HealthDataResolver.ReadResult result) {
                    Log.d(APP_TAG, "Getting sleep...");

                    long start, end;
                    Sleep sleep = null;
                    Cursor c = null;
                    try {
                        c = result.getResultCursor();
                        if (c != null) {
                            if (c.getCount() == 0) {
                                Log.d(APP_TAG, "No sleep entry found.");
                            }
                            while (c.moveToNext()) {
                                start = c.getLong(c.getColumnIndex(HealthConstants.Sleep.START_TIME));
                                end = c.getLong(c.getColumnIndex(HealthConstants.Sleep.END_TIME));

                                sleep = new Sleep(start, end);

                                Log.d(APP_TAG, sleep.toString());
                            }
                        }
                    } finally {
                        if (c != null) {
                            c.close();
                        }
                    }

                    // Save to s health data object
                    mSHealthData.setSleep(sleep);
                    mCountDownLatch.countDown();
                }
            });
        } catch (Exception e) {
            Log.d(APP_TAG, e.getClass().getName() + " - " + e.getMessage());
            Log.d(APP_TAG, "Getting sleep failed.");
        }
    }

    // TODO: Ambient temperature not working?
    // Ambient temperature
    private void readYesterdayAmbientTemperature(long startTime, long endTime) {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);

        // Set time range to all day yesterday
        HealthDataResolver.Filter filter = HealthDataResolver.Filter.and(HealthDataResolver.Filter.greaterThanEquals(HealthConstants.AmbientTemperature.START_TIME, startTime), HealthDataResolver.Filter.lessThanEquals(HealthConstants.AmbientTemperature.START_TIME, endTime));

        HealthDataResolver.ReadRequest request = new HealthDataResolver.ReadRequest.Builder()
                .setDataType(HealthConstants.AmbientTemperature.HEALTH_DATA_TYPE)
                .setProperties(new String[]{
                        HealthConstants.AmbientTemperature.TEMPERATURE,
                        HealthConstants.AmbientTemperature.HUMIDITY,
                        HealthConstants.AmbientTemperature.ALTITUDE
                })
                .setFilter(filter)
                .build();

        try {
            resolver.read(request).setResultListener(new HealthResultHolder.ResultListener<HealthDataResolver.ReadResult>() {
                @Override
                public void onResult(HealthDataResolver.ReadResult result) {
                    Log.d(APP_TAG, "Getting ambient temperature...");

                    float temperature, humidity, altitude;
                    AmbientTemperature ambientTemperature = null;
                    Cursor c = null;
                    try {
                        c = result.getResultCursor();
                        if (c != null) {
                            if (c.getCount() == 0) {
                                Log.d(APP_TAG, "No ambient temperature entry found.");
                            }
                            while (c.moveToNext()) {
                                temperature = c.getFloat(c.getColumnIndex(HealthConstants.AmbientTemperature.TEMPERATURE));
                                humidity = c.getFloat(c.getColumnIndex(HealthConstants.AmbientTemperature.HUMIDITY));
                                altitude = c.getFloat(c.getColumnIndex(HealthConstants.AmbientTemperature.ALTITUDE));

                                ambientTemperature = new AmbientTemperature(temperature, humidity, altitude);

                                Log.d(APP_TAG, ambientTemperature.toString());
                            }
                        }
                    } finally {
                        if (c != null) {
                            c.close();
                        }
                    }

                    mSHealthData.setAmbientTemperature(ambientTemperature);
                    mCountDownLatch.countDown();
                }
            });
        } catch (Exception e) {
            Log.d(APP_TAG, e.getClass().getName() + " - " + e.getMessage());
            Log.d(APP_TAG, "Getting ambient temperature failed.");
        }
    }

    // Heart rate
    private void readYesterdayHeartRate(long startTime, long endTime) {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);

        // Set time range to all day yesterday
        HealthDataResolver.Filter filter = HealthDataResolver.Filter.and(HealthDataResolver.Filter.greaterThanEquals(HealthConstants.HeartRate.START_TIME, startTime), HealthDataResolver.Filter.lessThanEquals(HealthConstants.HeartRate.START_TIME, endTime));

        HealthDataResolver.ReadRequest request = new HealthDataResolver.ReadRequest.Builder()
                .setDataType(HealthConstants.HeartRate.HEALTH_DATA_TYPE)
                .setProperties(new String[]{
                        HealthConstants.HeartRate.HEART_BEAT_COUNT,
                        HealthConstants.HeartRate.HEART_RATE
                })
                .setFilter(filter)
                .build();

        try {
            resolver.read(request).setResultListener(new HealthResultHolder.ResultListener<HealthDataResolver.ReadResult>() {
                @Override
                public void onResult(HealthDataResolver.ReadResult result) {
                    Log.d(APP_TAG, "Getting heart rate...");

                    int heartBeatCount;
                    long heartRate;
                    Cursor c = null;
                    try {
                        c = result.getResultCursor();
                        if (c != null) {
                            if (c.getCount() == 0) {
                                Log.d(APP_TAG, "No heart rate entry found.");
                            }
                            while (c.moveToNext()) {
                                heartBeatCount = c.getInt(c.getColumnIndex(HealthConstants.HeartRate.HEART_BEAT_COUNT));
                                heartRate = c.getLong(c.getColumnIndex(HealthConstants.HeartRate.HEART_RATE));

                                Log.d(APP_TAG, "Heart beat count " + heartBeatCount
                                        + ", heart rate " + heartRate + "bpm"
                                );
                            }
                        }
                    } finally {
                        if (c != null) {
                            c.close();
                        }
                    }

                    // TODO: Save heart rate to DB
                    mCountDownLatch.countDown();
                }
            });
        } catch (Exception e) {
            Log.d(APP_TAG, e.getClass().getName() + " - " + e.getMessage());
            Log.d(APP_TAG, "Getting heart rate failed.");
        }
    }

    // Blood glucose
    private void readYesterdayBloodGlucose(long startTime, long endTime) {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);

        // Set time range to all day yesterday
        HealthDataResolver.Filter filter = HealthDataResolver.Filter.and(HealthDataResolver.Filter.greaterThanEquals(HealthConstants.BloodGlucose.START_TIME, startTime), HealthDataResolver.Filter.lessThanEquals(HealthConstants.BloodGlucose.START_TIME, endTime));

        HealthDataResolver.ReadRequest request = new HealthDataResolver.ReadRequest.Builder()
                .setDataType(HealthConstants.BloodGlucose.HEALTH_DATA_TYPE)
                .setProperties(new String[]{HealthConstants.BloodGlucose.GLUCOSE})
                .setFilter(filter)
                .build();

        try {
            resolver.read(request).setResultListener(new HealthResultHolder.ResultListener<HealthDataResolver.ReadResult>() {
                @Override
                public void onResult(HealthDataResolver.ReadResult result) {
                    Log.d(APP_TAG, "Getting blood glucose...");

                    float glucose;
                    Cursor c = null;
                    try {
                        c = result.getResultCursor();
                        if (c != null) {
                            if (c.getCount() == 0) {
                                Log.d(APP_TAG, "No blood glucose entry found.");
                            }
                            while (c.moveToNext()) {
                                glucose = c.getLong(c.getColumnIndex(HealthConstants.BloodGlucose.GLUCOSE));

                                Log.d(APP_TAG, "Glucose: " + glucose);
                            }
                        }
                    } finally {
                        if (c != null) {
                            c.close();
                        }
                    }

                    // TODO: Save blood glucose to DB
                    mCountDownLatch.countDown();
                }
            });
        } catch (Exception e) {
            Log.d(APP_TAG, e.getClass().getName() + " - " + e.getMessage());
            Log.d(APP_TAG, "Getting blood glucose failed.");
        }
    }

    // Blood pressure
    private void readYesterdayBloodPressure(long startTime, long endTime) {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);

        // Set time range to all day yesterday
        HealthDataResolver.Filter filter = HealthDataResolver.Filter.and(HealthDataResolver.Filter.greaterThanEquals(HealthConstants.BloodPressure.START_TIME, startTime), HealthDataResolver.Filter.lessThanEquals(HealthConstants.BloodPressure.START_TIME, endTime));

        HealthDataResolver.ReadRequest request = new HealthDataResolver.ReadRequest.Builder()
                .setDataType(HealthConstants.BloodPressure.HEALTH_DATA_TYPE)
                .setProperties(new String[]{
                        HealthConstants.BloodPressure.SYSTOLIC,
                        HealthConstants.BloodPressure.DIASTOLIC,
                        HealthConstants.BloodPressure.MEAN,
                        HealthConstants.BloodPressure.PULSE
                })
                .setFilter(filter)
                .build();

        try {
            resolver.read(request).setResultListener(new HealthResultHolder.ResultListener<HealthDataResolver.ReadResult>() {
                @Override
                public void onResult(HealthDataResolver.ReadResult result) {
                    Log.d(APP_TAG, "Getting blood pressure...");

                    float systolic, diastolic, mean;
                    int pulse;
                    Cursor c = null;
                    try {
                        c = result.getResultCursor();
                        if (c != null) {
                            if (c.getCount() == 0) {
                                Log.d(APP_TAG, "No blood pressure entry found.");
                            }
                            while (c.moveToNext()) {
                                systolic = c.getLong(c.getColumnIndex(HealthConstants.BloodPressure.SYSTOLIC));
                                diastolic = c.getLong(c.getColumnIndex(HealthConstants.BloodPressure.DIASTOLIC));
                                mean = c.getLong(c.getColumnIndex(HealthConstants.BloodPressure.MEAN));
                                pulse = c.getInt(c.getColumnIndex(HealthConstants.BloodPressure.PULSE));

                                Log.d(APP_TAG, "Mean blood pressure " + mean + ", systolic " + systolic + ", diastolic " + diastolic + ", pulse " + pulse);
                            }
                        }
                    } finally {
                        if (c != null) {
                            c.close();
                        }
                    }

                    // TODO: Save blood pressure to DB
                    mCountDownLatch.countDown();
                }
            });
        } catch (Exception e) {
            Log.d(APP_TAG, e.getClass().getName() + " - " + e.getMessage());
            Log.d(APP_TAG, "Getting blood pressure failed.");
        }
    }
}
