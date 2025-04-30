package com.tabulify.tabli;

import com.tabulify.flow.step.EnrichStep;
import com.tabulify.flow.step.SelectSupplierArgument;
import com.tabulify.flow.step.TargetArguments;
import com.tabulify.flow.step.TransferArgumentProperty;
import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.type.Key;
import net.bytle.type.KeyNormalizer;

/**
 * Define the words of the command line
 * and create the library
 */
public class TabliWords {


  public static final String XPATH = "xpath";

  public static final String CLI_NAME = "tabli";


  public static final String ATTRIBUTE_PROPERTY = "--attribute";
  public static final String SOURCE_ATTRIBUTE = "--source-attribute";
  public static final String TARGET_ATTRIBUTE_PROPERTY = "--target-attribute";
  public static final String LOG_LEVEL_NAME = "log-level";
  public static final String LOG_LEVEL_LONG_OPTION = "--" + LOG_LEVEL_NAME;

  /**
   * Conf options
   */
  public static final String TYPE_PROPERTY = "--type";

  public static final String KEY = "key";
  public static final String VALUE = "value";
  public static final String QUERY_COMMAND = "query";
  public static final String HEAD_COMMAND = "head";
  public static final String TAIL_COMMAND = "tail";

  /**
   * Transfer operation
   */
  public static final String COPY_COMMAND = "copy";
  public static final String INSERT_COMMAND = "insert";
  public static final String MOVE_COMMAND = "move";
  public static final String UPDATE_COMMAND = "update";
  public static final String UPSERT_COMMAND = "upsert";
  public static final String DELETE_COMMAND = "delete";
  public static final String VAULT_COMMAND = "vault";
  public static final String ENCRYPT_COMMAND = "encrypt";
  public static final String DECRYPT_COMMAND = "decrypt";
  public static final String FLOW_COMMAND = "flow";
  public static final String PIPELINE_SELECTORS = "pipeline-selectors";
  public static final String TEMPLATE_COMMAND = "template";
  public static final String PING_COMMAND = "ping";
  public static final String DIAGNOSTIC_COMMAND = "diagnostic";


  protected static final String PASSPHRASE_PROPERTY = "--passphrase";
  protected final static String WITH_DEPENDENCIES_PROPERTY = "--" + Key.toLongOptionName(SelectSupplierArgument.WITH_DEPENDENCIES);
  protected final static String VIRTUAL_COLUMN_PROPERTY = "--" + Key.toLongOptionName(EnrichStep.EnrichStepArgument.VIRTUAL_COLUMN);
  protected static final String LIMIT_PROPERTY = "--limit";

  // Config file words
  static final String CONF_VARIABLES_PATH_PROPERTY = "--vars";

  static final String ENVIRONMENT = "--env";

  // Module
  static final String CONNECTION_COMMAND = "connection";
  static final String DATA_COMMAND = "data";
  static final String DEPENDENCY_COMMAND = "dependency";

  // SubCommands level 2 / Action


  // Between two remote system ?
  static final String TRANSFER_COMMAND = "transfer";

  static final String COMPARE_COMMAND = "compare";
  static final String FILL_COMMAND = "fill";

  static final String EXECUTE_COMMAND = "execute";
  static final String SUMMARY = "summary";

  // Delete command
  static final String DROP_COMMAND = "drop";
  static final String TRUNCATE_COMMAND = "truncate";

  // Initialize DML
  static final String CREATE_COMMAND = "create";
  static final String ADD_COMMAND = "add";


  // Scalar UI - Show/Info
  /**
   * The command that shows the data
   * Other idea (display)
   */
  static final String PRINT_COMMAND = "print";

  /**
   * Show metadata in a form format
   * (List shows them in a list format)
   */
  static final String INFO_COMMAND = "info";

  /**
   * Environment Variables
   */
  static final String VARIABLE_COMMAND = "variable";

  /**
   * The command that print the data structure metadata
   */
  static final String STRUCTURE_COMMAND = "structure";

  // Table UI -
  static final String LIST_COMMAND = "list";

  static final String SET_COMMAND = "set";


  static final String HELP_FLAG = "--help";
  public static final String VERSION_FLAG = "--version";

  // Options
  public static final String NOT_STRICT_FLAG = "--not-strict";
  public static final String IS_STRICT_FLAG = "--is-strict";
  public static final String FORCE_FLAG = "--force";
  public static final String MAX_RECORD_COUNT = "--max-record-count";

  /**
   * Tabli only
   */
  static final String OUTPUT_OPERATION_OPTION = "--output-operation";
  static final String OUTPUT_TRANSFER_OPERATION_OPTION = "--output-transfer-operation";

  // Transfer Operation
  static final String TRANSFER_OPERATION_OPTION = CliParser.PREFIX_LONG_OPTION + Key.toLongOptionName(TransferArgumentProperty.TRANSFER_OPERATION);
  static final String TARGET_OPERATION_OPTION = CliParser.PREFIX_LONG_OPTION + Key.toLongOptionName(TransferArgumentProperty.TARGET_OPERATION);
  static final String SOURCE_OPERATION_OPTION = CliParser.PREFIX_LONG_OPTION + Key.toLongOptionName(TransferArgumentProperty.SOURCE_OPERATION);


  // Cross DataStore Transfer options
  static final String TARGET_WORKER_OPTION = CliParser.PREFIX_LONG_OPTION + Key.toLongOptionName(TransferArgumentProperty.TARGET_WORKER_COUNT);
  static final String BUFFER_SIZE_OPTION = CliParser.PREFIX_LONG_OPTION + Key.toLongOptionName(TransferArgumentProperty.BUFFER_SIZE);

  static final String WITH_BIND_VARIABLES = CliParser.PREFIX_LONG_OPTION + Key.toLongOptionName(TransferArgumentProperty.WITH_BIND_VARIABLES);
  static final String TARGET_COMMIT_FREQUENCY_OPTION = CliParser.PREFIX_LONG_OPTION + Key.toLongOptionName(TransferArgumentProperty.TARGET_COMMIT_FREQUENCY);
  static final String TARGET_BATCH_SIZE_OPTION = CliParser.PREFIX_LONG_OPTION + Key.toLongOptionName(TransferArgumentProperty.TARGET_BATCH_SIZE);
  static final String METRICS_DATA_URI_OPTION = CliParser.PREFIX_LONG_OPTION + Key.toLongOptionName(TransferArgumentProperty.METRICS_DATA_URI);
  static final String SOURCE_FETCH_SIZE_OPTION = CliParser.PREFIX_LONG_OPTION + Key.toLongOptionName(TransferArgumentProperty.SOURCE_FETCH_SIZE);


  static final String OUTPUT_DATA_URI = "--output-data-uri";
  // Options used in all sub actions
  static final String PROJECT_HOME = "--project-home";


  /**
   * Argument
   */
  public static String TARGET_DATA_URI = KeyNormalizer.create(TargetArguments.TARGET_DATA_URI).toCliLongOptionName();
  public static String SOURCE_SELECTOR = "source-selector";
  public static String DATA_SELECTORS = "data-selector...";
  public static String NAME_SELECTORS = "name-selector...";
  /**
   * A selector to select the generation file
   */
  public static final String GENERATOR_SELECTOR = "--generator-selector";
  public static final String TARGET_SELECTOR = "target-selector...";

  // First created for the xml cli
  public static final String EXTRACT = "extract";
  public static final String CHECK = "check";
  public static final String UPDATE = "update";
  public static final String GET = "of";

  /**
   * Init the library of options
   *
   * @param cliCommand - the command
   */
  static void initLibrary(CliCommand cliCommand) {


    cliCommand.addWordToLibrary(WITH_DEPENDENCIES_PROPERTY)
      .setTypeAsFlag()
      .setDescription("If set, the dependencies will be also selected")
      .setGroup("Selection Options")
      .setShortName("-wd")
      .setDefaultValue(false);

    cliCommand.addWordToLibrary(DATA_SELECTORS)
      .setTypeAsArg()
      .setDescription("One or more data or script selectors")
      .setMandatory(true);

    String data_definition = "Data Definition Options";
    cliCommand.addWordToLibrary(TabliWords.ATTRIBUTE_PROPERTY)
      .setTypeAsProperty()
      .setGroup(data_definition)
      .setShortName("-a")
      .setDescription("Set or add a data resource attribute");

    cliCommand.addWordToLibrary(TabliWords.SOURCE_ATTRIBUTE)
      .setTypeAsProperty()
      .setGroup(data_definition)
      .setShortName("-sa")
      .setValueName("attributeName=value")
      .setDescription("Set a source data resource attribute");

    cliCommand.addWordToLibrary(TabliWords.TARGET_ATTRIBUTE_PROPERTY)
      .setTypeAsProperty()
      .setGroup(data_definition)
      .setShortName("-ta")
      .setValueName("attributeName=value")
      .setDescription("Set a target data resource attribute");

    cliCommand.addWordToLibrary(TabliWords.NOT_STRICT_FLAG)
      .setTypeAsFlag()
      .setDescription("A minor error will not stop the process.")
      .setDefaultValue(false)
      .setShortName("-ns");

    cliCommand.addWordToLibrary(TabliWords.VIRTUAL_COLUMN_PROPERTY)
      .setTypeAsProperty()
      .setDescription("Add a virtual column with the value of a data resource attribute")
      .setValueName("columnName=resourceAttributeName")
      .setShortName("-vc")
    ;

    cliCommand.addWordToLibrary(TabliWords.TYPE_PROPERTY)
      .setTypeAsProperty()
      .setDescription("The type of the resource")
      .setValueName("mediaType|mimeType|extensionFile")
      .setShortName("-t")
    ;


  }


}
