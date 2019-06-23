package net.bytle.db.queryExecutor;

import java.util.Map;
import java.util.TreeMap;

public class Statistics implements IStatistics {
    public long startUnixTimeInMillis;
    public long endUnixTimeInMillis;
    public long minimalRuntime = Long.MAX_VALUE;
    public long maximalRuntime = Long.MIN_VALUE;
    public int failCount = 0;
    public int successCount = 0;
    public long[] runTimes = new long[]{};
    public Map<Long, Integer> tpsMap = new TreeMap<Long, Integer>();

    public int repetitions;
    public String id;

    public Statistics(int r, String s) {
        this.repetitions = r;
        this.id = s;

        runTimes = new long[repetitions];
        for (int i = 0; i < repetitions; i++)
            runTimes[i] = -1;

        startUnixTimeInMillis = System.currentTimeMillis();
    }

    public void storeRunTime(int i, long l) {
        runTimes[i] = l;
    }

    public void storeTps(long l, int i) {
        tpsMap.put(l, i);
    }

    public void calculate() {
        endUnixTimeInMillis = System.currentTimeMillis();

        successCount = 0;
        failCount = 0;
        for (int i = 0; i < repetitions; i++) {
            if (runTimes[i] != -1) {
                if (runTimes[i] < minimalRuntime)
                    minimalRuntime = runTimes[i];
                if (runTimes[i] > maximalRuntime)
                    maximalRuntime = runTimes[i];
                successCount++;
            } else
                failCount++;
        }
    }

    @Override
    public long getStartTimeInMillis() {
        return startUnixTimeInMillis;
    }

    @Override
    public long getEndTimeInMillis() {
        return endUnixTimeInMillis;
    }

    @Override
    public int getTotalCount() {
        return repetitions;
    }

    @Override
    public long getMinimalRuntimeInMillis() {
        return minimalRuntime;
    }

    @Override
    public long getMaximalRuntimeInMillis() {
        return maximalRuntime;
    }

    @Override
    public double getAverageRuntimeInMillis() {
        return 1.0 * getTotalRunTimeInMillis() / repetitions;
    }

    @Override
    public long getTotalRunTimeInMillis() {
        return endUnixTimeInMillis - startUnixTimeInMillis;
    }

    @Override
    public int getFailCount() {
        return failCount;
    }

    @Override
    public int getSuccessCount() {
        return successCount;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long[] getAllRunTimes() {
        return runTimes;
    }

    @Override
    public Map<Long, Integer> getTpsMap() {
        return tpsMap;
    }

}
