package net.bytle.cli;

import net.bytle.type.Attribute;
import net.bytle.type.Key;
import net.bytle.type.Lists;
import net.bytle.type.Strings;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

/**
 * <p>
 * A command node in the command tree
 * <p>
 * A cli command can be the root node or the leaf node
 *
 *
 * <p>
 * A command can have:
 * * children commands creating a chain of command
 * * options
 * * flag
 */
public class CliCommand extends CliWord {


  /**
   * A name
   */
  private final String name;


  /**
   * The parent cli command
   * (null for the root)
   */
  private final CliCommand parentCliCommand;


  /**
   * To retrieve a word for this command
   * (ie at this level)
   * <p>
   * String Key is:
   * for an option (flag or property) `--option`
   * or
   * for a command or argument `command` or `arg`
   * <p>
   * You can then have the same alphabetic word for a property or a command
   * <p>
   * Example:
   * * the `conf` command
   * * the `--conf` options
   */
  private final Map<String, CliWord> localWordsMap = new HashMap<>();


  // Only for the root cli
  // A repository of word that is normally only used in the root
  // To retrieve the word by name
  private final Map<String, CliWord> globalWordMap = new HashMap<>();
  // To retrieve the position of the word on the global list
  private final List<CliWord> globalWordList = new ArrayList<>();


  // Example and footer for the usage text
  private final List<String> examples = new ArrayList<>();
  private String footer;

  // Example: the global help Word
  private CliWord helpWord = null;
  // The word for the version
  private CliWord versionWord = null;


  // The args to parse
  private String[] argsToParse;

  // Configuration
  private final Map<String, Object> conf = new HashMap<>();


  /**
   * Called through the {@link CliCommand#createRoot(String, String[])}
   * this is then technically the root
   *
   * @param name the name of the command
   */
  protected CliCommand(CliCommand cliCommand, String name) {
    super(cliCommand, name);

    this.parentCliCommand = cliCommand;
    this.name = name;
    this.setTypeAsCommand();

  }

  public static CliCommand createRoot(String commandName, String[] args) {
    return new CliCommand(null, commandName).setArgsToParse(args);
  }

  /**
   * Used only in test
   *
   * @param cliName the name of the cli
   * @return the cli command object for chaining
   */
  public static CliCommand createRootWithEmptyInput(String cliName) {
    String[] args = {};
    return createRoot(cliName, args);
  }


  /**
   *
   *
   * @param argsToParse the args that we got from the main java method
   * @return the command object
   */
  private CliCommand setArgsToParse(String[] argsToParse) {
    this.argsToParse = argsToParse;
    return this;
  }


  public CliWord addProperty(String longName) {

    nameOptionCheck(longName);
    return getOrCreateWordOf(longName)
      .setTypeAsProperty();

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
      .values()
      .stream()
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
  public CliWord addArg(String word) {

    nameArgAndCommandCheck(word);
    CliWord cliWord = getAllWords()
      .stream()
      .filter(s -> s.getName().equals(word))
      .findFirst()
      .orElse(null);
    if (cliWord != null) {
      throw new RuntimeException("The word (" + word + ") is already defined as a (" + cliWord.getType() + "). You can't add it as an argument.");
    }
    return getOrCreateWordOf(word)
      .setTypeAsArg();

  }

  protected static void nameArgAndCommandCheck(String word) {
    if (word.startsWith(CliParser.PREFIX_LONG_OPTION)) {
      throw new RuntimeException("A argument or command should not start with " + CliParser.PREFIX_LONG_OPTION);
    }
  }

  @Override
  public CliCommand setDescription(String... desc) {
    super.setDescription(desc);
    return this;
  }

  public CliCommand addExample(String... example) {
    StringBuilder stringBuilder = new StringBuilder();
    boolean codeBlockToggle = false;
    for (String s : example) {

      /**
       * Code block got automatically an HALF tab
       */
      if (codeBlockToggle && !s.equals(CliUsage.CODE_BLOCK)) {
        stringBuilder.append(CliUsage.TAB);
      }
      if (s.equals(CliUsage.CODE_BLOCK)) {
        if (!codeBlockToggle) {
          codeBlockToggle = true;
          stringBuilder.append(CliUsage.CODE_BLOCK);
          continue;
        } else {
          codeBlockToggle = false;
          continue;
        }
      }

      /**
       * Default
       */
      stringBuilder.append(s)
        .append(CliUsage.EOL);

    }
    this.examples.add(stringBuilder.toString());
    return this;
  }

  public CliCommand setFooter(String... footer) {
    this.footer = Strings.createMultiLineFromStrings(footer).toString();
    return this;
  }


  /**
   * Return a child (command|cli) from this (client|command)
   * creating a (hierarchy|chain) of command
   *
   * @param word - the new word name that is used to create the command
   * @return a {@link CliCommand}
   */
  public CliCommand addChildCommand(String word) {

    nameArgAndCommandCheck(word);
    CliWord cliWord = localWordsMap.get(word);

    CliCommand cliCommand = null;
    if (cliWord != null) {
      if (cliWord instanceof CliCommand)
        cliCommand = (CliCommand) cliWord;
    }
    /**
     * Case if the cliWord is a property word
     */
    if (cliCommand == null) {

      // Create the cli
      cliCommand = new CliCommand(this, word);
      cliCommand.setInsertionOrder(localWordsMap.size());
      localWordsMap.put(word, cliCommand);

    }

    return cliCommand;

  }


  /**
   * @return the arg words of the cliCommand sorted by insertion order (not alphabetically)
   */
  @SuppressWarnings("WeakerAccess")
  public List<CliWord> getArgs() {

    return localWordsMap
      .values()
      .stream()
      .sorted(Comparator.comparing(CliWord::getInsertionOrder))
      .filter(CliWord::isArg)
      .collect(Collectors.toCollection(ArrayList::new));

  }


  /**
   * @return a list of the children command of this command
   */
  public List<CliCommand> getChildCommands() {

    return localWordsMap.values()
      .stream()
      .filter(CliWord::isCommand)
      .map(CliCommand.class::cast)
      .sorted()
      .collect(Collectors.toList());

  }


  /**
   * From the actual cli to the parent
   * * the children are not included
   * * the root also as this will not be a word
   * <p>
   * If you want a full chain of command (ie with the root), see {@link #getChainOfCommand()}
   *
   * @return a list of all command name from this command to the cli (the root command)
   */
  public List<CliCommand> getParentsCommands() {

    List<CliCommand> cliCommands = new ArrayList<>();
    if (this.parentCliCommand != null) {
      cliCommands.add(this.parentCliCommand);
      cliCommands.addAll(this.parentCliCommand.getParentsCommands());
    }
    Collections.reverse(cliCommands);
    return cliCommands;

  }

  /**
   * @return Chain of command with the parents and the actual command
   */
  public List<CliCommand> getChainOfCommand() {

    List<CliCommand> cliCommands = getParentsCommands();
    cliCommands.add(this);
    return cliCommands;

  }


  /**
   * @return the properties of this command (local and of its parents)
   */
  public List<CliWord> getAllOptions() {

    List<CliWord> options = this.getDescendantOptions();
    ArrayList<CliWord> collectProperties = this.localWordsMap.values()
      .stream()
      .filter(CliWord::isOption)
      .collect(Collectors.toCollection(ArrayList::new));
    options.addAll(collectProperties);
    return options;

  }

  /**
   * @return the local options of this command (without the option of its parents)
   * A property is a flag or an option
   */
  public List<CliWord> getLocalOptions() {

    return localWordsMap.values().stream()
      .filter(CliWord::isOption)
      .sorted()
      .collect(Collectors.toCollection(ArrayList::new));

  }

  public List<CliWord> getLocalWords() {

    return new ArrayList<>(localWordsMap.values());

  }

  /**
   * @return the local flag of this command (without the option of its parents)
   */
  public List<CliWord> getLocalFlags() {

    return localWordsMap.values().stream()
      .filter(CliWord::isFlag)
      .collect(Collectors.toCollection(ArrayList::new));

  }

  /**
   * Create a global word definition in order to share the same definition
   * between different commands.
   * <p>
   * This function may be called normally only from the root command (the cli)
   * but it's also possible from a child
   * <p>
   * If you add a word to a command using the same name, this definition will be used.
   *
   * @param name the name of the created word globally
   * @return a word
   */
  public CliWord addWordToLibrary(String name) {

    CliWord word = getGlobalWord(name);
    if (word == null) {
      word = new CliWord(this, name);
      globalWordMap.put(name, word);
      globalWordList.add(word);
    }
    return word;

  }

  protected static void nameOptionCheck(String name) {
    if (!name.startsWith(CliParser.PREFIX_LONG_OPTION)) {
      throw new RuntimeException("A option should start with " + CliParser.PREFIX_LONG_OPTION);
    }
  }


  /**
   * Return the root command of the chain (the cli)
   * If the cli is the root, it returns itself
   *
   * @return the root command (ie the cli)
   */
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
   * @return a list of all words defined globally with the function {@link #addWordToLibrary(String)}
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
   * * {@link #addProperty(String)}
   * * {@link #addArg(String)}
   * * {@link #addChildCommand(String)}
   *
   * @param name the word name to create
   * @return a word, ie `--name` for an option or `name` for a command
   */
  CliWord getOrCreateWordOf(String name) {

    // The word may be a parent word
    // Not command word because you may find
    // the same command of several level (example: info)
    CliWord cliWord = getParentWords()
      .stream()
      .filter(w -> !w.isCommand())
      .collect(Collectors.toMap(CliWord::getName, identity()))
      .get(name);
    if (cliWord != null) {
      return cliWord;
    }

    // Short name Options
    List<String> shortNames = localWordsMap.values()
      .stream()
      .map(CliWord::getShortName)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
    Set<String> duplicates = Lists.findDuplicates(shortNames);
    if (duplicates.size() != 0) {
      for (String duplicate : duplicates) {
        List<String> duplicateCliWord = localWordsMap.values()
          .stream()
          .filter(x -> x.getShortName() != null)
          .filter(x -> x.getShortName().equals(duplicate))
          .map(CliWord::getName)
          .collect(Collectors.toList());
        throw new RuntimeException(
          "The short name (" + duplicate + ") is used twice. The following words declare it ("
            + String.join(",", duplicateCliWord)
            + ")");
      }

    }
    cliWord = localWordsMap.values()
      .stream()
      .filter(x -> x.getShortName() != null)
      .collect(Collectors.toMap(CliWord::getShortName, identity()))
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
      cliWord.setInsertionOrder(localWordsMap.size());
      localWordsMap.put(name, cliWord);

    }
    return cliWord;

  }


  /**
   * @return an example string
   */
  public List<String> getExamples() {
    return examples;
  }

  /**
   * @return the footer to print in a usage
   */
  public String getFooter() {
    return this.footer;
  }

  /**
   * @param word -  a word
   * @return true if the word is defined for this cliCommand
   */
  public boolean hasWord(String word) {
    return localWordsMap.containsKey(word);
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
    return localWordsMap.values().stream()
      .filter(CliWord::isMandatory)
      .collect(Collectors.toCollection(ArrayList::new));
  }

  /**
   * @return local and parent words
   */
  public List<CliWord> getAllWords() {
    List<CliWord> cliWords =
      localWordsMap
        .values()
        .stream()
        .sorted()
        .collect(Collectors.toList());
    if (parentCliCommand != null) {
      cliWords.addAll(parentCliCommand.getAllWords());
    } else {
      cliWords.add(this);
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
      cliWords.addAll(parentCliCommand.localWordsMap.values());
      cliWords.addAll(parentCliCommand.getParentWords());
    }
    return cliWords;
  }

  /**
   * @return the descendants options (options of the parent)
   */
  public List<CliWord> getDescendantOptions() {
    ArrayList<CliWord> cliWords = new ArrayList<>();
    if (getParentCommand() != null) {
      cliWords.addAll(getParentCommand().getLocalOptions());
      cliWords.addAll(getParentCommand().getDescendantOptions());
    }
    return cliWords;
  }


  public CliWord getHelpWord() {
    CliCommand root = getRootCommand();
    if (root.helpWord != null) {
      return root.helpWord;
    } else {
      return null;
    }
  }

  public CliCommand setHelpWord(String helpWord) {
    CliCommand root = getRootCommand();
    root.helpWord = addFlag(helpWord);
    return this;
  }

  public CliWord getVersionWord() {
    CliCommand root = getRootCommand();
    if (root.versionWord != null) {
      return root.versionWord;
    } else {
      return null;
    }
  }

  public CliCommand setVersionWord(String versionWord) {
    CliCommand root = getRootCommand();
    root.versionWord = addFlag(versionWord);
    return this;
  }


  /**
   * @param cliWord - a cliWord
   * @return - the cliCommand for this cliWord or null
   */
  public CliCommand getChildCommand(CliWord cliWord) {

    return (CliCommand) this.localWordsMap.get(cliWord.getName());

  }


  public CliWord addFlag(String name) {
    nameOptionCheck(name);
    return getOrCreateWordOf(name)
      .setTypeAsFlag();
  }


  public CliWordGroup getGroup(String name) {
    return CliWordGroup.get(this, name);
  }


  public CliCommand getParentCommand() {
    return this.parentCliCommand;
  }

  public boolean isModule() {
    return this.getChildCommands().size() > 0;
  }

  public boolean isRoot() {
    return parentCliCommand == null;
  }

  /**
   *
   * Return a  word by its name
   * @param name The word name
   * @return the word object
   */
  public CliWord getLocalWord(String name) {
    return localWordsMap.get(name);
  }

  /**
   * Parse the args given at the command
   * creation {@link #createRoot(String, String[])}
   *
   * @return the parser
   */
  public CliParser parse() {
    return parse(getArgsToParse());
  }

  private String[] getArgsToParse() {
    return getRootCommand().argsToParse;
  }

  /**
   * created to simplify the test. Generally the args are given at the
   * root command creation {@link #createRoot(String, String[])}
   *
   * @param args - the argument to parse
   * @return the parser
   */
  public CliParser parse(String[] args) {
    return CliParser
      .create(this, args)
      .parse();
  }

  /**
   *
   * @param map Add configuration (ie external properties)
   * @return the command
   */
  public CliCommand addConfigurations(Map<String, Object> map) {
    getRootCommand().conf.putAll(map);
    return this;
  }


  public String getConf(String configName) {
    Object o = getRootCommand().conf.get(configName);
    if (o != null) {
      return o.toString();
    } else {
      return null;
    }
  }


  public CliCommand addPropertyFromAttribute(Attribute attribute) {
    addProperty(CliParser.PREFIX_LONG_OPTION + Key.toLongOptionName(attribute.toString()))
      .setShortName(CliParser.PREFIX_SHORT_OPTION + Key.toShortOptionName(attribute.toString()))
      .setDescription(attribute.getDescription())
      .setDefaultValue(attribute.getDefaultValue().toString());
    return this;
  }
}
