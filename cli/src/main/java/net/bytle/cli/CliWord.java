package net.bytle.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class CliWord {

    public static final String DEFAULT_PRINTED_VALUE = "value";
    private final CliCommand command;
    /**
     * The type of word - only 3 possibilities
     */
    private int type = TYPE_OPTION;
    private static final int TYPE_OPTION = 1;
    private static final int TYPE_COMMAND = 2;
    private static final int TYPE_ARG = 3;
    private static final int TYPE_FLAG = 4;


    /**
     * The name of the word
     * It must be unique
     */
    private String name;

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
     */
    private Boolean isInConfigFile = false;

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
    private List<CliWordGroup> groups = new ArrayList<>();


    /**
     * To create a word, you need to use one of:
     * * {@link CliCommand#commandOf(String)}
     * * {@link CliCommand#optionOf(String)}
     * * {@link CliCommand#argOf(String)}
     *
     * @param wordName - the unique name of the word
     */
    CliWord(CliCommand cliCommand, String wordName) {
        this.command = cliCommand;
        this.name = wordName;
    }

    /**
     * @return the unique name of the word
     */
    @SuppressWarnings("WeakerAccess")
    public String getName() {
        return name;
    }


    /**
     * @return if this word is expecting a value (normally can be true only for a word option
     */
    @SuppressWarnings("WeakerAccess")
    public boolean hasValue() {
        if (isArg() || isCommand() || isFlag()) {
            CliLog.LOGGER.fine("The word (" + getName() + ") is not an option. It then cannot ");
            return false;
        } else {
            return true;
        }
    }

    /**
     * Is this a flag word
     *
     * @return true if it's a flag
     */
    public boolean isFlag() {
        return this.type == TYPE_FLAG;
    }

    /**
     * Is this a property word
     *
     * @return true if it's a flag or option
     */
    public boolean isProperty() {
        return this.type == TYPE_FLAG || this.type == TYPE_OPTION;
    }

    /**
     * @return the description of this word
     */
    @SuppressWarnings("WeakerAccess")
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
    @SuppressWarnings("WeakerAccess")
    public CliWord setDescription(String desc) {
        this.description = desc;
        return this;
    }

    /**
     * Define a word as an option
     *
     * @return - the word for chaining initialization
     */
    @SuppressWarnings("WeakerAccess")
    public CliWord setTypeAsOption() {
        this.type = TYPE_OPTION;
        return this;
    }

    /**
     * @return true if the word is an option
     * You can define that this word is a command with the function {@link #setTypeAsOption()}
     */
    @SuppressWarnings("WeakerAccess")
    public Boolean isOption() {
        return type == TYPE_OPTION;
    }

    /**
     * @return the string representation of a word
     */
    @Override
    public String toString() {
        return name;
    }


    /**
     * @return true if the word defines an argument
     * You can define that this word is a command with the function {@link #setTypeAsArg()}
     */
    boolean isArg() {
        return type == TYPE_ARG;
    }

    /**
     * Define that this word is an argument
     *
     * @return - a word for chaining initialization
     */
    @SuppressWarnings("WeakerAccess")
    public CliWord setTypeAsArg() {
        this.type = TYPE_ARG;
        return this;
    }

    /**
     * @return true if this word defines a command
     * You can define that this word is a command with the function {@link #setTypeAsCommand()}
     */
    boolean isCommand() {
        return type == TYPE_COMMAND;
    }

    /**
     * Define that this word is a command
     *
     * @return - the word for enabling chaining initialization
     */
    CliWord setTypeAsCommand() {
        this.type = TYPE_COMMAND;
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
     * @return
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
     * @return the type name of the word
     */
    public String getTypeName() {
        if (isOption()) {
            return "option";
        } else if (isArg()) {
            return "argument";
        } else if (isCommand()) {
            return "command";
        }
        return "unknown";
    }


    /**
     * Option expected in the config file ?
     *
     * @return - the cliWord for chaining construction
     */
    public CliWord setIsInConfigFile(Boolean b) {
        this.isInConfigFile = b;
        return this;
    }

    /**
     * @param valueName - the name of the value that is used in the usage
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
     * @return if the word option is expected to be in the config file
     * Used in the usage
     */
    public boolean isInConfigFile() {
        return this.isInConfigFile;
    }

    public CliWord setShortName(String shortName) {
        this.shortName = shortName;
        return this;
    }

    /**
     * @return the short name
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
     * @param defaultValue
     * @return cliWord for chaining
     */
    public CliWord setDefaultValue(Object defaultValue) {

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
        this.type = TYPE_FLAG;
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
            return Arrays.asList(CliWordGroup.get(this.command, "Local"));
        }
    }

    public Integer getLevel() {
        if (getGroups().size() >= 1) {
            Optional<Integer> level = getGroups().stream()
                    .map(x -> x.getLevel())
                    .max(Integer::compareTo);
            return level.get();
        } else {
            return 1;
        }
    }

    public String getDefaultValue() {
        List<String> defaultValues = getDefaultValues();
        if (defaultValues.size()>0){
            return defaultValues.get(0);
        } else {
            return null;
        }

    }
}
