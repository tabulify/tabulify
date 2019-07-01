package net.bytle.cli;

import java.util.Date;

public class CliTimer {


    private static Date startTime;
    private final String name;

    @Override
    public String toString() {
        return name;
    }



    private Date endTime;

    long SECONDS_IN_MILLI = 1000;
    long MINUTES_IN_MILLI = 1000 * 60;
    long HOURS_IN_MILLI = 1000 * 60 * 60;


    private long responseTimeInMs;

    private CliTimer(String name) {
        this.name = name;
    }

    static public CliTimer getTimer(String name) {
        return new CliTimer(name);
    }


    public String getName() {

        return name;
    }

    public CliTimer start() {
        startTime = new Date();
        return this;
    }

    public void stop() {
        endTime = new Date();
        responseTimeInMs = endTime.getTime() - startTime.getTime();


    }


    @SuppressWarnings("WeakerAccess")
    public long getResponseTimeInMilliSeconds() {
        return responseTimeInMs;
    }

    /**
     * @return the response time in (hour:minutes:seconds.milli)
     */

    public String getResponseTime() {
        long elapsedHours = responseTimeInMs / HOURS_IN_MILLI;

        long diff = responseTimeInMs % HOURS_IN_MILLI;
        long elapsedMinutes = diff / MINUTES_IN_MILLI;

        diff = diff % MINUTES_IN_MILLI;
        long elapsedSeconds = diff / SECONDS_IN_MILLI;

        diff = diff % SECONDS_IN_MILLI;
        long elapsedMilliSeconds = diff;

        return elapsedHours + ":" + elapsedMinutes + ":" + elapsedSeconds + "." + elapsedMilliSeconds;

    }
}
