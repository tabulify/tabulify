package net.bytle.cli;

import net.bytle.exception.CastException;
import net.bytle.log.Log;
import net.bytle.type.Attribute;
import net.bytle.type.Casts;
import net.bytle.type.MapKeyIndependent;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


/**
 *
 */
public class CliParser {

  private static final Log LOGGER = CliLog.LOGGER;

  public static final String PREFIX_LONG_OPTION = "--";
  public static final String PREFIX_SHORT_OPTION = "-";


  private final String[] args;
  private final CliCommand cliCommand;


  // Verbosity
  private CliWord verboseWord;

  /**
   * Contains the founded words
   * that are recognized and there value if any
   * <p>
   * When null, parse was not done
   */
  private final Map<CliWord, List<String>> foundWordsInArgs = new HashMap<>();

  /**
   * The cli word by there name
   * <p>
   * This structure is used :
   * * to lookup the cliWord by name in the get function
   * * to help with the parse
   */
  private final Map<String, CliWord> knownWords = new MapKeyIndependent<>();

  // A logger initialized in the {@link CliCommand#getLogger} function
  private final Log logger = CliLog.LOGGER;


  /**
   * use the other constructor {@link #CliParser(CliCommand, String[])}
   */
  @SuppressWarnings("unused")
  private CliParser() {
    args = new String[0];
    cliCommand = null;
  }

  CliParser(CliCommand cliCommand, String[] args) {
    assert cliCommand != null;
    this.cliCommand = cliCommand;
    this.args = args;

    /**
     * Build the map of known words with a name
     */
    for (CliWord cliWord : this.cliCommand.getAllWords()) {
      if (cliWord.getShortName() != null) {
        this.knownWords.put(cliWord.getShortName(), cliWord);
      }
      this.knownWords.put(cliWord.getName(), cliWord);
    }


  }

  public static CliParser create(CliCommand cliCommand, String[] argsToParse) {
    return new CliParser(cliCommand, argsToParse);
  }

  @Override
  public String toString() {
    return "CliParser for the command " + cliCommand;
  }

  /**
   * Lazy initialization
   * This function is called when a get function is called
   */
  public CliParser parse() {

    LOGGER.fine("Parsing the command (" + this.cliCommand.getName() + ")");

    /*
      Parsing
     */
    parseArgsArray();

    /*
      Checks
     */
    // Help
    // Help check must be before mandatory check
    // Otherwise the mandatory check will stop the process
    checkHelpOption();

    // Mandatory check
    checkMandatoryField();

    // Verbose
    checkLogVerbosity();

    return this;

  }


  /**
   * Check the mandatory fields and exit if any is missing
   */
  private void checkMandatoryField() {
    for (CliWord word : cliCommand.getMandatoryWords()) {
      if (!foundWordsInArgs.containsKey(word)) {
        String msg = "The " + word.getType() + " " + word.getName() + " is mandatory and was not found";
        logger.severe(msg);
        throw new IllegalArgumentException(msg);
      }
    }
  }


  /**
   * Parse the args into a list of word with or without value
   */
  private void parseArgsArray() {

    /*
     * Local variable
     * above the loop
     */
    // Number of argument found
    int argumentCountFound = 0;

    CliLog.LOGGER.fine("Known Words");
    for(Map.Entry<String,CliWord> knownWord: knownWords.entrySet()){
      CliLog.LOGGER.fine("   - Word ("+knownWord.getKey()+")");
    }

    boolean unknownOption = false;
    for (int i = 0; i < args.length; i++) {

      String s = args[i];

      CliLog.LOGGER.fine("Processing ("+s+")");

      CliWord namedCliWord = getNamedKnownWord(s);
      if (namedCliWord == null) {
        LOGGER.fine("No word found for (" + s + ")");
      } else {
        LOGGER.fine("Word found: " + namedCliWord);
      }


      /*
       * After that an unknown option has been seen,
       * a word can't be a command
       */
      if (unknownOption && namedCliWord != null && namedCliWord.isCommand()) {
        namedCliWord = null;
      }

      if (namedCliWord != null) {

        if (namedCliWord.hasValue()) {

          i++;
          List<String> strings = foundWordsInArgs.computeIfAbsent(namedCliWord, k -> new ArrayList<>());
          if (i < args.length) {
            strings.add(args[i]);
          } else {
            if (!this.cliCommand.isModule()) {
              /*
               * In a module parsing, the value of a unknown property
               * may be a name
               * Because it can be the case, we don't throw an error for module parsing
               */
              throw new IllegalArgumentException("The " + namedCliWord.getType() + " (" + namedCliWord + ") expects one or more value(s) and no values were given.");
            }
          }

        } else {

          foundWordsInArgs.put(namedCliWord, null);

        }

      } else {


        /*
         * This name is not known as a named word
         * This is then an argument or unknown word
         *
         * If this a module parse, there is no args, but we continue to parse the options
         * Generally the global options at the command root (ie verbosity)
         * that are available for all commands
         */
        if (!this.cliCommand.isModule()) {

          /*
           * If this is an option, we should have found it before
           */
          if (s.startsWith(CliParser.PREFIX_SHORT_OPTION)) {
            throw new IllegalArgumentException("The option (" + s + ") is not an known option for the command (" + cliCommand + ").");
          }
          /*
           * This is an end command and this should be an argument
           */
          if (!cliCommand.getArgs().isEmpty()) {

            // If we don't have another argument to store the value, we add it to the last one
            if (argumentCountFound + 1 > cliCommand.getArgs().size()) {

              final CliWord lastArg = cliCommand.getArgs().get(cliCommand.getArgs().size() - 1);
              List<String> lastArgValues = foundWordsInArgs.get(lastArg);
              lastArgValues.add(s);
              final String msg = "There is only " + cliCommand.getArgs().size() + " argument(s) defined. The value (" + s + ") is the " + (argumentCountFound + 1) + " arguments. It was added to the argument " + lastArg.getName();
              logger.fine(msg);

            } else {

              final List<String> values = new ArrayList<>();
              values.add(s);
              foundWordsInArgs.put(cliCommand.getArgs().get(argumentCountFound), values);
              argumentCountFound++;

            }

          } else {

            throw new IllegalArgumentException("The word (" + s + ") is not an option for the command (" + cliCommand + "). If it's a command, they should be written in first position.");

          }

        } else {

          unknownOption = true;

        }

      }


    }

  }

  private String removeOptionPrefix(String s) {
    if (s.startsWith(PREFIX_LONG_OPTION)) {
      s = s.substring(PREFIX_LONG_OPTION.length());
    } else if (s.startsWith(PREFIX_SHORT_OPTION)) {
      s = s.substring(PREFIX_SHORT_OPTION.length());
    }
    return s;
  }


  private CliWord getNamedKnownWord(String s) {
    CliWord cliWord = knownWords.get(s);
    CliWord namedCliWord = null;
    if (cliWord != null && cliWord.hasVariableName()) {
      /*
       * Not all cliWord have names
       * (ie edge case when a value is the same as the name
       * of an argument)
       */
      namedCliWord = cliWord;
    }
    return namedCliWord;
  }


  /**
   * The log verbosity
   */
  private void checkLogVerbosity() {

    if (verboseWord != null) {
      if (getBoolean(verboseWord)) {

        CliLog.LOGGER.makeLoggerVerbose();

      }
    }

  }


  /**
   * Return: true if a word is present
   *
   * @param cliWord the word
   * @return the value of the word
   */
  public Boolean getBoolean(CliWord cliWord) {

    return getObject(cliWord, Boolean.class);

  }

  private <T> T getObject(CliWord cliWord, Class<T> aClass) {

    if (cliWord == null) {

      return null;

    } else {

      /**
       * Value determination
       * Value in args ?
       */
      List<String> value = foundWordsInArgs.get(cliWord);

      /**
       * Not found
       * In System Property
       */
      if (value == null) {
        String systemPropertyName = cliWord.getSystemPropertyName();
        if (systemPropertyName == null) {
          /**
           * Retrieve by name with scope
           * Value is for a property, we delete from the name the long option `--`
           * Word can have minus option in their name ie (--log-level)
           */
          systemPropertyName = cliWord.getId().replace(CliParser.PREFIX_LONG_OPTION, "");
        }
        String systemPropertyValue = System.getProperty(systemPropertyName);
        if (systemPropertyValue == null) {
          /**
           * Retrieve by name
           */
          systemPropertyValue = System.getProperty(cliWord.getName());
        }
        if (systemPropertyValue != null) {
          value = Collections.singletonList(systemPropertyValue);
        }
      }

      /**
       * Not found in the args
       * In env ?
       */
      if (value == null) {
        String envName = cliWord.getEnvName();
        if (cliWord.getEnvName() == null) {
          envName = cliWord.getId();
        }
        String env = System.getenv().get(envName);
        if (env != null) {
          value = Collections.singletonList(env);
        }
      }

      /**
       * Not found
       * In Conf map ?
       */
      if (value == null) {
        String configName = cliWord.getConfigName();
        if (configName == null) {
          /**
           * Get the id with the scope
           * (ie the command is attached)
           */
          configName = cliWord.getId();
        }
        String configPropertyValue = cliCommand.getConf(configName);
        if (configPropertyValue == null) {
          /**
           * Retrieve by name without the scope (command)
           */
          configPropertyValue = cliCommand.getConf(cliWord.getName());
        }
        if (configPropertyValue != null) {
          value = Collections.singletonList(configPropertyValue);
        }
      }

      /**
       * Default value
       */
      if (value == null) {

        String defaultValue = cliWord.getDefaultValue();

        /**
         * Flag have always false as default
         */
        if (defaultValue == null && cliWord.isFlag()) {
          defaultValue = "false";
        }
        if (defaultValue != null) {
          value = Collections.singletonList(defaultValue);
        }
      }

      if (value != null) {

        /**
         * Flags has always a value.
         * Ie default set or otherwise false
         *
         * If flag is present, opposite of the value
         */
        if (cliWord.isFlag()) {

          if (aClass != Boolean.class) {
            throw new RuntimeException("The word (" + cliWord + ") is a flag and you can ask only a boolean value, not a" + aClass.getSimpleName());
          }
          Boolean booleanValue = cast(value.get(0), Boolean.class, cliWord);

          /**
           *  If flag is present, opposite of the value
           */
          boolean found = foundWordsInArgs.containsKey(cliWord);
          if (found) {
            booleanValue = !booleanValue;
          }
          logger.info("(" + cliWord + ") word was found with the value: " + booleanValue);
          return aClass.cast(booleanValue);

        } else {
          T result;
          if (value.size() == 1) {
            result = cast(value.get(0), aClass, cliWord);
          } else {
            String join = String.join(" ", value);
            result = cast(join, aClass, cliWord);
          }

          logger.info("(" + cliWord + ") word was found with the value (" + result + ")");
          return result;
        }

      } else {

        /**
         * No value
         */


        String defaultValue = cliWord.getDefaultValue();
        if (defaultValue != null) {
          T castedDefaultValue = cast(defaultValue, aClass, cliWord);
          logger.info("(" + cliWord + ") word was not found. Default value returned (" + castedDefaultValue + ")");
          return castedDefaultValue;
        } else {

          if (aClass == Boolean.class) {
            logger.info("(" + cliWord + ") word was not found. False returned.");
            return aClass.cast(false);
          } else {
            logger.info("(" + cliWord + ") word was not found. Null returned.");
            return null;
          }
        }

      }

    }

  }


  private <T> T cast(String value, Class<T> aClass, CliWord cliWord) {
    try {
      return Casts.cast(value, aClass);
    } catch (CastException e) {
      /**
       * We throw an illegal argument exception
       * to tell that the input was not good
       */
      throw new IllegalArgumentException("The word " + cliWord + " with the value (" + value + ") is not a " + aClass.getSimpleName());
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
  public Boolean getBoolean(String word) {

    CliWord cliWord = knownWords.get(word);

    return getBoolean(cliWord);

  }


  /**
   * Return a number. Null if the option is not present
   *
   * @param word a word
   * @return the value of the word as double
   */
  @SuppressWarnings("WeakerAccess")
  public Double getDouble(CliWord word) {

    return getObject(word, Double.class);

  }

  /**
   * Return a integer. Null if the option is not present
   *
   * @param word -  the word name
   * @return the integer value of the name
   */
  public Integer getInteger(CliWord word) {

    return getObject(word, Integer.class);

  }


  /**
   * Return a string value from an option
   *
   * @param word - a word
   * @return the word value as a string or null
   */
  public String getString(CliWord word) {

    return getObject(word, String.class);

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
      List<String> strings = foundWordsInArgs.get(word);
      if (strings == null) {
        if (word.getDefaultValues().size() > 0) {
          strings = word.getDefaultValues();
          logger.info("(" + word + ") word was found with the default values: " + String.join(",", strings));
        } else {
          strings = new ArrayList<>();
          logger.info("(" + word + ") word was not found");
        }
      } else {
        logger.info("(" + word + ") word was found with the values: " + String.join(",", strings));
      }
      return strings;
    }

  }


  /**
   * @param word - a word
   * @return the value of the word as a path
   */
  public Path getPath(CliWord word) {

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
   * @param word - a word string
   * @return the value of the word as a path
   */
  public Path getPath(String word) {

    CliWord cliWord = knownWords.get(word);
    return getPath(cliWord);

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
    return foundWordsInArgs.size();
  }


  /**
   * @return a list of the child commands founds
   */
  public List<CliCommand> getFoundedChildCommands() {

    assert cliCommand != null;
    return foundWordsInArgs.keySet().stream()
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
  public CliCommand getCommand() {

    List<CliCommand> cliCommands = getFoundedChildCommands();
    if (cliCommands.size() > 0) {
      return cliCommands.get(0);
    } else {
      return null;
    }

  }

  /**
   * @return the found arguments
   */
  public List<CliWord> getArgs() {

    return foundWordsInArgs.keySet().stream()
      .filter(CliWord::isArg)
      .collect(Collectors.toCollection(ArrayList::new));

  }

  /**
   * @return the found options
   */
  public List<CliWord> getOptions() {

    return foundWordsInArgs.keySet().stream()
      .filter(CliWord::isOption)
      .collect(Collectors.toCollection(ArrayList::new));

  }

  /**
   * @param word - the word
   * @return - the value of the word as string. If the word as several values, return them concatenated with a space
   */
  public String getString(String word) {

    CliWord cliWord = knownWords.get(word);
    return getString(cliWord);

  }

  public String getString(Attribute keyIndependent) {

    CliWord cliWord = knownWords.get(keyIndependent.toString());
    return getString(cliWord);

  }

  /**
   * @param word - the word
   * @return - the value of the word as integer. If the word has several values, return the first one.
   */
  @SuppressWarnings("WeakerAccess")
  public Integer getInteger(String word) {

    CliWord cliWord = knownWords.get(word);
    return getInteger(cliWord);


  }

  /**
   * @param word - the word
   * @return - the value of the word as double. If the word has several values, return the first one
   */
  @SuppressWarnings("WeakerAccess")
  public Double getDouble(String word) {

    CliWord cliWord = knownWords.get(word);

    return getDouble(cliWord);


  }

  /**
   * @param word - the word
   * @return - the values of the word as a list of string
   */
  @SuppressWarnings("WeakerAccess")
  public List<String> getStrings(String word) {

    CliWord cliWord = knownWords.get(word);
    return getStrings(cliWord);

  }

  public List<String> getStrings(Attribute attribute) {


    return getStrings(attribute.toString());

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
      throw new RuntimeException(msg);
    } else {
      logger.warning(msg);
      return true;
    }
  }

  /**
   * @param word - the word
   * @return - the command if found or null
   */
  @SuppressWarnings("WeakerAccess")
  public CliCommand getCommand(String word) {

    CliWord cliWord = knownWords.get(word);
    return getCommand(cliWord);


  }

  private CliCommand getCommand(CliWord cliWord) {
    if (cliWord == null) {
      return null;
    } else {
      if (foundWordsInArgs.containsKey(cliWord)) {
        if (cliWord instanceof CliCommand) {
          return (CliCommand) cliWord;
        } else {
          throw new IllegalStateException("The word (" + cliWord + ") is not a command but a " + cliWord.getType() + "");
        }
      } else {
        return null;
      }
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

    CliWord helpWord = cliCommand.getHelpWord();
    if (helpWord != null) {
      /**
       * The check is done only
       * if there is no child command
       */
      if (getFoundedChildCommands().size() == 0) {
        if (this.getBoolean(helpWord)) {
          CliUsage.print(cliCommand);
          throw new HelpPrintedException();
        }
      }
    }

  }

  public Map<String, String> getProperties(String word) {
    List<String[]> arrays = getStrings(word)
      .stream()
      .map(s -> s.split("=", 2))
      .collect(Collectors.toList());
    Map<String, String> properties = new HashMap<>();
    for (String[] array : arrays) {
      String key = array[0];
      String value = null;
      if (array.length > 1) {
        value = array[1];
      }
      properties.put(key, value);
    }
    return properties;
  }

  public boolean has(String word) {
    CliWord cliWord = knownWords.get(word);
    return foundWordsInArgs.containsKey(cliWord);
  }

}
