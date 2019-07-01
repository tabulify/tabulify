package net.bytle.cli;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;


/**
 * Created by gerard on 20-06-2017.
 * <p>
 */
public class CliParser {

    private static final Logger LOGGER = CliLog.getCliLog().getLogger();

    @SuppressWarnings("WeakerAccess")
    public static final String PREFIX_LONG_OPTION = "--";
    @SuppressWarnings("WeakerAccess")
    public static final String PREFIX_SHORT_OPTION = "-";


    private final String[] args;
    private final CliCommand cliCommand;


    // Verbosity
    private CliWord verboseWord;

    // Contains the word that are recognized
    private Map<String, List<String>> foundWords;

    // A logger initialized in the {@link CliCommand#getLogger} function
    private final Logger logger = CliLog.getCliLog().getLogger();


    /**
     * use the other constructor {@link #CliParser(CliCommand, String[])}
     */
    @SuppressWarnings("unused")
    private CliParser() {
        args = new String[0];
        cliCommand = null;
    }

    CliParser(CliCommand cliCommand, String[] args) {
        this.cliCommand = cliCommand;
        this.args = args;
    }

    @Override
    public String toString() {
        return "CliParser for the command " + cliCommand;
    }

    /**
     * Lazy initialization
     * This function is called when a get function is called
     */
    private void parse() {

        LOGGER.fine("Parsing the command (" + this.cliCommand.getName() + ")");

        // Not null to show that the parse was done
        foundWords = new HashMap<>();

        /**
         * In the parse functions
         * The value is set if any value is null
         */
        // Parse the command arguments
        parseCommandArgument();


        // Only if there is no childCommand
        //
        // At first, this was to suppress the repetition
        // of log showing that the config word was searched
        // for each command and sub-command
        // but this rule was applied for other parsing
        if (this.getChildCommands().size() == 0) {

            // Parse the environment variable
            parseEnvironmentVariable();

            // Parse the config file
            parseConfigFiles();

            // System
            parseSystemProperties();

            // Default value
            initWordsWithDefaultValue();

        }

        // Help
        // Help check must be before mandatory check
        // Otherwise the mandatory check will stop the process
        checkHelpOption();

        // Mandatory check
        checkMandatoryField();

        // Verbose
        checkLogVerbosity();

    }

    /**
     * Add the value of the system properties variable coupled to cliWords
     * by {@link CliWord#setSystemPropertyName(String)}
     */
    private void parseSystemProperties() {
        // Parse the environment variable
        // List of options with a system property name
        Map<String, CliWord> systemPropertyMap = cliCommand.getProperties().stream()
                .filter(x -> x.getSystemPropertyName() != null)
                .collect(Collectors.toMap(CliWord::getSystemPropertyName, Function.identity()));

        //noinspection Duplicates
        if (systemPropertyMap.size() > 0) {
            for (String systemPropertyKey : systemPropertyMap.keySet()) {
                String value = System.getProperty(systemPropertyKey);
                if (value != null) {
                    String name = systemPropertyMap.get(systemPropertyKey).getName();
                    // If any value is already set discard it
                    if (!foundWords.containsKey(name)) {
                        foundWords.put(systemPropertyMap.get(systemPropertyKey).getName(), Collections.singletonList(value));
                    }
                }
            }
        }
    }

    /**
     * Add the default value to the foundWords
     * defined by {@link CliWord#setDefaultValue(Object)}
     */
    private void initWordsWithDefaultValue() {
        for (CliWord word : cliCommand.getWords()) {
            if (word.getDefaultValues().size() > 0) {
                // If any value is already set discard it
                if (!foundWords.containsKey(word.getName())) {
                    foundWords.put(word.getName(), word.getDefaultValues());
                }
            }
        }
    }

    /**
     * Check the mandatory fields and exit if any is missing
     */
    private void checkMandatoryField() {
        for (CliWord word : cliCommand.getMandatoryWords()) {
            if (!foundWords.keySet().contains(word.getName())) {
                logger.severe("The " + word.getTypeName() + " " + word.getName() + " is mandatory and was not found");
                CliUsage.print(cliCommand, 2);
                System.exit(1);
            }
        }
    }

    /**
     * Add the value of environment variable coupled to cliWords
     * by {@link CliWord#setEnvName(String)}
     */
    private void parseEnvironmentVariable() {
        // Parse the environment variable
        // List of options
        Map<String, CliWord> envNamesMap = cliCommand.getProperties().stream()
                .filter(x -> x.getEnvName() != null)
                .collect(Collectors.toMap(CliWord::getEnvName, Function.identity()));
        if (envNamesMap.size() > 0) {
            Set<String> envNames = envNamesMap.keySet();
            Map<String, String> envs = System.getenv();
            for (String envKey : envs.keySet()) {
                if (envNames.contains(envKey)) {
                    String name = envNamesMap.get(envKey).getName();
                    // If any value is already set discard it
                    if (!foundWords.containsKey(name)) {
                        foundWords.put(name, Collections.singletonList(envs.get(envKey)));
                    }
                }
            }
        }
    }

    private void parseConfigFiles() {

        List<Path> configFiles = new ArrayList<>();

        // The path can be set directly  via client code
        // with the setConfigPath
        Path globalConfigPath = cliCommand.getGlobalConfigFile();
        if (globalConfigPath != null) {
            configFiles.add(globalConfigPath);
        }

        // If it's given via a parameter
        if (cliCommand.getConfigWord() != null) {

            // Do we found it in an env or in the command line already
            Path configPath = getPath(cliCommand.getConfigWord());
            if (configPath != null) {
                configFiles.add(configPath);
            }

        }


        Properties properties = new Properties();
        for (Path path : configFiles) {
            try {
                if (Files.exists(path)) {
                    properties.load(Files.newInputStream(path));
                } else {
                    logger.fine("The config file (" + path.toAbsolutePath().toString() + ") was not found.");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        // Parse the config file
        if (properties.size() > 0) {

            // List of options
            List<String> optionNames = cliCommand.getProperties()
                    .stream()
                    .map(CliWord::getName)
                    .collect(Collectors.toCollection(ArrayList::new));


            for (Object key : properties.keySet()) {

                String propertyName = String.valueOf(key);

                if (optionNames.contains(propertyName)) {
                    CliWord cliWord = cliCommand.wordOf(propertyName);
                    // If found
                    if (cliWord != null) {

                        // If any value is already set discard it
                        if (!foundWords.containsKey(propertyName)) {
                            String value = null;
                            if (cliWord.hasValue()) {
                                value = (String) properties.get(key);
                            }
                            foundWords.put(cliWord.getName(), Collections.singletonList(value));
                        } else {
                            LOGGER.fine("The option (" + propertyName + ") was found in the config file but as the value was already known, it was discarded");
                        }

                    }
                }
            }
        }

    }

    /**
     * Parse the args into a list of word with or without value
     */
    private void parseCommandArgument() {
        // Number of main argument recognized as cliCommand argument
        Integer argNumber = 0;
        // Number of defined cliCommand args (above this limit, the values are added to the last one)
        Integer argsSize = cliCommand.getArgs().size();

        // A list of the sub-command name to check if the main argument is a sub-command
        final List<CliCommand> cliCommandChainFromChild = cliCommand.getParentsCommands();
        cliCommandChainFromChild.addAll(cliCommand.getChildCommands());
        List<String> cliNames = cliCommandChainFromChild.stream()
                .map(CliCommand::getName)
                .collect(Collectors.toCollection(ArrayList::new));

        // A list of the options to check if the main argument is a sub-command
        List<String> optionLongQualifiedNames = cliCommand.getProperties().stream()
                .map(x -> PREFIX_LONG_OPTION + x)
                .collect(Collectors.toCollection(ArrayList::new));

        // A list of the options to check if the main argument is a sub-command
        List<String> optionShortQualifiedNames = cliCommand.getProperties().stream()
                .filter(x -> x.getShortName() != null)
                .map(x -> PREFIX_SHORT_OPTION + x.getShortName())
                .collect(Collectors.toCollection(ArrayList::new));

        for (int i = 0; i < args.length; i++) {

            String s = args[i];

            // If the main arg value is a long option
            if (s.startsWith(PREFIX_LONG_OPTION)) {

                if (optionLongQualifiedNames.contains(s)) {

                    logger.fine("Long Property found !" + s);
                    CliWord cliWord = cliCommand.wordOf(s.substring(PREFIX_LONG_OPTION.length()));
                    i = processOptions(i, cliWord);

                } else {

                    logger.fine("The option (" + s + ") is not defined and was ignored.");

                }
                continue;

            }

            // If the main arg value is a short option
            if (s.startsWith(PREFIX_SHORT_OPTION)) {

                if (optionShortQualifiedNames.contains(s)) {

                    logger.fine("Short Property found !" + s);
                    CliWord cliWord = cliCommand.wordOf(s.substring(PREFIX_SHORT_OPTION.length(), s.length()));
                    i = processOptions(i, cliWord);

                } else {

                    logger.fine("The option (" + s + ") is not defined and was ignored.");

                }
                continue;

            }


            // This is an argument or a command
            if (cliNames.contains(s)) {

                // This is a command string
                foundWords.put(s, null);
                continue;

            }


            // This is may be an argument
            if (cliCommand.getArgs().size() > 0) {

                // If we don't have another argument to store the value, we add it to the last one
                if (argNumber + 1 > argsSize) {

                    final CliWord lastArg = cliCommand.getArgs().get(argsSize - 1);
                    List<String> lastArgValues = foundWords.get(lastArg.getName());
                    lastArgValues.add(s);
                    final String msg = "Their is only " + argsSize + " argument(s) defined and the value (" + s + ") is the " + (argNumber + 1) + " arguments. It was added to the argument " + lastArg.getName();
                    logger.fine(msg);

                } else {

                    final List<String> values = new ArrayList<>();
                    values.add(s);
                    foundWords.put(cliCommand.getArgs().get(argNumber).getName(), values);
                    argNumber++;

                }

            } else {

                // This happens really often using multiple sub-command
                // The first command does not have any notion of the others sub-command
                // and you got this message
                logger.fine("The word (" + s + ") is not defined as a command and no arguments were defined. It was ignored.");

            }

        }
    }

    /**
     * Use in the long and short option processing
     * to suppress duplicate code
     *
     * @param i       - the loop counter
     * @param cliWord - the cliWord
     * @return the loop counter
     */
    private int processOptions(int i, CliWord cliWord) {
        if (cliWord.hasValue()) {

            // Go to the next args value
            i++;
            List<String> strings = foundWords.computeIfAbsent(cliWord.getName(), k -> new ArrayList<>());
            strings.add(args[i]);

        } else {

            foundWords.put(cliWord.getName(), null);

        }
        return i;
    }


    /**
     * The log verbosity
     */
    private void checkLogVerbosity() {

        if (verboseWord != null) {
            if (getBoolean(verboseWord)) {

                CliLog.getCliLog().makeLoggerVerbose();

            }
        }

    }


    /**
     * Return: true if a word is present
     *
     * @param cliWord the word
     * @return the value of the word
     */
    @SuppressWarnings("WeakerAccess")
    public Boolean getBoolean(CliWord cliWord) {

        if (cliWord == null) {
            return null;
        } else {
            return getBoolean(cliWord.getName());
        }


    }

    /**
     * If an option has:
     * * no value, return true if an option is present, false otherwise
     * * value, return the output of the function @{link Boolean#valueOf}
     *
     * @param word - the word name
     * @return the word value
     */
    @SuppressWarnings("WeakerAccess")
    public Boolean getBoolean(String word) {

        if (foundWords == null) {
            parse();
        }
        Boolean b = foundWords.containsKey(word);

        if (b) {

            CliWord cliWord = cliCommand.wordOf(word);
            if (cliWord != null) {

                if (cliWord.hasValue()) {
                    String value = foundWords.get(cliWord.getName()).get(0);
                    b = Boolean.valueOf(value);
                }

            }

            logger.info("(" + word + ") word was found with the value: " + b);

        } else {

            logger.info("(" + word + ") word was not found");

        }

        return b;

    }


    /**
     * Return a number. Null if the option is not present
     *
     * @param word a word
     * @return the value of the word as double
     */
    @SuppressWarnings("WeakerAccess")
    public Double getDouble(CliWord word) {

        if (word == null) {
            return null;
        } else {
            return getDouble(word.getName());
        }

    }

    /**
     * Return a integer. Null if the option is not present
     *
     * @param word -  the word name
     * @return the integer value of the name
     */
    @SuppressWarnings("WeakerAccess")
    public Integer getInteger(CliWord word) {

        if (word == null) {
            return null;
        } else {
            return getInteger(word.getName());
        }

    }


    /**
     * Return a string value from an option
     *
     * @param word - a word
     * @return the word value as a string or null
     */
    @SuppressWarnings("WeakerAccess")
    public String getString(CliWord word) {

        if (word == null) {
            return null;
        } else {
            return getString(word.getName());
        }

    }

    /**
     * Return a list of the values
     *
     * @param word - The word
     * @return the values of the word as a list of string
     */
    @SuppressWarnings("WeakerAccess")
    public List<String> getStrings(CliWord word) {

        if (word == null) {
            return null;
        } else {
            return getStrings(word.getName());
        }

    }


    /**
     * @param word - a word
     * @return the value of the word as a path
     */
    @SuppressWarnings("WeakerAccess")
    public Path getPath(CliWord word) {

        return getPath(word.getName());

    }

    /**
     * @param word - a word string
     * @return the value of the word as a path
     */
    @SuppressWarnings("WeakerAccess")
    public Path getPath(String word) {

        if (foundWords == null) {
            parse();
        }

        String s = this.getString(word);
        if (s == null) {
            return null;
        } else {
            Path path;
            try {
                path = Paths.get(s);
            } catch (Exception e) {
                LOGGER.severe("The value (" + s + ") is not recognized as a path.");
                throw new RuntimeException(e);
            }
            return path;
        }

    }


    /**
     * Utility to return the string of a text file argument
     * It can be an argument or an option, this is why the arg is a string
     *
     * @param word - the word
     * @param fail - indicates if the method will exit the process or return null if any error occurs
     * @return - the content of a file as as string
     */
    @SuppressWarnings("WeakerAccess")
    public String getFileContent(CliWord word, Boolean fail) {

        if (word == null) {
            return null;
        } else {
            return getFileContent(word.getName(), fail);
        }
    }


    /**
     * Define a boolean option in order to show
     * all message (fine and finest level included)
     *
     * @param word - the word that will trigger the printing of all log message
     * @return - the CliParser to allow a chain initialization
     */
    @SuppressWarnings("unused")
    public CliParser verboseWord(CliWord word) {
        verboseWord = word;
        return this;
    }

    /**
     * Use in the test
     *
     * @return the number of word recognized (without the system words)
     */
    Integer getNumberOfFoundWords() {
        if (foundWords == null) {
            parse();
        }
        return foundWords.size();
    }


    /**
     * @return a list of the child commands founds
     */
    @SuppressWarnings("WeakerAccess")
    public List<CliCommand> getChildCommands() {

        if (foundWords == null) {
            parse();
        }

        assert cliCommand != null;
        return foundWords.keySet().stream()
                .map(cliCommand::wordOf)
                .filter(CliWord::isCommand)
                .map(cliCommand::getChildCommand)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));


    }


    /**
     * @return the cliCommand that has initialized this parser
     * <p>
     * This is handy when you pass the cliParser to a function
     * and that you want to retrieve the cliCommand.
     */
    @SuppressWarnings("WeakerAccess")
    public CliCommand getCommand() {

        if (foundWords == null) {
            parse();
        }

        List<CliCommand> cliCommands = getChildCommands();
        if (cliCommands.size() > 0) {
            return cliCommands.get(0);
        } else {
            return null;
        }

    }

    /**
     * @return the found arguments
     */
    @SuppressWarnings("WeakerAccess")
    public List<CliWord> getArgs() {

        if (foundWords == null) {
            parse();
        }

        return foundWords.keySet().stream()
                .map(cliCommand::wordOf)
                .filter(CliWord::isArg)
                .collect(Collectors.toCollection(ArrayList::new));

    }

    /**
     * @return the found options
     */
    @SuppressWarnings("WeakerAccess")
    public List<CliWord> getOptions() {

        if (foundWords == null) {
            parse();
        }

        return foundWords.keySet().stream()
                .map(cliCommand::wordOf)
                .filter(CliWord::isOption)
                .collect(Collectors.toCollection(ArrayList::new));

    }

    /**
     * @param word - the word
     * @return - the value of the word as string. If the word as several values, return the first one.
     */
    @SuppressWarnings("WeakerAccess")
    public String getString(String word) {

        CliWord cliWord = cliCommand.wordOf(word);
        List<String> values = getStrings(cliWord);
        String s = null;
        if (values.size() != 0) {
            s = String.join(" ", values);

            logger.info("(" + cliWord + ") word was found with the value: " + s);

        } else {

            logger.info("(" + cliWord + ") word was not found");

        }
        return s;

    }

    /**
     * @param word - the word
     * @return - the value of the word as integer. If the word has several values, return the first one.
     */
    @SuppressWarnings("WeakerAccess")
    public Integer getInteger(String word) {

        if (foundWords == null) {
            parse();
        }

        Integer i = null;
        final List<String> strings = foundWords.get(word);
        if (strings != null) {
            String value = strings.get(0);
            if (value != null) {
                try {
                    i = Integer.valueOf(value);
                } catch (Exception e) {
                    logger.severe("The word " + word + " with the value (" + value + ") is not a integer.");
                    CliUsage.print(cliCommand, 2);
                    System.exit(1);
                }
            }
        }
        return i;

    }

    /**
     * @param word - the word
     * @return - the value of the word as double. If the word has several values, return the first one
     */
    @SuppressWarnings("WeakerAccess")
    public Double getDouble(String word) {

        if (foundWords == null) {
            parse();
        }

        Double d = null;
        final List<String> strings = foundWords.get(word);
        if (strings != null) {
            String value = strings.get(0);
            if (value != null) {
                try {
                    d = Double.valueOf(value);
                    LOGGER.info("(" + word + ") word was found with the value:" + d);
                } catch (Exception e) {
                    logger.severe("The " + word + " with the value (" + value + ") is not a double.");
                    CliUsage.print(cliCommand, 2);
                    System.exit(1);
                }
            }
        }
        return d;

    }

    /**
     * @param word - the word
     * @return - the values of the word as a list of string
     */
    @SuppressWarnings("WeakerAccess")
    public List<String> getStrings(String word) {

        if (foundWords == null) {
            parse();
        }

        List<String> strings = foundWords.get(cliCommand.wordOf(word).getName());
        if (strings == null) {
            strings = new ArrayList<>();
        }
        return strings;

    }

    /**
     * @param word - a word name or a path
     * @return - the content of the file as string that is defined by the value of the word
     * <p>
     * if the path defines a file that does not exist,
     * it will exit
     */
    @SuppressWarnings("WeakerAccess")
    public String getFileContent(String word) {
        return getFileContent(word, true);
    }

    /**
     * @param word - a word name or a path
     * @param exit -  if the path defines a file that does not exist, it will exit
     * @return - the content of the file defined by the value of the word as path
     */
    @SuppressWarnings("WeakerAccess")
    public String getFileContent(String word, Boolean exit) {

        try {

            String pathAsString;
            if (cliCommand.hasWord(word)) {
                pathAsString = getString(word);
            } else {
                // This is a path
                pathAsString = word;
            }

            Path path = Paths.get(pathAsString);
            if (Files.exists(path)) {
                if (Files.isDirectory(path)) {
                    String msg = "The path word value (" + path.toAbsolutePath() + ") is not a file but a directory.";
                    if (noExit(exit, msg)) return null;
                }
            } else {
                String msg = "The path word value (" + path.toAbsolutePath() + ") does not exist.";
                if (noExit(exit, msg)) return null;
            }

            // Get the content
            StringBuilder s = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(path.toFile()));
            String line;
            while ((line = reader.readLine()) != null) {
                s.append(line).append(System.getProperty("line.separator"));
            }
            return s.toString();

        } catch (IOException | InvalidPathException e) {
            throw new RuntimeException(e);
        }

    }

    private boolean noExit(Boolean exit, String msg) {
        if (exit) {
            logger.severe(msg);
            CliUsage.print(cliCommand, 2);
            System.exit(1);
        } else {
            logger.warning(msg);
            return true;
        }
        return false;
    }

    /**
     * @param word - the word
     * @return - the command if found or null
     */
    @SuppressWarnings("WeakerAccess")
    public CliCommand getCommand(String word) {

        if (foundWords == null) {
            parse();
        }

        if (getBoolean(word)) {
            return cliCommand.commandOf(word);
        } else {
            return null;
        }

    }

    /**
     * @param word - a word
     * @return - the content of file defined by the value of the word
     */
    @SuppressWarnings("WeakerAccess")
    public String getFileContent(CliWord word) {
        return getFileContent(word, true);
    }

    /**
     * An utility function to check the help option and print it if present
     */
    void checkHelpOption() {

        if (foundWords == null) {
            parse();
        }

        // If no child command parsed, the help is for this command
        if (this.getChildCommands().size() == 0) {
            CliWord helpWord = cliCommand.getHelpWord();
            if (helpWord != null) {
                if (this.getBoolean(helpWord.getName())) {
                    CliUsage.print(cliCommand);
                    System.exit(0);
                }
            }
        }

    }

}
