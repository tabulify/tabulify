package com.tabulify.tabul;

import com.tabulify.Tabular;
import com.tabulify.TabularAttributeEnum;
import com.tabulify.flow.engine.PipelineStepBuilderTargetArgument;
import com.tabulify.flow.operation.SelectPipelineStepArgument;
import com.tabulify.flow.operation.TransferPipelineStepArgument;
import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.type.KeyNormalizer;

/**
 * Define the words of the command line
 * and create the library
 */
public class TabulWords {


  public static final String XPATH = "xpath";

  public static final String CLI_NAME = Tabular.TABUL_NAME;


  public static final String NATIVE_ATTRIBUTE = "--native-attribute";
  public static final String ATTRIBUTE_OPTION = "--attribute";
  public static final String SOURCE_ATTRIBUTE = "--source-attribute";
  public static final String TARGET_ATTRIBUTE = "--target-attribute";
  public static final String LOG_LEVEL_LONG_OPTION = "--" + TabularAttributeEnum.LOG_LEVEL.getKeyNormalized().toCliLongOptionName();

  /**
   * Conf options
   */
  public static final String TYPE_PROPERTY = "--type";
  // print to the console without headers and don't make the control characters visible
  public static final String PIPE_MODE = "--pipe-mode";

  public static final String KEY = "key";
  public static final String VALUE = "value";

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
  public static final String START_COMMAND = "start";
  public static final String STOP_COMMAND = "stop";
  public static final String SERVICE_COMMAND = "service";
  public static final String TIMEOUT_PROPERTY = "--timeout";
  public static final String STRICT_SELECTION = "--strict-selection";
  public static final String NO_STRICT_SELECTION = "--no-strict-selection";
  public static final String NO_RESULTS = "--no-results";
  public static final String INIT_COMMAND = "init";


  protected static final String PASSPHRASE_PROPERTY = "--" + TabularAttributeEnum.PASSPHRASE.getKeyNormalized().toCliLongOptionName();
  protected final static String WITH_DEPENDENCIES_PROPERTY = "--" + SelectPipelineStepArgument.WITH_DEPENDENCIES.getKeyNormalized().toCliLongOptionName();
  protected final static String VIRTUAL_COLUMN_PROPERTY = "--virtual-column";
  protected static final String LIMIT_PROPERTY = "--limit";

  // Config file words
  static final String CONF_PATH_PROPERTY = "--" + TabularAttributeEnum.CONF.getKeyNormalized().toCliLongOptionName();

  static final String EXECUTION_ENVIRONMENT = "--" + TabularAttributeEnum.EXEC_ENV.getKeyNormalized().toCliLongOptionName();

  // Module
  static final String APP_COMMAND = "app";
  static final String CONNECTION_COMMAND = "connection";
  public static final String DATA_COMMAND = "data";
  static final String DEPENDENCY_COMMAND = "dependency";

  // SubCommands level 2 / Action


  // Between two remote system ?
  static final String TRANSFER_COMMAND = "transfer";

  static final String DIFF_COMMAND = "diff";
  static final String FILL_COMMAND = "fill";

  static final String EXECUTE_COMMAND = "execute";
  static final String SUMMARIZE_COMMAND = "summarize";

  // Delete command
  static final String DROP_COMMAND = "drop";
  static final String TRUNCATE_COMMAND = "truncate";

  // Initialize DML
  static final String CREATE_COMMAND = "create";
  static final String CONCAT_COMMAND = "concat";
  static final String ADD_COMMAND = "add";
  public static final String REPLACE_COMMAND = "replace";


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
   * Attribute
   */
  static final String ENV_COMMAND = "env";

  /**
   * The command that print the data structure metadata
   */
  public static final String DESCRIBE_COMMAND = "describe";

  // Table UI -
  static final String LIST_COMMAND = "list";
  static final String UNZIP_COMMAND = "unzip";

  static final String SET_COMMAND = "set";


  static final String HELP_FLAG = "--help";
  public static final String VERSION_FLAG = "--version";

  // Options
  public static final String NOT_STRICT_EXECUTION_FLAG = "--not-strict";
  public static final String STRICT_EXECUTION_FLAG = "--strict";

  public static final String MAX_RECORD_COUNT = "--max-record-count";

  /**
   * Tabul only
   */
  static final String OUTPUT_OPERATION_OPTION = "--output-operation";
  static final String OUTPUT_TRANSFER_OPERATION_OPTION = "--output-transfer-operation";

  // Transfer Operation
  static final String TRANSFER_OPERATION_OPTION = CliParser.PREFIX_LONG_OPTION + KeyNormalizer.createSafe(TransferPipelineStepArgument.TRANSFER_OPERATION).toCliLongOptionName();
  static final String TARGET_OPERATION_OPTION = CliParser.PREFIX_LONG_OPTION + KeyNormalizer.createSafe(TransferPipelineStepArgument.TARGET_OPERATION).toCliLongOptionName();
  static final String SOURCE_OPERATION_OPTION = CliParser.PREFIX_LONG_OPTION + KeyNormalizer.createSafe(TransferPipelineStepArgument.SOURCE_OPERATION).toCliLongOptionName();


  // Cross DataStore Transfer options
  static final String TARGET_WORKER_OPTION = CliParser.PREFIX_LONG_OPTION + KeyNormalizer.createSafe(TransferPipelineStepArgument.TARGET_WORKER_COUNT).toCliLongOptionName();
  static final String BUFFER_SIZE_OPTION = CliParser.PREFIX_LONG_OPTION + KeyNormalizer.createSafe(TransferPipelineStepArgument.BUFFER_SIZE).toCliLongOptionName();

  static final String WITH_PARAMETERS = CliParser.PREFIX_LONG_OPTION + KeyNormalizer.createSafe(TransferPipelineStepArgument.WITH_PARAMETERS).toCliLongOptionName();
  static final String TARGET_COMMIT_FREQUENCY_OPTION = CliParser.PREFIX_LONG_OPTION + KeyNormalizer.createSafe(TransferPipelineStepArgument.TARGET_COMMIT_FREQUENCY).toCliLongOptionName();
  static final String TARGET_BATCH_SIZE_OPTION = CliParser.PREFIX_LONG_OPTION + KeyNormalizer.createSafe(TransferPipelineStepArgument.TARGET_BATCH_SIZE).toCliLongOptionName();
  static final String METRICS_DATA_URI_OPTION = CliParser.PREFIX_LONG_OPTION + KeyNormalizer.createSafe(TransferPipelineStepArgument.METRICS_DATA_URI).toCliLongOptionName();
  static final String SOURCE_FETCH_SIZE_OPTION = CliParser.PREFIX_LONG_OPTION + KeyNormalizer.createSafe(TransferPipelineStepArgument.SOURCE_FETCH_SIZE).toCliLongOptionName();
  static final String TRANSFER_MAPPING_STRICT = CliParser.PREFIX_LONG_OPTION + KeyNormalizer.createSafe(TransferPipelineStepArgument.TRANSFER_MAPPING_STRICT).toCliLongOptionName();


  static final String OUTPUT_DATA_URI = "--output-data-uri";
  // Options used in all sub actions
  static final String APP_HOME = CliParser.PREFIX_LONG_OPTION + KeyNormalizer.createSafe(TabularAttributeEnum.APP_HOME).toCliLongOptionName();
  public static final String SELECTION_OPTIONS_GROUP = "Selection Options";
  static final String TYPE_COMMAND = "type";


  /**
   * Argument
   */
  public static String TARGET_DATA_URI = KeyNormalizer.createSafe(PipelineStepBuilderTargetArgument.TARGET_DATA_URI).toCliLongOptionName();
  /**
   * One source selector value
   */
  public static String SOURCE_SELECTOR = "source-selector";
  /**
   * Multiple source selector value
   */
  public static String SOURCE_SELECTORS = "source-selector...";
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
      .setGroup(SELECTION_OPTIONS_GROUP)
      .setShortName("-wd")
      .setDefaultValue(false);

    cliCommand.addWordToLibrary(DATA_SELECTORS)
      .setTypeAsArg()
      .setDescription("One or more data or script selectors")
      .setMandatory(true);

    String data_definition = "Data Definition Options";
    cliCommand.addWordToLibrary(TabulWords.ATTRIBUTE_OPTION)
      .setTypeAsProperty()
      .setGroup(data_definition)
      .setShortName("-a")
      .setDescription("Set or add a data resource attribute");

    cliCommand.addWordToLibrary(TabulWords.SOURCE_ATTRIBUTE)
      .setTypeAsProperty()
      .setGroup(data_definition)
      .setShortName("-sa")
      .setValueName("attributeName=value")
      .setDescription("Set a source attribute");

    cliCommand.addWordToLibrary(TabulWords.TARGET_ATTRIBUTE)
      .setTypeAsProperty()
      .setGroup(data_definition)
      .setShortName("-ta")
      .setValueName("attributeName=value")
      .setDescription("Set a target attribute");

    cliCommand.addWordToLibrary(STRICT_SELECTION)
      .setTypeAsFlag()
      .setDefaultValue(false)
      .setGroup(SELECTION_OPTIONS_GROUP)
      .setDescription("If set the selection will return an error if no data resources have been selected");

    cliCommand.addWordToLibrary(NO_STRICT_SELECTION)
      .setTypeAsFlag()
      .setDefaultValue(true)
      .setGroup(SELECTION_OPTIONS_GROUP)
      .setDescription("If set an empty selection will not return an error if no data resources have been selected");

    cliCommand.addWordToLibrary(TabulWords.NOT_STRICT_EXECUTION_FLAG)
      .setTypeAsFlag()
      .setDescription("A minor error will not stop the process.")
      .setDefaultValue(false)
      .setShortName("-ns");

    cliCommand.addWordToLibrary(TabulWords.VIRTUAL_COLUMN_PROPERTY)
      .setTypeAsProperty()
      .setDescription("Add a virtual column with the value of a data resource attribute")
      .setValueName("columnName=resourceAttributeName")
      .setShortName("-vc")
    ;

    cliCommand.addWordToLibrary(TabulWords.TYPE_PROPERTY)
      .setTypeAsProperty()
      .setDescription("The type of the resource")
      .setValueName("mediaType|mimeType|extensionFile")
      .setShortName("-t")
    ;


  }


}
