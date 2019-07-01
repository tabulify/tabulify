package net.bytle.cli;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


@SuppressWarnings("WeakerAccess")
public class Clis {

    private static final Logger LOGGER = CliLog.getCliLog().getLogger();


    /**
     * The only static function to return the first command called the cli.
     * The cli is the root command in the chain of command.
     * When you want to create a child command, you will call the
     * non-static function {@link CliCommand#commandOf(String)}
     * <p>
     * Normally, you call this function only once.
     * The function will always return a new object. There is no cache.
     *
     * @param commandName the name of the cli which is what you need to type in the console to call it
     * @return a cli (ie a root {@link CliCommand}
     */
    @SuppressWarnings("WeakerAccess")
    public static CliCommand getCli(String commandName) {

        return new CliCommand(commandName);

    }

    @SuppressWarnings("WeakerAccess")
    public static CliParser getParser(CliCommand cliCommand, String[] args) {
        return new CliParser(cliCommand, args);
    }

    public static void createGlobalConfigFile(CliCommand cliCommand) {

        List<CliWord> globalWords = cliCommand.getGlobalWords();
        BufferedWriter outputStream;

        try {
            outputStream = Files.newBufferedWriter(cliCommand.getGlobalConfigFile());

            final String commentWord = "#";
            for (CliWord cliWord : globalWords) {
                if (cliWord.isInConfigFile()) {
                    final String description = cliWord.getDescription();
                    if (description != null) {
                        outputStream.write(commentWord + description);
                    }
                    outputStream.newLine();
                    List<String> defaultValues = cliWord.getDefaultValues();
                    if (defaultValues.size() == 0) {
                        defaultValues = new ArrayList<>();
                        defaultValues.add("value");
                    }
                    outputStream.write(commentWord + cliWord.getName() + "=" + String.join(",", defaultValues));
                    outputStream.newLine();
                    outputStream.newLine();
                }
            }
            outputStream.flush();
            outputStream.close();

        } catch (IOException e) {

            throw new RuntimeException(e);

        }
    }


}
