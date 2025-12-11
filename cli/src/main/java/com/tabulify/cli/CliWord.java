package com.tabulify.cli;

import com.tabulify.type.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.tabulify.cli.CliWordType.*;

public class CliWord implements Comparable<CliWord> {

  public static final String DEFAULT_PRINTED_VALUE = "value";
  private final CliCommand command;

  /**
   * The type of word - only 3 possibilities
   */
  private CliWordType type = ARGUMENT;

  /**
   * The separator of the name for the key value
   * <p>
   * Example:
   * The `attributes` property in the `tabul data list` command
   * has a default key value of `tabul.data.list.attributes`
   */
  public static final String PROPERTY_NAME_SEPARATOR = ".";


  /**
   * The name of the word
   * It must be unique
   */
  private final String name;

  /**
   * The description of the word
   */
  private String description;


  /**
   * The environment name of this word
   */
  private String envName;

  /**
   * The system property name of this word
   * ie java -Dname=value ......
   */
  private String systemPropertyName;

  /**
   * Is this word mandatory ?
   */
  private boolean mandatory = false;

  /**
   * The name of the property in the property file
   * if any
   */
  private String configFileKey = null;

  /**
   * The name of the value (Used in the syntax)
   */
  private String valueName = DEFAULT_PRINTED_VALUE;

  /**
   * The short name of this word.
   * Apply normally only to an option
   */
  private String shortName;

  /**
   * The default
   */
  private List<String> defaultValues = new ArrayList<>();

  /**
   * The groups in which the variable will appears
   */
  private final List<CliWordGroup> groups = new ArrayList<>();
  /**
   * The insertion order
   */
  private Integer insertionOrder;


  /**
   * To create a word, you need to use one of:
   * * {@link CliCommand#addChildCommand(String)}
   * * {@link CliCommand#addProperty(String)}
   * * {@link CliCommand#addArg(String)}
   *
   * @param wordName - the unique name of the word
   */
  CliWord(CliCommand cliCommand, String wordName) {
    this.command = cliCommand;
    this.name = wordName;
  }

  /**
   * @return the unique name of the word
   * on the scope (ie not with the command chain)
   */
  public String getName() {
    return name;
  }


  /**
   * @return if this word is expecting a value
   */
  public boolean hasValue() {
    if (this.type == PROPERTY) {
      return true;
    } else {
      CliLog.LOGGER.fine("The word (" + getName() + ") is not an option. It then cannot ");
      return false;
    }
  }

  /**
   * Is this a flag word
   *
   * @return true if it's a flag
   */
  public boolean isFlag() {
    return this.type == FLAG;
  }

  /**
   * Is this a property word
   *
   * @return true if it's a flag or option
   */
  public boolean isOption() {
    return this.type == FLAG || this.type == PROPERTY;
  }

  /**
   * @return the description of this word
   */
  public String getDescription() {
    if (description == null) {
      return "No Description available";
    } else {
      return description;
    }
  }

  /**
   * Set the description of this word
   *
   * @param desc - the description
   * @return - the word for a chaining initialization
   */
  public CliWord setDescription(String... desc) {
    this.description = Strings.createMultiLineFromStrings(desc).toString();
    return this;
  }

  /**
   * Define a word as an option
   *
   * @return - the word for chaining initialization
   */
  public CliWord setTypeAsProperty() {
    CliCommand.nameOptionCheck(getName());
    this.type = PROPERTY;
    return this;
  }


  /**
   * @return the string representation of a word
   * in its scope (ie chain of command)
   */
  @Override
  public String toString() {

    return getRelativeId();

  }

  /**
   * @return true if the word defines an argument
   * You can define that this word is a command with the function {@link #setTypeAsArg()}
   */
  boolean isArg() {
    return type == ARGUMENT;
  }

  /**
   * Define that this word is an argument
   *
   * @return - a word for chaining initialization
   */
  @SuppressWarnings("WeakerAccess")
  public CliWord setTypeAsArg() {
    CliCommand.nameArgAndCommandCheck(getName());
    this.type = ARGUMENT;
    return this;
  }

  /**
   * @return true if this word defines a command
   * You can define that this word is a command with the function {@link #setTypeAsCommand()}
   */
  boolean isCommand() {
    return type == COMMAND;
  }

  /**
   * Define that this word is a command
   *
   * @return - the word for enabling chaining initialization
   */
  CliWord setTypeAsCommand() {

    CliCommand.nameArgAndCommandCheck(getName());

    this.type = COMMAND;
    return this;
  }

  /**
   * Set the name of the environment variable for this word
   *
   * @param name the name of the environment variable
   * @return this word for construction chaining
   */
  @SuppressWarnings("WeakerAccess")
  public CliWord setEnvName(String name) {
    this.envName = name;
    return this;
  }

  @SuppressWarnings("WeakerAccess")
  public String getEnvName() {
    return this.envName;
  }

  /**
   * Set this word has mandatory
   * If the word is not found, the parser will stop
   * with an error message.
   *
   * @return the word
   */
  @SuppressWarnings("WeakerAccess")
  public CliWord setMandatory(Boolean b) {
    this.mandatory = b;
    return this;
  }

  /**
   * @return true if the word is mandatory
   */
  public boolean isMandatory() {
    return mandatory;
  }

  /**
   * Option expected in the config file ?
   *
   * @return - the cliWord for chaining construction
   */
  public CliWord setConfigFileKey(String key) {
    this.configFileKey = key;
    return this;
  }

  /**
   * @param valueName - an example of value seen in the usage
   * @return - this word for instance chaining
   */
  public CliWord setValueName(String valueName) {
    this.valueName = valueName;
    return this;
  }

  /**
   * @return the name of the value of this option
   * used in the syntax namely
   */
  public String getValueName() {
    if (isOption()) {
      return valueName;
    } else {
      return "";
    }
  }


  /**
   * @param shortName the short name
   * @return the word
   */
  public CliWord setShortName(String shortName) {
    // For a command, the alias does not have any prefix
    if (this.isOption()) {
      if (!shortName.startsWith(CliParser.PREFIX_SHORT_OPTION)) {
        throw new RuntimeException("Short option names should start with a minus");
      }
    }
    this.shortName = shortName;
    return this;
  }

  /**
   * @return the short name for an option
   * or an alias for a command
   */
  public String getShortName() {
    return this.shortName;
  }

  public CliWord addDefaultValue(Object defaultValue) {

    this.defaultValues.add(String.valueOf(defaultValue));
    return this;

  }

  /**
   * An alias of {@link #addDefaultValue(Object)} but reset all value to one
   *
   * @param defaultValue the default value
   * @return cliWord for chaining
   */
  public CliWord setDefaultValue(Object defaultValue) {

    if (defaultValue == null) {
      return this;
    }
    this.defaultValues = new ArrayList<>();
    this.defaultValues.add(String.valueOf(defaultValue));
    return this;

  }

  public List<String> getDefaultValues() {
    return this.defaultValues;
  }

  public CliWord setSystemPropertyName(String systemPropertyName) {
    this.systemPropertyName = systemPropertyName;
    return this;
  }

  public String getSystemPropertyName() {
    return this.systemPropertyName;
  }

  public CliWord setTypeAsFlag() {
    CliCommand.nameOptionCheck(getName());
    this.type = FLAG;
    return this;

  }

  public CliWord addGroup(String name) {

    return addGroup(CliWordGroup.get(this.command, name));
  }

  private CliWord addGroup(CliWordGroup group) {
    if (!groups.contains(group)) {
      groups.add(group);
    }
    return this;
  }

  public CliWord setGroup(String name) {
    return addGroup(name);
  }

  public CliWord setGroup(CliWordGroup group) {
    return addGroup(group);
  }

  public List<CliWordGroup> getGroups() {
    if (groups.size() > 0) {
      return groups;
    } else {
      return Collections.singletonList(CliWordGroup.get(this.command, "Options"));
    }
  }

  public Integer getImportantLevel() {
    if (getGroups().size() >= 1) {
      return getGroups().stream()
        .mapToInt(CliWordGroup::getImportanceLevel)
        .max()
        .orElse(0);
    } else {
      return 1;
    }
  }

  public String getDefaultValue() {
    List<String> defaultValues = getDefaultValues();
    if (defaultValues.size() > 0) {
      return defaultValues.get(0);
    } else {
      return null;
    }

  }

  public String getConfigName() {
    return configFileKey;
  }

  public CliWord setInsertionOrder(int insertionOrder) {
    this.insertionOrder = insertionOrder;
    return this;
  }

  /**
   * By default word, options are shown
   * alphabetically
   *
   * @param o the object to compare to
   * @return tue if the same
   */
  @Override
  public int compareTo(CliWord o) {
    return this.name.compareTo((o).name);
  }

  /**
   * @return the command where this word is attached
   * It can be null when the word is the {@link CliCommand#createRoot(String, String[]) root command}
   */
  public CliCommand getCliCommand() {
    return this.command;
  }

  public CliWordType getType() {
    return type;
  }

  /**
   * * A command has a variable name (ie the command name)
   * * An option has a variable name (ie short or long option)
   * * But an argument has no name. It has a position
   * <p>
   * Ie except the args all other options have a name
   *
   * @return true if there is a name
   */
  public boolean hasVariableName() {
    return !isArg();
  }

  public String getId() {
    /**
     * The root word (ie the root command)
     * have no command
     */
    if (getCliCommand() == null) {
      /**
       * This is the root command
       */
      return getName();
    } else {
      return getCliCommand().getId() + PROPERTY_NAME_SEPARATOR + getName();
    }
  }

  /**
   * @return The {@link #getId Id} without the cli root name (Used as key in conf file)
   */
  public String getRelativeId() {
    String id = getId();
    return id.substring(id.indexOf(PROPERTY_NAME_SEPARATOR) + 1);
  }

  public Integer getInsertionOrder() {
    return insertionOrder;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CliWord cliWord = (CliWord) o;
    return command.equals(cliWord.command) &&
      name.equals(cliWord.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(command, name);
  }
}
