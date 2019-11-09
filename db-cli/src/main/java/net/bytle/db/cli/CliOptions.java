package net.bytle.db.cli;

import net.bytle.cli.CliCommand;

import static net.bytle.db.cli.Words.*;

public class CliOptions {

    /**
     * Add the copy options to the command
     * @param command
     */
    public static void addCopyOptions(CliCommand command) {
        command.optionOf(SOURCE_FETCH_SIZE_OPTION);
        command.optionOf(SOURCE_QUERY_OPTION);
        command.optionOf(BUFFER_SIZE_OPTION);
        command.optionOf(TARGET_WORKER_OPTION);
        command.optionOf(COMMIT_FREQUENCY_OPTION);
        command.optionOf(TARGET_BATCH_SIZE_OPTION);
        command.optionOf(METRICS_PATH_OPTION);
    }
}
