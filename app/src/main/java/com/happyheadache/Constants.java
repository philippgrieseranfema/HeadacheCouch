package com.happyheadache;

import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.happyheadache.models.HeadacheEntry;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;

/**
 * Created by Alexandra Fritzen on 18/10/2016.
 */

public class Constants {
    /*
    * STRINGS
    */
    public static final String APP_TAG = "HappyHeadache";

    public static final String EMPTY_STRING = "";
    public static final String EMAIL_TYPE = "vnd.android.cursor.item/email";

    public static final String PREFERENCE_S_HEALTH_DISABLED = "sHealthDisabled";
    public static final String PREFERENCE_GOOGLE_FIT_DISABLED = "googleFitDisabled";
    public static final String PREFERENCE_NEW_HEADACHE_TIMER_STARTING_POINT = "newHeadacheTimerStartingPoint";
    public static final String PREFERENCE_FIRST_START = "firstStart";
    public static final String PREFERENCE_HAS_OPENED_SURVEY = "hasOpenedSurvey";
    public static final String PREFERENCE_LOGIN = "isLogin";

    public static final String TIMER_ACTION = "com.happyheadache.newheadachetimer";

    public static final String FIREBASE_CHILD_HEADACHES = "headaches";
    public static final String FIREBASE_CHILD_SHEALTH = "shealth";
    public static final String FIREBASE_CHILD_GOOGLEFIT = "googlefit";
    public static final String FIREBASE_CHILD_WEATHERLIB = "weatherlib";
    public static final String FIREBASE_CHILD_SENSOR = "sensor";

    public static final String INTENT_EDIT_HEADACHE_ENTRY = "edit";
    public static final String INTENT_RETURN_TO_HEADACHE_HISTORY = "return";

    public static final String BROADCAST_DATA_REFRESH = "com.happyheadache.DATA_REFRESH";

    public static final String FIREBASE_MODEL_ID = "id";
    public static final String FIREBASE_MODEL_DATETIME = "dateTime";
    public static final String FIREBASE_MODEL_DATETIMESTRING = "dateTimeString";
    public static final String FIREBASE_MODEL_DURATION = "duration";
    public static final String FIREBASE_MODEL_INTENSITY = "intensity";
    public static final String FIREBASE_MODEL_SYMPTOMS = "symptoms";
    public static final String FIREBASE_MODEL_DIDTAKEPAINRELIEVERS = "didTakePainRelievers";
    public static final String FIREBASE_MODEL_PAINRELIEVERS = "painRelievers";
    public static final String FIREBASE_MODEL_USERID = "userId";

    public static final String INITIAL_SURVEY_LINK = "http://vmkrcmar86.informatik.tu-muenchen.de/limesurvey/index.php/718759?newtest=Y&userId=";

    /*
     * ENUMS
     */
    public enum SYMPTOM {
        PHOTOPHOBIA("Photophobia"), PHONOPHOBIA("Phonophobia"), OSMOPHOBIA("Osmphobia"), NAUSEA("Nausea"), BLURRED_VISION("BlurredVision"), LIGHTHEADEDNESS("Lightheadedness"), FATIGUE("Fatigue"), INSOMNIA("Insomnia");

        final String numSymptom;

        SYMPTOM(String num) {
            this.numSymptom = num;
        }

        public String getValue() {
            return this.numSymptom + EMPTY_STRING;
        }

        public static SYMPTOM toSymptom(String val) {
            SYMPTOM retSymptom = null;
            for (SYMPTOM tempSymptom : SYMPTOM.values()) {
                if (tempSymptom.getValue().equals(val)) {
                    retSymptom = tempSymptom;
                    break;
                }
            }
            return retSymptom;
        }
    }

    /*
     * METHODS
     */
    public static String formatDate(long millis, Context context) {
        // Get the device's date format
        final String format = Settings.System.getString(context.getContentResolver(), Settings.System.DATE_FORMAT);
        java.text.DateFormat dateFormat;
        if (TextUtils.isEmpty(format)) {
            dateFormat = DateFormat.getMediumDateFormat(context.getApplicationContext());
        } else {
            dateFormat = new SimpleDateFormat(format);
        }
        return dateFormat.format(millis);
    }

    public static String formatTime(long millis, Context context) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);

        return formatTime(hourOfDay, minute, context);
    }

    public static String formatTime(int hourOfDay, int minute, Context context) {
        // Get device's time format
        String time;
        if (DateFormat.is24HourFormat(context)) {
            time = hourOfDay + ":" + minute;
        } else {
            String min = String.valueOf(minute);
            if (minute < 10) {
                min = "0" + minute;
            }

            if (hourOfDay < 12) {
                time = hourOfDay + ":" + min + " AM";
            } else {
                time = hourOfDay - 12 + ":" + min + " PM";
            }
        }
        return time;
    }

    public static String formatIntensity(int intensity, Context context) {
        int intensityResource;
        switch (intensity) {
            case 0:
                intensityResource = R.string.all_verymild;
                break;
            case 1:
                intensityResource = R.string.all_verymild;
                break;
            case 2:
                intensityResource = R.string.all_mild;
                break;
            case 3:
                intensityResource = R.string.all_mild;
                break;
            case 4:
                intensityResource = R.string.all_moderate;
                break;
            case 5:
                intensityResource = R.string.all_moderate;
                break;
            case 6:
                intensityResource = R.string.all_moderate;
                break;
            case 7:
                intensityResource = R.string.all_severe;
                break;
            case 8:
                intensityResource = R.string.all_severe;
                break;
            case 9:
                intensityResource = R.string.all_verysevere;
                break;
            case 10:
                intensityResource = R.string.all_verysevere;
                break;
            default:
                intensityResource = R.string.headachehistory_unknown;
                break;
        }
        return context.getString(intensityResource);
    }

    public static String formatHeadacheEntry(HeadacheEntry entry, String breakItem, Context context) {
        String painReliever;
        if (entry.isDidTakePainRelievers()) {
            painReliever = entry.getPainRelievers();
        } else {
            painReliever = context.getResources().getString(R.string.headachehistory_none);
        }

        String intensity = formatIntensity(entry.getIntensity(), context);

        String symptoms = EMPTY_STRING;
        if (entry.getSymptoms() != null && entry.getSymptoms().size() > 0) {
            int i = 0;
            for (Map.Entry<String, Boolean> symptom : entry.getSymptoms().entrySet()) {
                i++;
                if (symptom.getValue()) {
                    int symptomStringResource = 0;
                    switch (Constants.SYMPTOM.toSymptom(symptom.getKey())) {
                        case PHONOPHOBIA:
                            symptomStringResource = R.string.all_phonophobiatitle;
                            break;
                        case PHOTOPHOBIA:
                            symptomStringResource = R.string.all_photophobiatitle;
                            break;
                        case OSMOPHOBIA:
                            symptomStringResource = R.string.all_osmophobiatitle;
                            break;
                        case NAUSEA:
                            symptomStringResource = R.string.all_nauseatitle;
                            break;
                        case BLURRED_VISION:
                            symptomStringResource = R.string.all_blurredvisiontitle;
                            break;
                        case LIGHTHEADEDNESS:
                            symptomStringResource = R.string.all_lightheadednesstitle;
                            break;
                        case FATIGUE:
                            symptomStringResource = R.string.all_fatiguetitle;
                            break;
                        case INSOMNIA:
                            symptomStringResource = R.string.all_insomniatitle;
                            break;
                    }
                    symptoms += context.getString(symptomStringResource);
                    if (i < entry.getSymptoms().entrySet().size()) {
                        symptoms += ", ";
                    }
                }
            }
        } else {
            symptoms += context.getString(R.string.headachehistory_none);
        }

        return String.format(context.getString(R.string.headachehistory_datelist), formatDate(entry.getDateTime(), context.getApplicationContext()) + " " + formatTime(entry.getDateTime(), context.getApplicationContext())) + breakItem
                + String.format(context.getResources().getQuantityString(R.plurals.headachehistory_durationlist, entry.getDuration()), entry.getDuration()) + breakItem
                + String.format(context.getString(R.string.headachehistory_intensitylist), intensity) + breakItem
                + String.format(context.getString(R.string.headachehistory_painrelieverlist), painReliever) + breakItem
                + String.format(context.getString(R.string.headachehistory_symptomslist), symptoms);
    }

    public static String getCurrentUserId() {
        String userId;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            // User is not logged in
            // TODO: Figure out a way to keep data??
            userId = "???";
        } else {
            // User is logged in
            userId = user.getUid();
        }
        return userId;
    }
}
