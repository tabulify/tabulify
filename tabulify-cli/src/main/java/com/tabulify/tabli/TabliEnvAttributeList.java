package com.tabulify.tabli;

import com.tabulify.Tabular;
import com.tabulify.TabularAttributeEnum;
import com.tabulify.conf.Origin;
import com.tabulify.model.RelationDef;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.exception.CastException;
import net.bytle.exception.IllegalArgumentExceptions;
import net.bytle.regexp.Glob;
import net.bytle.type.Casts;
import net.bytle.type.KeyCase;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.Strings;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TabliEnvAttributeList {


  public static final String DEFAULT_SELECTOR = "*";

  public static List<DataPath> run(Tabular tabular, CliCommand childCommand) {

    String tip = "To have a nice output because a value may be very lengthy, you should set the width of your terminal to a big number (such as 10000)";

    // Command
    childCommand
      .setDescription(
        "List the configurations",
        CliUsage.EOL,
        "Tip:" + tip
      )
      .addExample(
        "List all `tabli` configurations",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " *",
        CliUsage.CODE_BLOCK
      )
      .addExample(
        "List all Home configurations",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " " + " *home*",
        CliUsage.CODE_BLOCK
      )
      .addExample(
        "List all attributes set by OS Environment variables",
        CliUsage.CODE_BLOCK,
        CliUsage.getFullChainOfCommand(childCommand) + " " + TabliWords.TYPE_PROPERTY + " " + Origin.OS,
        CliUsage.CODE_BLOCK
      );


    List<String> origins = Arrays.stream(Origin.values())
      .map(Origin::toString)
      .map(tabular::toPublicName)
      .sorted().collect(Collectors.toList());

    childCommand.addProperty(TabliWords.TYPE_PROPERTY)
      .setDescription("The type of the configurations to return ('" + String.join(", ", origins) + "' or `all`)")
      .addDefaultValue(Origin.ALL)
    ;

    childCommand.addArg(TabliWords.NAME_SELECTORS)
      .setDescription("One or more glob selector(s) that will filter the output by the key name")
      .setDefaultValue(DEFAULT_SELECTOR);


    // Arguments
    final CliParser cliParser = childCommand.parse();

    List<Glob> nameSelectors = cliParser.getStrings(TabliWords.NAME_SELECTORS)
      .stream()
      .map(Glob::createOf)
      .collect(Collectors.toList());


    /**
     * If no type is given but a name selector is
     * we set the type to all to filter on all type of variables
     */
    List<Origin> listOrigins;
    if (
      !nameSelectors.isEmpty()
        && !nameSelectors.get(0).getPattern().equals(DEFAULT_SELECTOR)
        && !cliParser.has(TabliWords.TYPE_PROPERTY)
    ) {
      // no origin given and a name selector given
      listOrigins = Collections.singletonList(Origin.ALL);
      TabliLog.LOGGER_TABLI.info("The `type` of attribute was to `all` because a selector was given (without any type option)");
    } else {
      listOrigins = cliParser.getStrings(TabliWords.TYPE_PROPERTY)
        .stream()
        .map(s -> {
          try {
            return Casts.cast(s, Origin.class);
          } catch (CastException e) {
            throw IllegalArgumentExceptions.createForArgumentValue(s, TabliWords.TYPE_PROPERTY, Origin.class, e);
          }
        })
        .collect(Collectors.toList());
    }

    RelationDef feedbackDataDef = tabular.getMemoryDataStore()
      .getDataPath("configuration-list")
      .getOrCreateRelationDef()
      .addColumn("attribute")
      .addColumn("value")
      .addColumn("origin")
      .addColumn("description");

    /**
     * We put them in env case so that the user
     * got the fact that it can set them as os env variable
     */
    KeyCase attributeCase = KeyCase.SNAKE_UPPER;
    try (InsertStream insertStream = feedbackDataDef.getDataPath().getInsertStream()) {
      tabular
        .getAttributes()
        .stream()
        .filter(e -> {
          if (listOrigins.contains(Origin.ALL)) {
            return true;
          }
          Origin origin = e.getOrigin();
          return listOrigins.contains(origin);
        })
        .filter(e -> Glob.matchOneOfGlobs(e.getAttributeMetadata().toString().toLowerCase(), nameSelectors, Pattern.CASE_INSENSITIVE))
        .sorted()
        .forEach(e ->
          {
            /**
             * Map Skip
             * (ie {@link TabularAttributeEnum.NATIVE_DRIVER}
             */
            if (e.getAttributeMetadata().equals(TabularAttributeEnum.NATIVE_DRIVER)) {
              return;
            }

            String capitalizedOrigin = Strings.createFromString(e.getOrigin().getDescription())
              .toFirstLetterCapitalCase()
              .toString();
            Object originalValue = e.getValueOrDefaultOrNull();


            insertStream.insert(
              KeyNormalizer.createSafe(e.getAttributeMetadata()).toCaseSafe(attributeCase),
              valuetoString(originalValue),
              capitalizedOrigin,
              e.getAttributeMetadata().getDescription()
            );
          }
        );

    }

    TabliLog.LOGGER_TABLI.tip(tip);
    return Collections.singletonList(feedbackDataDef.getDataPath());
  }

  private static String valuetoString(Object originalValue) {

    if (originalValue == null) {
      return "";
    }
    boolean isCollection = Collection.class.isAssignableFrom(originalValue.getClass());
    if (isCollection) {
      return String.join(", ", Casts.castToCollection(originalValue, String.class));
    }
    return originalValue.toString();

  }
}
