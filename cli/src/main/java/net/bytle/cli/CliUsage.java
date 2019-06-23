package net.bytle.cli;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CliUsage {

    public final static String TAB = "\t";
    private static final Logger LOGGER = CliLog.getCliLog().getLogger();

    /**
     * Return the usage
     *
     * @return a text describing the usage of the {@link CliCommand}
     */
    public static String get(CliCommand cliCommand, int level) {


        StringBuilder usage = new StringBuilder("\n" + cliCommand.getDescription());

        usage.append("\n\nSyntax:");
        usage.append(" (" + level + ")");
        usage.append("\n\n");

        usage.append(TAB);

        // Properties are calculated here because we are not printing
        // a lot of things if we have no proeprties
        //
        // Properties are printed:
        //   * if they are Mandatory
        //   * if they have no groups
        //   * if they have a group level of 1
        //
        // Print the local options that are not set to be found in the config file
        List<CliWord> properties = cliCommand.getLocalProperties()
                .stream()
                .filter(x -> x.getLevel() <= level || x.isMandatory())
                .collect(Collectors.toList());

        // The chain of command
        // The command comes after the option
        // This then possible to differentiate the arg value of the option value
        // Example: below TableName is not the value of the cif option but the argument
        // db table download -tdf <path> -cif <TableName>
        final List<CliCommand> cliCommandChain = cliCommand.getFullParentsCommand();
        for (int i = 0; i < cliCommandChain.size(); i++) {
            CliCommand c = cliCommandChain.get(i);
            usage.append(c.getName());

            if (i < cliCommandChain.size() - 1) {
                usage.append(" ");
            }
        }

        // TODO - add the mandatory options ?
        if (properties.size() != 0) {
            usage.append(" [options|flags]");
        }


        if (cliCommand.getChildCommands().size() > 0) {
            usage.append(" <command>");
        }

        // Args
        for (CliWord arg : cliCommand.getArgs()) {
            usage.append(" <").append(arg.getName()).append(">");
        }

        Integer maxCharArgs = 0;
        for (CliWord word : cliCommand.getWords()) {
            int optionLength = getPrintedWord(word).length();
            if (optionLength > maxCharArgs) {
                maxCharArgs = optionLength;
            }
        }
        int tabPosition = maxCharArgs + 4;


        if (cliCommand.getArgs().size() > 0 || cliCommand.getChildCommands().size() > 0 || properties.size() > 0) {

            usage.append("\n\nwhere:\n");

            if (cliCommand.getChildCommands().size() > 0) {

                usage.append("\n\tCommand (is one of):");


                for (CliCommand childCliCommand : cliCommand.getChildCommands()) {
                    final int generation = tabPosition - childCliCommand.getName().length();
                    String tabForOption = new String(new char[generation]).replace("\0", " ");
                    usage.append("\n" + TAB + TAB + "* ").append(childCliCommand.getName()).append(tabForOption).append(childCliCommand.getDescription());
                }
                usage.append("\n");

            }


            if (properties.size() > 0) {

                List<CliWordGroup> groups =
                        properties.stream()
                                .flatMap(x -> x.getGroups().stream())
                                .distinct()
                                .collect(Collectors.toList());

                for (CliWordGroup cliWordGroup : groups) {

                    usage
                            .append("\n\t")
                            .append(cliWordGroup.getName())
                            .append(":")
                            .append(" (" + cliWordGroup.getLevel() + ")"); // TODO: add level if it's level fine
                    if (cliWordGroup.getDescription() != null) {
                        usage.append("\n\t")
                                .append(cliWordGroup.getDescription());
                    }
                    List<CliWord> words = properties.stream()
                            .filter(x -> x.getGroups().contains(cliWordGroup))
                            .collect(Collectors.toList());

                    printWordUsage(usage, tabPosition, words);

                    usage.append("\n");

                }

            }


            // Print the options that are not in the config file
//            if (level > 1) {
//                properties = cliCommand.getParentProperties();
//                if (properties.size() > 0) {
//
//                    usage.append("\n\tGlobal Options:");
//
//                    printWordUsage(usage, tabPosition, properties);
//
//                    usage.append("\n");
//
//                }
//            }
//
//            if (level >= 2) {
//                Path configFile = cliCommand.getGlobalConfigFile();
//                if (configFile != null) {
//                    usage.append(configFile);
//                } else {
//                    usage.append("Path unknown");
//                }
//                usage.append(" ):");
//            }


            // Print the args
            if (cliCommand.getArgs().size() > 0) {

                if (cliCommand.getChildCommands().size() > 0) {
                    LOGGER.warning("The cliCommand is a parent command, the argument can then not yet been known");
                }

                usage.append("\n\tArgs:");
                printWordUsage(usage, tabPosition, cliCommand.getArgs());
                usage.append("\n");

            }


        }

        String example = cliCommand.getExample();
        if (example != null) {
            usage.append("\n").append(example);
        }

        String footer = cliCommand.getFooter();
        if (footer != null) {
            usage.append("\n").append(footer);
        }

        return usage.toString();

    }

    /**
     * Print of a word
     *
     * @param word
     * @return
     */
    protected static String getPrintedWord(CliWord word) {
        // The formatting  of the option give the max character number
        // + 1 for the minus after the shortName
        // + 2 for the double minus after the word name
        StringBuilder stringBuilder = new StringBuilder();

        if (word.isArg() || word.isCommand()) {
            stringBuilder.append(word.getName());

        } else {
            if (word.getShortName() != null) {
                stringBuilder
                        .append(CliParser.PREFIX_SHORT_OPTION)
                        .append(word.getShortName())
                        .append(",");
            }

            stringBuilder
                    .append(CliParser.PREFIX_LONG_OPTION)
                    .append(word.getName());

            if (word.isOption()) {
                stringBuilder.append(" <")
                        .append(word.getValueName())
                        .append(">");
            }
        }

        // Todo - if log level is fine
        stringBuilder.append(" (" + word.getLevel() + " - " + word.isMandatory() + ")");
        return stringBuilder.toString();

    }

    /**
     * Add the options list to the usage
     * This is used for the local and parent options printing
     *
     * @param usage       - the text to print
     * @param tabPosition - the position of the text
     * @param words       - the options to print
     */
    private static void printWordUsage(StringBuilder usage, int tabPosition, List<CliWord> words) {
        for (CliWord word : words) {
            int length = getPrintedWord(word).length();
            final int generation = tabPosition - length;
            String tabForOption = new String(new char[generation]).replace("\0", " ");
            usage
                    .append("\n" + TAB + TAB + "* ")
                    .append(getPrintedWord(word))
                    .append(tabForOption)
                    .append(word.getDescription());
        }
    }


    @SuppressWarnings("WeakerAccess")
    public static void print(CliCommand cliCommand, int level) {
        System.out.println(get(cliCommand, level));
    }

    @SuppressWarnings("unused")
    public static void print(CliCommand cliCommand) {
        print(cliCommand, 1);
    }

    /**
     * An utility function that returns the full chain of command
     * (rootCommand + cliChain) with a space between them
     * <p>
     * Example: cli subcommand1 subcommand2
     * <p>
     * This is mostly used when building example
     *
     * @param cliCommand
     * @return the full chain of command
     */
    public static String getFullChainOfCommand(CliCommand cliCommand) {

        StringBuilder chainOfCommand = new StringBuilder();

        final List<CliCommand> cliCommandChain = cliCommand.getFullParentsCommand();

        for (int i = 0; i < cliCommandChain.size(); i++) {
            CliCommand c = cliCommandChain.get(i);
            chainOfCommand.append(c.getName());
            chainOfCommand.append(" ");
        }

        return chainOfCommand.toString();

    }
}
