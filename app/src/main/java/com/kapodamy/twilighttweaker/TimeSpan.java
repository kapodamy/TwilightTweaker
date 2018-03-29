package com.kapodamy.twilighttweaker;

/**
 * Created by kapodamy on 15/03/2018.
 */

public class TimeSpan {
    public TimeSpan(int hour, int min) {
        minutes = min;
        this.hour = hour;
    }

    public int minutes;
    public int hour;

    public void setTimeSpan(TimeSpan ts) {
        minutes = ts.minutes;
        hour = ts.hour;
    }

    public int getTotalMinutes() {
        return minutes + (hour * 60);
    }
}
