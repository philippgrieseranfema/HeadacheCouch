package com.happyheadache.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static java.lang.String.*;

/**
 * Created by Alexandra Fritzen on 04/11/2016.
 */

public class Sleep {
    private long start, end;

    public Sleep() {}

    public Sleep(long start, long end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public String toString() {
        Date startDate = new Date(start);
        Date endDate = new Date(end);

        long difference = end - start;
        String differenceString = format("%d hours, %d min",
                TimeUnit.MILLISECONDS.toHours(difference),
                TimeUnit.MILLISECONDS.toMinutes(difference) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(difference))
        );

        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm, dd.MM.yyyy");

        return "Went to bed at " + formatter.format(startDate)
                + ", woke up at " + formatter.format(endDate)
                + ", total hours of sleep " + differenceString;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }
}
