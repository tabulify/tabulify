package net.bytle.db.cli;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.db.Tabular;
import net.bytle.db.spi.DataPath;
import net.bytle.db.transfer.TransferProperties;
import net.bytle.log.Log;

import static net.bytle.db.cli.Words.*;

public class CliOptions {

    private static final Log LOGGER = DbCliLog.LOGGER_DB_CLI;

    /**
     * Add the copy options to the command
     *
     * @param command
     */
    public static void addMoveOptions(CliCommand command) {

        command.optionOf(SOURCE_FETCH_SIZE_OPTION)
                .setGroup("Move Options")
                .setDescription("defines the fetch size against the SOURCE database")
                .setDefaultValue(TransferProperties.DEFAULT_FETCH_SIZE);

        // No default because it's fetch size dependent
        command.optionOf(BUFFER_SIZE_OPTION)
                .setGroup("Move Options")
                .setDescription("defines the size of the buffer between SOURCE and TARGET threads");

        command.optionOf(TARGET_WORKER_OPTION)
                .setGroup("Move Options")
                .setDescription("defines the TARGET number of thread against the TARGET database")
                .addDefaultValue(TransferProperties.DEFAULT_TARGET_WORKER_COUNT);

        command.optionOf(COMMIT_FREQUENCY_OPTION)
                .setGroup("Move Options")
                .setDescription("defines the commit frequency against the TARGET table by batch")
                .addDefaultValue(TransferProperties.DEFAULT_COMMIT_FREQUENCY);

        command.optionOf(TARGET_BATCH_SIZE_OPTION)
                .setGroup("Move Options")
                .setDescription("defines the batch size against the TARGET table")
                .addDefaultValue(TransferProperties.DEFAULT_BATCH_SIZE);

        command.optionOf(METRICS_DATA_URI_OPTION)
                .setGroup("Move Options")
                .setDescription("defines a data uri where the data metrics should be saved");

    }

    public static TransferProperties getMoveOptions(CliParser cliParser) {

        DataPath metricsDataPath = Tabular.tabular().getDataPath(cliParser.getString(METRICS_DATA_URI_OPTION));

        Integer batchSize = cliParser.getInteger(TARGET_BATCH_SIZE_OPTION);
        Integer fetchSize = cliParser.getInteger(SOURCE_FETCH_SIZE_OPTION);
        Integer commitFrequency = cliParser.getInteger(COMMIT_FREQUENCY_OPTION);
        int targetWorkerCount = cliParser.getInteger(Words.TARGET_WORKER_OPTION);
        Integer bufferSize = cliParser.getInteger(BUFFER_SIZE_OPTION);
        if (bufferSize == null) {
            bufferSize = 2 * targetWorkerCount * fetchSize;
            LOGGER.info(BUFFER_SIZE_OPTION + " parameter NOT found. Using default : " + bufferSize);
        }

        return TransferProperties.of()
                .setQueueSize(bufferSize)
                .setTargetWorkerCount(targetWorkerCount)
                .setMetricsPath(metricsDataPath)
                .setFetchSize(fetchSize)
                .setBatchSize(batchSize)
                .setCommitFrequency(commitFrequency);
    }
}
