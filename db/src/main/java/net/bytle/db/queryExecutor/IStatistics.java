package net.bytle.db.queryExecutor;

import java.util.Map;

public interface IStatistics {

    long getStartTimeInMillis();

    long getEndTimeInMillis();

    int getTotalCount();

    int getFailCount();

    int getSuccessCount();

    long getMinimalRuntimeInMillis();

    long getMaximalRuntimeInMillis();

    double getAverageRuntimeInMillis();

    long getTotalRunTimeInMillis();

    String getId();

    long[] getAllRunTimes();

    Map<Long, Integer> getTpsMap();
}
