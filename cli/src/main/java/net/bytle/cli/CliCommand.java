package net.bytle.cli;

import net.bytle.log.Log;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by gerard on 20-06-2017
 * <p>
 * A command
 * <p>
 * The root command is called the appHome command
 * <p>
 * A command can have:
 * * children commands creating a chain of command
 * * options
 * * argument
 */
public class CliCommand {

    // The directory below the App Home where the config file could be found
    public static final String CONF_DIR = "conf";
    // The default value
    public static final String DEFAULT_APP_HOME_WORD = "BCLI_APP_HOME";
    private static final java.util.logging.Logger LOGGER = Log.getCliLog().getLogger();
    // The name of the appHome (ie command)
    private final String name;
    // To retrieve a local word by name
    private final Map<String, CliWord> localWordsMap = new HashMap<>();
    // Only for the parent command
    private HashMap<String, CliCommand> childCommandsMap = new HashMap<>();
    // To preserve the order
    private List<CliCommand> childCommandsList = new ArrayList<>();
    // Only for the child command
    private CliCommand parentCliCommand;
    // General properties
    private String description;
    // To retrieve the position of the arg on this client
    private List<CliWord> localWordsList = new ArrayList<>();
    // Only for the root appHome
    // A repository of word that is normally only used in the root
    // To retrieve the word by name
    private Map<String, CliWord> globalWordMap = new HashMap<>();
    // To retrieve the position of the word on the global list
    private List<CliWord> globalWordList = new ArrayList<>();
    // The path to the configuration file or its word
    private Path globalConfigPath;
    private String configWord;
    // Example and footer for the usage text
    private String example;
    private String footer;
    // Example: the global help Word
    private CliWord helpWord = null;
    // The app home word
    private CliWord appHomeWord;


    /**
     * Called through the {@link Clis#getCli(String)}
     * this is then technically the root
     *
     * @param name
     */
    protected CliCommand(String name) {

        this.name = name;

        // Normally, this is the parent because
        // this function is called only for the root command
        // but who knows
        CliCommand rootCommand = this.getRootCommand();
        if (rootCommand.appHomeWord == null) {
            CliWord appHome = this.getRootCommand().optionOf(DEFAULT_APP_HOME_WORD)
                    .setDescription("Define the app home directory")
                    .setSystemPropertyName(DEFAULT_APP_HOME_WORD)
                    .setEnvName(DEFAULT_APP_HOME_WORD);
            rootCommand.appHomeWord = appHome;
        }

    }

    public CliCommand(CliCommand cliCommand, String childCommand) {

        this.parentCliCommand = cliCommand;
        this.name = childCommand;


    }


    @SuppressWarnings("WeakerAccess")
    public CliWord optionOf(String name) {

        return wordOf(name)
                .setTypeAsOption();

    }

    private CliWord getGlobalWord(String name) {
        CliCommand rootCommand = getRootCommand();

        // Long name
        CliWord cliWord = rootCommand.globalWordMap.get(name);
        if (cliWord != null) {
            return cliWord;
        }

        // Short name
        List<CliWord> cliWords = rootCommand.globalWordMap
                .entrySet()
                .stream()
                .map(s -> s.getValue())
                .filter(s -> s.getShortName() != null && s.getShortName().equals(name))
                .collect(Collectors.toList());

        switch (cliWords.size()) {
            case 1:
                return cliWords.get(0);
            case 0:
                return null;
            default:
                throw new RuntimeException("The following words have the same short option name (" + cliWords + ")");
        }


    }

    @SuppressWarnings("WeakerAccess")
    public CliWord argOf(String word) {

        return wordOf(word)
                .setTypeAsArg();

    }

    @SuppressWarnings("WeakerAccess")
    public String getDescription() {
        if (description == null) {
            return "Description not known.";
        } else {
            return description;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public CliCommand setDescription(String desc) {
        this.description = desc;
        return this;
    }

    /**
     * Return a child (command|appHome) from this (client|command)
     * creating a (hierarchy|chain) of command
     *
     * @param word - the new word name that is used to create the command
     * @return a {@link CliCommand}
     */
    @SuppressWarnings("WeakerAccess")
    public CliCommand commandOf(String word) {

        CliCommand childCommand = childCommandsMap.get(word);

        if (childCommand == null) {

            // Create the appHome
            childCommand = new CliCommand(this, word);
            childCommandsMap.put(word, childCommand);
            childCommandsList.add(childCommand);

            // Add the appHome as a word for the parsing process
            wordOf(word).setTypeAsCommand();

        }
        return childCommand;

    }

    /**
     * @return The name of the command
     */
    @SuppressWarnings("WeakerAccess")
    public String getName() {
        return name;
    }

    /**
     * @return the arg words of the cliCommand
     */
    @SuppressWarnings("WeakerAccess")
    public List<CliWord> getArgs() {

        return localWordsList.stream()
                .filter(CliWord::isArg)
                .collect(Collectors.toCollection(ArrayList::new));

    }

    /**
     * @return a list of the children command of this command
     */
    @SuppressWarnings("WeakerAccess")
    public List<CliCommand> getChildCommands() {

        return childCommandsList;

    }

    /**
     * From the actual appHome to the parent
     * * the children are not included
     * * the root also as this will not be a word
     * <p>
     * If you want a full chain of command (ie with the root), see {@link #getFullParentsCommand()}
     *
     * @return a list of all command name from this command to the appHome (the root command)
     */
    @SuppressWarnings("WeakerAccess")
    public List<CliCommand> getParentsCommands() {

        List<CliCommand> cliCommands = new ArrayList<>();
        buildCliChain(this, cliCommands);
        Collections.reverse(cliCommands);
        return cliCommands;

    }

    /**
     * @return Chain of command with the root
     */
    public List<CliCommand> getFullParentsCommand() {

        List<CliCommand> cliCommands = getParentsCommands();
        cliCommands.add(0, this.getRootCommand());
        return cliCommands;

    }

    /**
     * A recursive function to build the cliCommand chain for the function {@link #getParentsCommands()}
     *
     * @param cliCommand      the cliCommand that must have a parent
     * @param cliCommandChain the list to build
     */
    private void buildCliChain(CliCommand cliCommand, List<CliCommand> cliCommandChain) {

        // The root is not in the chain
        if (cliCommand.parentCliCommand != null) {
            cliCommandChain.add(cliCommand);
            buildCliChain(cliCommand.parentCliCommand, cliCommandChain);
        }

    }

    /**
     * @return the properties of this command (local and of its parents)
     */
    @SuppressWarnings("WeakerAccess")
    public List<CliWord> getProperties() {

        List<CliWord> properties = this.getParentProperties();
        ArrayList<CliWord> collectProperties = this.localWordsList
                .stream()
                .filter(x -> x.isOption() || x.isFlag())
                .collect(Collectors.toCollection(ArrayList::new));
        properties.addAll(collectProperties);
        return properties;

    }

    /**
     * @return the local options of this command (without the option of its parents)
     */
    @SuppressWarnings("WeakerAccess")
    public List<CliWord> getLocalProperties() {

        return localWordsList.stream()
                .filter(x -> x.isProperty() && x != this.getAppHomeWord())
                .collect(Collectors.toCollection(ArrayList::new));

    }

    /**
     * @return the local flag of this command (without the option of its parents)
     */
    @SuppressWarnings("WeakerAccess")
    public List<CliWord> getLocalFlags() {

        return localWordsList.stream()
                .filter(x -> x.isFlag() && x != this.getAppHomeWord())
                .collect(Collectors.toCollection(ArrayList::new));

    }

    /**
     * Create a global word definition in order to share the same definition
     * between different commands.
     * <p>
     * This function may be called normally only from the root command (the appHome)
     * but it's also possible from a child
     * <p>
     * If you add a word to a command using the same name, this definition will be used.
     *
     * @param name the name of the created word globally
     * @return a word
     */
    @SuppressWarnings("WeakerAccess")
    public CliWord globalWordOf(String name) {

        // May be we need an extra object to manage the option NAMESPACE ?

        CliWord word = getGlobalWord(name);
        if (word == null) {
            word = new CliWord(this, name);
            globalWordMap.put(name, word);
            globalWordList.add(word);
        }
        return word;

    }

    /**
     * Return the root command of the chain (the appHome)
     * If the appHome is the root, it returns itself
     *
     * @return the root command (ie the appHome)
     */
    @SuppressWarnings("WeakerAccess")
    public CliCommand getRootCommand() {

        CliCommand root = this;
        if (this.parentCliCommand != null) {
            root = getRootCommand(this.parentCliCommand);
        }
        return root;

    }

    /**
     * Recursive function used by {@link #getRootCommand()}
     * to locate the root command
     *
     * @param cliCommand - the start command in the chain - normally the called command
     * @return the root command
     */
    private CliCommand getRootCommand(CliCommand cliCommand) {
        if (cliCommand.parentCliCommand != null) {
            return getRootCommand(cliCommand.parentCliCommand);
        } else {
            return cliCommand;
        }
    }

    /**
     * @return a list of all words defined globally with the function {@link #globalWordOf(String)}
     */
    @SuppressWarnings("WeakerAccess")
    public List<CliWord> getGlobalWords() {
        CliCommand cliCommand = getRootCommand();
        return cliCommand.globalWordList;
    }

    /**
     * To retrieve a word from the local command
     * or its parents.
     * <p>
     * To create a word, you use:
     * * {@link #optionOf(String)}
     * * {@link #argOf(String)}
     * * {@link #commandOf(String)}
     *
     * @param name the word name to create
     * @return a word
     */
    CliWord wordOf(String name) {

        // The word may be a parent word
        // Generally a command word
        CliWord cliWord = getParentWords()
                .stream()
                .collect(Collectors.toMap(CliWord::getName, Function.identity()))
                .get(name);
        if (cliWord != null) {
            return cliWord;
        }

        // Short name Options
        cliWord = localWordsMap.values()
                .stream()
                .filter(x -> x.getShortName() != null)
                .collect(Collectors.toMap(CliWord::getShortName, Function.identity()))
                .get(name);
        if (cliWord != null) {
            return cliWord;
        }

        // Long name
        cliWord = localWordsMap.get(name);
        if (cliWord == null) {
            // Do we have a global word definition
            cliWord = getGlobalWord(name);
            if (cliWord == null) {
                cliWord = new CliWord(this, name);
            }
            localWordsMap.put(name, cliWord);
            localWordsList.add(cliWord);
        }
        return cliWord;

    }

    /**
     * Return the command in the chain
     * <p>
     * This function is used by the {@link CliParser#getChildCommands()}
     * to return the found commands
     *
     * @param name - the word name of the command
     * @return the command found in the chain by name or null
     */
    CliCommand getCommandInChain(CliCommand cliCommand, String name) {

        CliCommand cli = cliCommand.childCommandsMap.get(name);
        if (cli == null && cliCommand.parentCliCommand != null) {
            cli = getCommandInChain(cliCommand.parentCliCommand, name);
        }
        return cli;

    }

    /**
     * @return an example string
     */
    @SuppressWarnings("WeakerAccess")
    public String getExample() {
        return example;
    }

    @SuppressWarnings("WeakerAccess")
    public CliCommand setExample(String example) {
        this.example = example;
        return this;
    }

    /**
     * @return the footer to print in a usage
     */
    @SuppressWarnings("WeakerAccess")
    public String getFooter() {
        return this.footer;
    }

    @SuppressWarnings("WeakerAccess")
    public CliCommand setFooter(String footer) {
        this.footer = footer;
        return this;
    }

    /**
     * @param word -  a word
     * @return true if the word is defined for this cliCommand
     */
    @SuppressWarnings("WeakerAccess")
    public boolean hasWord(String word) {
        return localWordsMap.keySet().contains(word);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CliCommand that = (CliCommand) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {

        return Objects.hash(name);
    }

    /**
     * @return the mandatory words
     */
    List<CliWord> getMandatoryWords() {
        return localWordsList.stream()
                .filter(CliWord::isMandatory)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * @return local and parent words
     */
    @SuppressWarnings("WeakerAccess")
    public List<CliWord> getWords() {
        List<CliWord> cliWords = new ArrayList<>(localWordsList);
        if (parentCliCommand != null) {
            cliWords.addAll(parentCliCommand.getWords());
        }
        return cliWords;
    }

    /**
     * @return parent words
     */
    @SuppressWarnings("WeakerAccess")
    public List<CliWord> getParentWords() {
        List<CliWord> cliWords = new ArrayList<>();
        if (parentCliCommand != null) {
            cliWords.addAll(parentCliCommand.localWordsList);
            cliWords.addAll(parentCliCommand.getParentWords());
        }
        return cliWords;
    }

    @SuppressWarnings("WeakerAccess")
    public List<CliWord> getParentProperties() {
        ArrayList<CliWord> cliWords = new ArrayList<>();
        if (parentCliCommand != null) {
            return getParentPropertiesRec(parentCliCommand, cliWords);
        } else {
            return cliWords;
        }
    }

    /**
     * Use by {@link #getParentProperties()} to return recursively all parents options
     *
     * @param cliCommand - the parent command
     * @param cliWords   - the list of word to fiil in
     * @return - the whole list of parent words through the branch
     */
    private List<CliWord> getParentPropertiesRec(CliCommand cliCommand, List<CliWord> cliWords) {

        // Add local option
        cliWords.addAll(cliCommand.getLocalProperties());

        // Recurse
        CliCommand parentCli = cliCommand.parentCliCommand;
        if (parentCli != null) {
            cliWords.addAll(parentCli.getParentProperties());
            getParentPropertiesRec(parentCli, cliWords);
        }

        return cliWords;
    }

    @SuppressWarnings("WeakerAccess")
    public CliWord getHelpWord() {
        CliCommand root = getRootCommand();
        if (root.helpWord != null) {
            return root.helpWord;
        } else {
            return null;
        }
    }

    @SuppressWarnings("unused")
    public CliCommand setHelpWord(String helpWord) {
        CliCommand root = getRootCommand();
        root.helpWord = flagOf(helpWord);
        return this;
    }

    /**
     * @return the config file path
     * This function will never return null
     * and may return a file that doesn't yet exist
     */
    public Path getGlobalConfigFile() {

        CliCommand rootCommand = getRootCommand();
        Path configPath = rootCommand.globalConfigPath;

        // Still null, do we have it in a system property
        if (configPath == null && this.getConfigWord() != null) {

            String configSystemPropertyName = this.getConfigWord().getSystemPropertyName();
            if (configSystemPropertyName != null) {
                String configPathSystem = System.getProperty(configSystemPropertyName);
                if (configPathSystem != null) {
                    configPath = Paths.get(configPathSystem);
                }
            }

            // Still null, do we have a default value
            if (configPath == null) {
                List<String> defaultValues = this.getConfigWord().getDefaultValues();
                if (defaultValues.size() > 0) {
                    configPath = Paths.get(defaultValues.get(0));
                }
            }

        }


        // Still null, do we have it to the location( appHome/config/config.ini )
        if (configPath == null) {


            if (getAppHome() != null) {

                configPath = Paths.get(getAppHome().toString(), "conf", getRootCommand().getName() + ".ini");
            }

        }


        return configPath;

    }

    /**
     * @param path - the path to the config ini file
     * @return - a appHome command
     * <p>
     * This function can be called from all command
     * but there is only one value
     */
    @SuppressWarnings("WeakerAccess")
    public CliCommand setGlobalConfigFile(Path path) {

        if (!Files.isRegularFile(path)) {
            LOGGER.fine("The config file (" + path + ") is not a file");
        }
        getRootCommand().globalConfigPath = path;
        return this;

    }

    /**
     * @return the option name that must hold the config path
     */
    public CliWord getConfigWord() {

        final String configWord = getRootCommand().configWord;
        if (configWord != null) {
            return wordOf(configWord);
        } else {
            return null;
        }
    }

    /**
     * Set a config word
     *
     * @param configWord
     * @return the cliCommand for a chaining initialization
     */
    public CliCommand setConfigWord(String configWord) {

        getRootCommand().configWord = configWord;

        // Add it as word if it doesn't exist
        // The client do it but it may be bizzare to not see it if not defined as options
        getRootCommand().optionOf(configWord);

        return this;

    }

    /**
     * @param cliWord - a cliWord
     * @return - the cliCommand for this cliWord or null
     */
    public CliCommand getChildCommand(CliWord cliWord) {
        return this.childCommandsMap.get(cliWord.getName());
    }

    /**
     * @return the app home word (generally an env variable)
     */
    public CliWord getAppHomeWord() {
        CliCommand root = getRootCommand();
        return root.appHomeWord;
    }

    /**
     * Which words define the application home directory
     *
     * @param appHomeWord - the app home word
     * @return - the appHome command for chaining
     */
    public CliCommand setAppHomeWord(CliWord appHomeWord) {

        CliCommand root = getRootCommand();

        // Technically if the user change this
        // We will have two words and we need to delete the first one
        if (root.appHomeWord != null) {
            root.localWordsMap.remove(root.appHomeWord.getName());
        }
        root.appHomeWord = appHomeWord;
        return this;

    }

    public Path getAppHome() {

        String appHomeValue = null;
        CliWord appHomeWord = this.getAppHomeWord();
        if (appHomeWord != null) {
            if (appHomeWord.getEnvName() != null) {
                appHomeValue = System.getenv(appHomeWord.getEnvName());
            }
            if (appHomeValue == null && appHomeWord.getSystemPropertyName() != null) {
                appHomeValue = System.getProperty(appHomeWord.getSystemPropertyName());
            }
        }
        if (appHomeValue == null) {
            appHomeValue = ".";
        }
        return Paths.get(appHomeValue);

    }

    public CliWord flagOf(String name) {
        return wordOf(name)
                .setTypeAsFlag();
    }


    public CliWordGroup getGroup(String name) {
        return CliWordGroup.get(this, name);
    }
}
