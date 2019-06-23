package net.bytle.db.queryExecutor;

import net.bytle.db.DbLoggers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

public class Load {

    private final static Logger LOGGER = DbLoggers.LOGGER_DB_QUERY;
    private final static String SEPARATOR = System.getProperty("line.separator");

    TreeMap<Long, TreeMap<String, Integer>> tpsMap = new TreeMap<Long, TreeMap<String, Integer>>();
    String[] threadNames;
    IExecutable[] executables;
    Thread[] threads = new Thread[]{};
    String outputDetail;
    String outputTps;
    Properties properties;

    public Load setup(final Properties propertiez) throws Exception {
        LOGGER.info("Setting up");

        this.properties = propertiez;

        for (String driver : properties.getProperty("drivers", "").split(",")) {
            driver = driver.trim();
            LOGGER.info("Loading driver " + driver);
            Class.forName(driver);
        }

        threadNames = properties.getProperty("threads", "").split(",");
        for (int i = 0; i < threadNames.length; i++)
            threadNames[i] = threadNames[i].trim();
        outputDetail = properties.getProperty("outputDetail", "outputDetail.txt");
        outputTps = properties.getProperty("outputTps", "outputTps.txt");

        return this;
    }

    public Load run() {
        LOGGER.info("Creating threads");

        threads = new Thread[threadNames.length];
        executables = new IExecutable[threadNames.length];

        final String timestampRun = new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss").format(new Date());
        properties.setProperty("timestamp_run", timestampRun);
        int i = 0;
        ClassBuilder<IExecutable> classBuilder = new ClassBuilder<IExecutable>();
        for (final String threadName : threadNames) {
            try {
                threads[i] = new Thread((executables[i] = classBuilder.build(QueryExecutor.class.getName()).setup(threadName, properties)));
                threads[i].setName(threadName);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            i++;
        }

        LOGGER.info("Starting threads");
        while (--i >= 0)
            if (threads[i] != null)
                threads[i].start();

        return this;
    }

    @SuppressWarnings("unchecked")
    public Load join() throws InterruptedException {
        LOGGER.info("Waiting for threads to finish");
        for (Thread thread : threads)
            if (thread != null)
                thread.join();

        return this;
    }

    @SuppressWarnings("unchecked")
    public void collect() throws IOException {
        BufferedWriter detailWriter = new BufferedWriter(new FileWriter(outputDetail));
        detailWriter.write(String.format(
                "thread_id\texecution_nr\truntime_in_millis\tfailed_indicator%s",
                SEPARATOR
        ));

        LOGGER.info("Collecting statistics and writing them to " + outputDetail);
        for (IExecutable executable : executables) {
            if (executable != null) {
                IStatistics statistics = executable.getStatistics();
                if (statistics != null) {
                    for (Map.Entry entry : statistics.getTpsMap().entrySet()) {
                        TreeMap<String, Integer> x = tpsMap.get((Long) entry.getKey());
                        if (x == null)
                            x = new TreeMap<String, Integer>();
                        x.put(statistics.getId(), (Integer) entry.getValue());
                        tpsMap.put((Long) entry.getKey(), x);
                    }

                    LOGGER.info(
                            String.format(
                                    Locale.US,
                                    "Id : %s, start : %d, end : %d, average : % 10.2f, total : % 8d, count : % 8d, min : % 8d, max : % 8d, success : % 8d, failed : % 8d, tps : % 10.2f",
                                    statistics.getId(),
                                    statistics.getStartTimeInMillis(),
                                    statistics.getEndTimeInMillis(),
                                    statistics.getAverageRuntimeInMillis(),
                                    statistics.getTotalRunTimeInMillis(),
                                    statistics.getTotalCount(),
                                    statistics.getMinimalRuntimeInMillis() == Long.MAX_VALUE ? 0 : statistics.getMinimalRuntimeInMillis(),
                                    statistics.getMaximalRuntimeInMillis() == Long.MIN_VALUE ? 0 : statistics.getMaximalRuntimeInMillis(),
                                    statistics.getSuccessCount(),
                                    statistics.getFailCount(),
                                    1000.0 * statistics.getTotalCount() / statistics.getTotalRunTimeInMillis()

                            )
                    );

                    long[] allRunTimes = statistics.getAllRunTimes();
                    for (int i = 0; i < allRunTimes.length; i++) {
                        long l = allRunTimes[i];
                        detailWriter.write(String.format(
                                "%s\t%d\t%d\t%d%s",
                                statistics.getId(),
                                i + 1,
                                l == -1 ? 0 : l,
                                l == -1 ? 1 : 0,
                                SEPARATOR
                        ));
                    }
                }
            }
        }
        detailWriter.close();

        BufferedWriter tpsWriter = new BufferedWriter(new FileWriter(outputTps));
        tpsWriter.write("s");
        for (IExecutable executable : executables) {
            if (executable != null) {
                IStatistics statistics = executable.getStatistics();
                if (statistics != null)
                    tpsWriter.write(String.format("\t%s", statistics.getId()));
            }
        }
        tpsWriter.write("\t" + "total" + SEPARATOR);

        long ll = -1;
        for (Map.Entry entry : tpsMap.entrySet()) {
            if (ll == -1)
                ll = (Long) entry.getKey();

            tpsWriter.write(String.valueOf((Long) entry.getKey() - ll));

            long total = 0;
            for (IExecutable executable : executables) {
                if (executable != null) {
                    IStatistics statistics = executable.getStatistics();
                    if (statistics != null) {
                        Integer value = ((TreeMap<String, Integer>) entry.getValue()).get(statistics.getId());
                        tpsWriter.write("\t" + (value == null ? "" : value));
                        total += (value == null ? 0 : value);
                    }
                }
            }
            tpsWriter.write("\t" + total + SEPARATOR);
        }

        tpsWriter.close();
    }
}
