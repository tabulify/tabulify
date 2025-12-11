package com.tabulify.cli;

import com.tabulify.exception.CastException;
import com.tabulify.exception.InternalException;
import com.tabulify.type.Lists;
import com.tabulify.type.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CliUsage {

  /**
   * Character constant
   */
  public final static String TAB = "    ";
  private static final String HALF_TAB = "  ";
  public static final String EOL = Strings.EOL;
  private static final String BLANK_LINE = EOL + EOL;
  private final static String BEFORE_HEAD_SEPARATOR = BLANK_LINE + BLANK_LINE;
  private static final String AFTER_HEADING_SEPARATOR = BLANK_LINE;
  private static final String H1_CHARACTER = "=";
  private static final String H2_CHARACTER = "-";
  public static final String CODE_BLOCK = EOL + EOL;

  /**
   * Log
   */
  private static final Logger LOGGER = CliLog.LOGGER;

  /**
   * Return the usage
   *
   * @return a text describing the usage of the {@link CliCommand}
   * <p>
   * Tip: No spacing at the end of a paragraph/section.
   * A section that opens should create space
   */
  public static String get(CliCommand cliCommand) {

    StringBuilder usage = new StringBuilder();

    /*
      Head
     */
    printHeading(usage, getFullChainOfCommand(cliCommand), 1);


    /*
      Description
     */
    usage.append(cliCommand.getDescription());

    /*
      Examples
     */
    List<String> examples = cliCommand.getExamples();
    if (examples.size() >= 1) {
      String exampleTitle = "Example";
      if (examples.size() > 1) {
        exampleTitle += "s";
      }
      printHeading(usage, exampleTitle, 2);
      int counter = 0;
      for (String example : examples) {
        counter++;
        usage
          .append(" ")
          .append(counter)
          .append(" - ")
          .append(example);
        if (counter < examples.size()) {
          usage
            .append(CliUsage.BLANK_LINE);
        }
      }
    }

    /*
      Syntax
     */
    printHeading(usage, "Syntax", 2);

    /*
      The chain of command

      The command comes after the option
      This then possible to differentiate the arg value of the option value
      Example: below TableName is not the value of the cif option but the argument
          db table download -tdf <path> -cif <TableName>
     */

    final List<CliCommand> cliCommandChain = cliCommand.getChainOfCommand();
    usage
      .append(EOL)
      .append(TAB);

    for (int i = 0; i < cliCommandChain.size(); i++) {

      usage.append(
        cliCommandChain
          .get(i)
          .getName()
      );

      if (i < cliCommandChain.size() - 1) {
        usage.append(" ");
      }
    }


    /*
      If this is a module,
        * print the command,
        * otherwise print the argument

      ie a command is an arg for a module.

      Note: the `if` statement is not really needed but gives
      the logic
     */
    if (cliCommand.isModule()) {

      usage.append(" <command>");
      CliWord helpWord = cliCommand.getHelpWord();
      if (helpWord != null) {
        usage
          .append(" [")
          .append(getPrintWord(helpWord))
          .append("]");
      }

    } else {

      if (cliCommand.getLocalOptions().size() != 0) {

        usage.append(" [options|flags]");

      }
      for (CliWord arg : cliCommand.getArgs()) {
        usage
          .append(" ")
          .append(getPrintWord(arg));
      }
    }


    /*
      Where if Options/Flag/Sub-Command
     */
    if (
      cliCommand.getArgs().size() > 0
        || cliCommand.getChildCommands().size() > 0
        || cliCommand.getLocalOptions().size() > 0
    ) {

      /*
        Open spacing
       */
      usage
        .append(BLANK_LINE)
        .append(EOL)
        .append("where:")
        .append(EOL);


      /*
        Print the commands or the args

        ie a command is an arg for a module.
       */
      if (cliCommand.isModule()) {

        List<CliWord> moduleWords;
        try {
          moduleWords = Lists.castToNewList(cliCommand.getChildCommands(), CliWord.class);
        } catch (CastException e) {
          throw new InternalException(e);
        }

        /*
          Words can be set on module level
         */
        List<CliWord> moduleOptionsWords = new ArrayList<>();
        CliWord helpWord = cliCommand.getHelpWord();
        if (helpWord != null) {
          moduleWords.add(helpWord);
          moduleOptionsWords.add(helpWord);
        }
        if (cliCommand.isRoot()) {
          CliWord versionWord = cliCommand.getVersionWord();
          if (versionWord != null) {
            moduleWords.add(versionWord);
            moduleOptionsWords.add(versionWord);
          }
        }
        printOptionHeading(usage, "Commands:");
        int tabPosition = getTabPosition(cliCommand, moduleWords);
        printWords(usage, tabPosition, cliCommand.getChildCommands());

        if (moduleOptionsWords.size() > 0) {
          printOptionHeading(usage, "Option:");
          printWords(usage, tabPosition, moduleOptionsWords);
        }

      } else {

        int tabPosition = getTabPosition(cliCommand, cliCommand.getAllWords());

        /*
          Print the args
         */
        if (cliCommand.getArgs().size() > 0) {

          if (cliCommand.getChildCommands().size() > 0) {
            LOGGER.warning("This command is a module command, the following arguments should not be defined (" + cliCommand.getArgs().stream().map(CliWord::getName).collect(Collectors.joining(", ")) + ")");
          }

          printOptionHeading(usage, "Arguments:");
          printWords(usage, tabPosition, cliCommand.getArgs());

        }

        /*
          Options (ie flag or property)

          Options are calculated here because we are not printing
          a lot of things if we have no properties

          Options are printed:
             * if they are Mandatory
             * if they have no groups
             * if they have a group level of 1
          Print the local options that are not set to be found in the config file
         */

        List<CliWord> localOptions = cliCommand.getLocalOptions();

        if (localOptions.size() > 0) {

          List<CliWordGroup> groups =
            localOptions.stream()
              .flatMap(x -> x.getGroups().stream())
              .distinct()
              .collect(Collectors.toList());

          for (CliWordGroup cliWordGroup : groups) {

            printOptionHeading(usage, cliWordGroup.getName() + ":");

            List<CliWord> words = localOptions.stream()
              .filter(x -> x.getGroups().contains(cliWordGroup))
              .collect(Collectors.toList());

            printWords(usage, tabPosition, words);

          }

        }


        if (!cliCommand.isRoot()) {
          /*
            If module, print the help option
            otherwise
            Print the options of the higher level
           */
          printOptionHeading(usage, "Global Options:");

          if (cliCommand.getChildCommands().size() > 0) {

            CliWord helpWord = cliCommand.getHelpWord();
            if (helpWord != null) {
              printWords(usage, tabPosition, Collections.singletonList(helpWord));
            }

          } else {

            localOptions = cliCommand.getDescendantOptions();
            if (localOptions.size() > 0) {

              printWords(usage, tabPosition, localOptions);

            }
          }
        }


      }

    }

    /*
      Print the footer if any
     */
    String footer = cliCommand.getFooter();
    if (footer != null) {
      printHeading(usage, "Footer", 2);
      usage.append(EOL)
        .append(footer);
    }

    return usage.toString();

  }

  /**
   *
   *
   * @param cliCommand the cli command
   * @param cliWords the cli words
   * @return the position where the second column (the description should be)
   */
  private static int getTabPosition(CliCommand cliCommand, List<? extends CliWord> cliWords) {
    /*
      The width of the column
      that contains the properties
     */
    int propertiesColumnWidth = 0;
    for (CliWord word : cliWords) {
      int propertyLength = getPrintWord(word).length();
      if (propertyLength > propertiesColumnWidth) {
        propertiesColumnWidth = propertyLength;
      }
    }
    return propertiesColumnWidth + 4;
  }

  private static void printOptionHeading(StringBuilder usage, String title) {
    usage
      .append(BLANK_LINE)
      .append(HALF_TAB)
      .append(title)
      .append(EOL);
  }

  private static void printHeading(StringBuilder stringBuilder, String title, int level) {

    /*
      Heading underline character
     */
    String hCharacter = H2_CHARACTER;
    if (level == 1) {
      hCharacter = H1_CHARACTER;
    }
    /*
      Spacing before
     */
    if (level != 1) {
      stringBuilder
        .append(BEFORE_HEAD_SEPARATOR);
    } else {
      stringBuilder.append(BLANK_LINE);
    }
    /*
      processing
     */
    stringBuilder
      .append(Strings.createFromString(title).toFirstLetterCapitalCase().toString())
      .append(EOL)
      .append(Strings.createFromString(hCharacter).multiply(title.length()).toString())
      .append(AFTER_HEADING_SEPARATOR);
  }

  /**
   *
   * <p>
   * Example:
   * `--option value`
   * `--option,-o value`
   * `--flag`
   *
   * @param word the word
   * @return The print of a word (command,args,flags)
   */
  public static String getPrintWord(CliWord word) {
    // The formatting  of the option give the max character number
    // + 1 for the minus after the shortName
    // + 2 for the double minus after the word name
    StringBuilder stringBuilder = new StringBuilder();

    if (word.isCommand()) {

      stringBuilder.append(word.getName());

    } else if (word.isArg()) {
      stringBuilder
        .append("<")
        .append(word.getName())
        .append(">");
    } else if (word.isOption()) {

      if (word.getShortName() != null) {
        stringBuilder
          .append(word.getShortName())
          .append(",");
      }

      stringBuilder
        .append(word.getName());

      if (word.hasValue()) {
        stringBuilder.append(" <")
          .append(word.getValueName())
          .append(">");
      }

    } else {

      throw new IllegalArgumentException("The word type (" + word.getType() + ") of the word (" + word + ") should be taken into account");

    }

    return stringBuilder.toString();

  }


  /**
   * Print option (flag, property), command in a list format
   *
   * @param stringBuilder - the text to build
   * @param tabPosition   - the position of the description
   * @param words         - the words to print
   */
  private static void printWords(StringBuilder stringBuilder, int tabPosition, List<? extends CliWord> words) {
    for (CliWord word : new ArrayList<>(words)) {
      String printWord = getPrintWord(word);
      int length = printWord.length();
      final int generation = tabPosition - length;
      String tabForOption = new String(new char[generation]).replace("\0", " ");
      stringBuilder
        .append(EOL)
        .append(HALF_TAB)
        .append(HALF_TAB)
        .append(printWord)
        .append(tabForOption)
        .append(word.getDescription())
        .append(EOL);
    }
  }

  public static void print(CliCommand cliCommand) {
    System.out.println(get(cliCommand));
  }


  /**
   * An utility function that returns the full chain of command
   * (rootCommand + cliChain) with a space between them
   * <p>
   * Example: cli subcommand1 subcommand2
   * <p>
   * This is mostly used when building example
   *
   * @param cliCommand the cli command
   * @return the full chain of command
   */
  public static String getFullChainOfCommand(CliCommand cliCommand) {

    StringBuilder chainOfCommand = new StringBuilder();

    final List<CliCommand> cliCommandChain = cliCommand.getChainOfCommand();

    for (CliCommand c : cliCommandChain) {
      chainOfCommand.append(c.getName());
      chainOfCommand.append(" ");
    }

    return chainOfCommand.toString().trim();

  }
}
