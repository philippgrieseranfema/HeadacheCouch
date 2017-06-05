package com.happyheadache.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

import com.happyheadache.R;
import com.happyheadache.newheadache.NewHeadacheActivity;

import java.util.TimeZone;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.happyheadache.Constants.PREFERENCE_NEW_HEADACHE_TIMER_STARTING_POINT;
import static com.happyheadache.Constants.TIMER_ACTION;

/**
 * Created by Alexandra Fritzen on 02/11/2016.
 */

public class NewHeadacheWidgetProvider extends AppWidgetProvider {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(TIMER_ACTION)) {

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor e = sharedPreferences.edit();
            boolean newHeadacheTimerOn = sharedPreferences.getLong(PREFERENCE_NEW_HEADACHE_TIMER_STARTING_POINT, 0) != 0;

            if (newHeadacheTimerOn) {
                Intent newHeadacheIntent = new Intent(context, NewHeadacheActivity.class);
                newHeadacheIntent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(newHeadacheIntent);
            } else {
                e.putLong(PREFERENCE_NEW_HEADACHE_TIMER_STARTING_POINT, java.util.Calendar.getInstance(TimeZone.getDefault()).getTimeInMillis());
                e.apply();
            }

            RemoteViews views = styleNewHeadacheTimerButton(context, !newHeadacheTimerOn, null);

            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            manager.updateAppWidget(appWidgetId, views);
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int count = appWidgetIds.length;

        for (int appWidgetId: appWidgetIds) {
            // Get the layout for the App Widget and attach an on-click listener to the button
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_newheadache);

            // Create an Intent to launch NewHeadacheActivity
            Intent newHeadacheIntent = new Intent(context, NewHeadacheActivity.class);
            PendingIntent newHeadachePendingIntent = PendingIntent.getActivity(context, 0, newHeadacheIntent, 0);
            views.setOnClickPendingIntent(R.id.button_widget_newheadache, newHeadachePendingIntent);

            Intent intent = new Intent(context, NewHeadacheWidgetProvider.class);
            intent.setAction(TIMER_ACTION);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.button_widget_newheadachetimer, pendingIntent);

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean newHeadacheTimerOn = sharedPreferences.getLong(PREFERENCE_NEW_HEADACHE_TIMER_STARTING_POINT, 0) != 0;
            views = styleNewHeadacheTimerButton(context, newHeadacheTimerOn, views);

            // Tell the AppWidgetManager to perform an update on the current app widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    private RemoteViews styleNewHeadacheTimerButton(Context context, boolean timerOn, RemoteViews views) {
        if (views == null) {
            views = new RemoteViews(context.getPackageName(), R.layout.widget_newheadache);
        }

        String action = context.getString(R.string.widget_startrecording);
        if (timerOn) {
            action = context.getString(R.string.widget_stoprecording);
        }
        views.setTextViewText(R.id.button_widget_newheadachetimer, action);
        return views;
    }
}
