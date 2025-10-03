package com.tabulify.tabul;

import com.tabulify.Tabular;
import com.tabulify.flow.engine.Pipeline;
import com.tabulify.flow.engine.PipelineBuilder;
import com.tabulify.flow.engine.PipelineStepBuilderTargetArgument;
import com.tabulify.flow.operation.*;
import com.tabulify.fs.FsConnection;
import com.tabulify.gen.GenDataPathAttribute;
import com.tabulify.gen.GeneratorMediaType;
import com.tabulify.gen.flow.enrich.EnrichPipelineStep;
import com.tabulify.gen.flow.fill.FillPipelineStep;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.Tabulars;
import com.tabulify.stream.PrinterPrintFormat;
import com.tabulify.transfer.TransferOperation;
import com.tabulify.transfer.TransferPropertiesCross;
import com.tabulify.transfer.TransferPropertiesSystem;
import com.tabulify.transfer.TransferResourceOperations;
import com.tabulify.uri.DataUriNode;
import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliWord;
import net.bytle.exception.*;
import net.bytle.type.Casts;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

import java.util.*;
import java.util.stream.Collectors;

import static com.tabulify.flow.operation.TransferPipelineStepArgument.*;
import static com.tabulify.tabul.TabulWords.*;

/**
 * A class that encapsulate all transfer alias command
 * It's a helper class
 */
public class TabulDataTransferManager {


  public static final TransferOperation DEFAULT_FILL_TRANSFER_OPERATION = TransferOperation.UPSERT;
  private final Config builder;
  private final CliParser cliParser;
  private List<DataUriNode> dataSelectors;

  public TabulDataTransferManager(Config builder) {
    this.builder = builder;

    switch (builder.commandType) {
      case FILL:
        addFillTransferOptions();
        break;
      case DEFAULT:
      case CONCAT:
        addAllTransferOptions();
        break;
      default:
        throw new MissingSwitchBranch("command type", builder.commandType);
    }

    /**
     * Move (drop source)
     * Replace (drop target)
     */
    if (builder.sourceOperation != null) {
      builder.childCommand.getLocalWord(toLongOption(SOURCE_OPERATION.toString()))
        .setDefaultValue(builder.sourceOperation.toString());
    }
    if (builder.targetOperation != null) {
      builder.childCommand.getLocalWord(toLongOption(TARGET_OPERATION.toString()))
        .setDefaultValue(builder.targetOperation.toString());
    }

    /**
     * Parse
     */
    cliParser = builder.childCommand.parse();

  }

  public static Config config(Tabular tabular, CliCommand childCommand, TransferOperation transferOperation, TabulDataTransferCommandType transferCommandType) {
    return new Config(tabular, childCommand, transferOperation, transferCommandType);
  }


  private String toLongOption(String string) {
    return CliParser.PREFIX_LONG_OPTION + KeyNormalizer.createSafe(string).toCliLongOptionName();
  }


  /**
   * Utility function to add all transfer options needed
   * in every case except with the {@link TabulDataFill} command
   */
  void addAllTransferOptions() {

    CliCommand childCommand = builder.childCommand;
    /**
     * Source creation
     */
    if (builder.commandType.equals(TabulDataTransferCommandType.CONCAT)) {
      childCommand.addArg(SOURCE_SELECTORS)
        .setDescription("One or more data selectors that select the data resources to concatenate")
        .setMandatory(true);
    } else {
      childCommand.addArg(SOURCE_SELECTOR)
        .setDescription("A data selector that select the data resources to transfer")
        .setMandatory(true);
    }
    childCommand.addProperty(SOURCE_ATTRIBUTE);
    childCommand.addFlag(WITH_DEPENDENCIES_PROPERTY);
    childCommand.addProperty(VIRTUAL_COLUMN_PROPERTY);
    childCommand.addProperty(TYPE_PROPERTY);

    /**
     * Target creation
     */
    if (builder.commandType.equals(TabulDataTransferCommandType.CONCAT)) {
      /**
       * The target is not mandatory so that we can print the source to the console
       */
      childCommand.addArg(TARGET_DATA_URI)
        .setDescription("The target data uri that defines the receiving target data resource (No templating is allowed)")
        .setMandatory(false);
    } else {
      childCommand.addArg(TARGET_DATA_URI)
        .setDescription(PipelineStepBuilderTargetArgument.TARGET_DATA_URI.getDescription())
        .setMandatory(false);
    }

    childCommand.addProperty(TARGET_ATTRIBUTE);

    /**
     * Transfer Options
     */
    for (TransferPipelineStepArgument value : values()) {

      addTransferOptionToCommand(value);

    }

    /**
     * Default transfer operation
     */
    if (builder.transferOperation != null) {
      childCommand.getLocalWord(TRANSFER_OPERATION_OPTION)
        .setDefaultValue(builder.transferOperation.toString());
    }

  }


  /**
   * Add a transfer argument option to the command
   *
   * @param transferPipelineStepArgument the transfer property
   * @return the word
   */
  private CliWord addTransferOptionToCommand(TransferPipelineStepArgument transferPipelineStepArgument) {

    CliCommand childCommand = builder.childCommand;
    Object defaultValue = transferPipelineStepArgument.getDefaultValue();
    CliWord cliWord;
    if (defaultValue != null) {
      if (defaultValue instanceof Boolean) {
        cliWord = childCommand.addFlag(CliParser.PREFIX_LONG_OPTION + KeyNormalizer.createSafe(transferPipelineStepArgument).toCliLongOptionName());
      } else {
        cliWord = childCommand.addProperty(CliParser.PREFIX_LONG_OPTION + KeyNormalizer.createSafe(transferPipelineStepArgument).toCliLongOptionName());
      }
    } else {
      cliWord = childCommand.addProperty(CliParser.PREFIX_LONG_OPTION + KeyNormalizer.createSafe(transferPipelineStepArgument).toCliLongOptionName());
    }
    String shortOption;
    switch (transferPipelineStepArgument) {
      case TRANSFER_OPERATION:
        // to not conflict with target operation
        shortOption = "op";
        break;
      case OUTPUT_TYPE:
        // to no conflict with source-operation
        shortOption = "out";
        break;
      default:
        shortOption = KeyNormalizer.createSafe(transferPipelineStepArgument).toCliShortOptionName();
    }

    cliWord.setGroup("Cross Data Transfer Options")
      .setDescription(transferPipelineStepArgument.getDescription())
      .setShortName(CliParser.PREFIX_SHORT_OPTION + shortOption)
      .setValueName(transferPipelineStepArgument.toString());
    if (defaultValue != null) {
      cliWord.setDefaultValue(transferPipelineStepArgument.getDefaultValue());
    }
    return cliWord;

  }


  public TransferPropertiesSystem.TransferPropertiesSystemBuilder getSystemTransferProperties() {
    Boolean transferMappingStrict = cliParser.getBoolean(TabulWords.TRANSFER_MAPPING_STRICT);
    Boolean withBindVariables = cliParser.getBoolean(TabulWords.WITH_PARAMETERS);
    if (transferMappingStrict == null) {
      transferMappingStrict = true;
    }
    TransferPropertiesSystem.TransferPropertiesSystemBuilder transferPropertiesSystemBuilder = TransferPropertiesSystem.builder()
      .setStrictMapping(transferMappingStrict)
      .setWithParameters(withBindVariables);

    String transferOperation = cliParser.getString(TRANSFER_OPERATION_OPTION);
    if (transferOperation != null) {
      transferPropertiesSystemBuilder.setOperation(TransferOperation.createFrom(transferOperation));
    } else {
      /**
       * data command copy, insert, ...set it
       */
      if (this.builder.transferOperation != null) {
        transferPropertiesSystemBuilder.setOperation(this.builder.transferOperation);
      }
      /**
       * Otherwise the default of {@link TransferPipelineStep} is used
       */
    }

    TransferResourceOperations[] targetOperations = cliParser.getStrings(TARGET_OPERATION_OPTION).stream()
      .map(s -> {
        try {
          return Casts.cast(s, TransferResourceOperations.class);
        } catch (CastException e) {
          throw IllegalArgumentExceptions.createForArgumentValue(s, TARGET_OPERATION_OPTION, TransferResourceOperations.class, e);
        }
      })
      .toArray(TransferResourceOperations[]::new);
    transferPropertiesSystemBuilder.setTargetOperations(targetOperations);

    List<String> sourceOperationProperties = cliParser.getStrings(SOURCE_OPERATION_OPTION);
    if (sourceOperationProperties != null) {
      TransferResourceOperations[] sourceOperations = sourceOperationProperties.stream()
        .map(s -> {
          try {
            return Casts.cast(s, TransferResourceOperations.class);
          } catch (CastException e) {
            throw IllegalArgumentExceptions.createForArgumentValue(s, SOURCE_OPERATION_OPTION, TransferResourceOperations.class, e);
          }
        })
        .toArray(TransferResourceOperations[]::new);
      transferPropertiesSystemBuilder.setSourceOperations(sourceOperations);
    }
    return transferPropertiesSystemBuilder;

  }

  public TransferPropertiesCross getCrossTransferProperties() {


    Integer batchSize = cliParser.getInteger(TARGET_BATCH_SIZE_OPTION);
    Integer fetchSize = cliParser.getInteger(SOURCE_FETCH_SIZE_OPTION);
    Integer commitFrequency = cliParser.getInteger(TARGET_COMMIT_FREQUENCY_OPTION);
    int targetWorkerCount = cliParser.getInteger(TARGET_WORKER_OPTION);
    Integer bufferSize = cliParser.getInteger(BUFFER_SIZE_OPTION);

    if (bufferSize == null) {
      bufferSize = 2 * targetWorkerCount * fetchSize;
      TabulLog.LOGGER_TABUL.info(BUFFER_SIZE_OPTION + " parameter NOT found. Using default : " + bufferSize);
    }

    TransferPropertiesCross transferPropertiesCross = TransferPropertiesCross.create()
      .setBufferSize(bufferSize)
      .setTargetWorkerCount(targetWorkerCount)
      .setFetchSize(fetchSize)
      .setBatchSize(batchSize)
      .setCommitFrequency(commitFrequency);


    String metricsDataUri = cliParser.getString(METRICS_DATA_URI_OPTION);
    if (metricsDataUri != null) {
      DataPath metricsDataPath = builder.tabular.getDataPath(metricsDataUri);
      transferPropertiesCross.setMetricsDataPath(metricsDataPath);
    }

    return transferPropertiesCross;

  }


  /**
   * The source target is created from the selectors if it was not injected such as in the {@link TabulDataFill}
   */
  public void addPipelineSelectStep(PipelineBuilder pipelineBuilder) {

    Tabular tabular = builder.tabular;
    Boolean withDependencies = cliParser.getBoolean(WITH_DEPENDENCIES_PROPERTY);
    MediaType mediaType = null;
    try {
      String string = cliParser.getString(TYPE_PROPERTY);
      mediaType = MediaTypes.parse(string);
    } catch (NullValueException e) {
      // ok, null
    }

    switch (builder.commandType) {

      case CONCAT:
      case DEFAULT:

        /**
         * The Data Uri are not created from the
         */
        dataSelectors = new ArrayList<>();
        if (builder.commandType == TabulDataTransferCommandType.CONCAT) {

          /**
           * They are not mandatory, no need to check for null
           * Trick as source selectors will get only the first arguments
           */
          String sourceSelectors = cliParser.getString(SOURCE_SELECTORS);
          dataSelectors.add(tabular.createDataUri(sourceSelectors));
          /**
           * All other source and the final target uri are in target-data-uri
           */
          List<String> targetDataUris = cliParser.getStrings(TARGET_DATA_URI);
          if (!targetDataUris.isEmpty()) {
            dataSelectors.addAll(targetDataUris.subList(0, targetDataUris.size() - 1)
              .stream()
              .map(tabular::createDataUri)
              .collect(Collectors.toList()));
          } else {
            /**
             * Only one source concat case, we print it as text
             */
            if (mediaType == null) {
              mediaType = MediaTypes.TEXT_PLAIN;
            }
          }

        } else {
          dataSelectors.add(tabular.createDataUri(cliParser.getString(SOURCE_SELECTOR)));
        }


        final Map<String, String> sourceAttributesRaw = cliParser.getProperties(SOURCE_ATTRIBUTE);
        final Map<KeyNormalizer, String> sourceAttributes;
        try {
          sourceAttributes = Casts.castToNewMap(sourceAttributesRaw, KeyNormalizer.class, String.class);
        } catch (CastException e) {
          throw new IllegalArgumentException("The validation of the values of the option " + SOURCE_ATTRIBUTE + " returns an error: " + e.getMessage(), e);
        }

        final Map<String, String> virtualColumns = cliParser.getProperties(VIRTUAL_COLUMN_PROPERTY);


        pipelineBuilder
          .addStep(
            SelectPipelineStep.builder()
              .setDataSelectors(dataSelectors)
              .setWithDependencies(withDependencies)
              .setDataDef(sourceAttributes)
              .setMediaType(mediaType)
          )
          .addStep(
            EnrichPipelineStep.builder()
              .addMetaColumns(virtualColumns)
          );
        return;

      case FILL:


        final long maxRowCount = cliParser.getInteger(MAX_RECORD_COUNT).longValue();
        List<String> genDataUris = cliParser.getStrings(GENERATOR_SELECTOR);
        List<DataUriNode> generatorSelectorsDataUriList = new ArrayList<>();
        for (String generatorSelector : genDataUris) {
          generatorSelectorsDataUriList.add(tabular.createDataUri(generatorSelector));
        }
        this.dataSelectors = cliParser.getStrings(TARGET_SELECTOR)
          .stream()
          .map(tabular::createDataUri)
          .collect(Collectors.toList());

        /**
         * Case when the selector is a data locator, the resource should exist
         * to select it
         */
        for (DataUriNode targetDataUri : this.dataSelectors) {
          if (!targetDataUri.isGlobPattern()) {
            // plain because there is no structure
            if (targetDataUri.getConnection() instanceof FsConnection) {
              mediaType = MediaTypes.TEXT_PLAIN;
            }
            DataPath dataPath = tabular.getDataPath(targetDataUri, mediaType);
            if (!Tabulars.exists(dataPath)) {
              Tabulars.create(dataPath);
            }
          }
        }

        /**
         * Return
         */
        pipelineBuilder
          .addStep(
            SelectPipelineStep.builder()
              .setDataSelectors(dataSelectors)
              .setWithDependencies(withDependencies)
              .setOrder(SelectPipelineStepArgumentOrder.CREATE)
              .setStrictSelection(true)
          )
          .addStep(
            FillPipelineStep.builder()
              .setMaxRecordCount(maxRowCount)
              .setGeneratorSelectors(generatorSelectorsDataUriList)
          );
        return;
      default:
        throw new IllegalStateException("The transfer command type (" + builder.commandType + ") is unknown.");
    }

  }


  /**
   * Run should return only one data path
   * but if the selector does not select anything
   * it may be null. Instead, we return an empty list
   * as this is the standard return of all command
   */
  public List<DataPath> run() {

    PipelineBuilder pipelineBuilder = Pipeline.builder(this.builder.tabular);
    addPipelineSelectStep(pipelineBuilder);
    addPipelineTransferStep(pipelineBuilder);

    List<DataPath> dataPaths = pipelineBuilder
      .build()
      .execute()
      .getDownStreamDataPaths();
    if (dataPaths.isEmpty()) {
      Tabul.printEmptySelectionFeedback(dataSelectors);
    }
    return dataPaths;
  }

  private void addPipelineTransferStep(PipelineBuilder pipelineSelectPart) {

    String targetUriArgument = cliParser.getString(TARGET_DATA_URI);
    if (builder.commandType == TabulDataTransferCommandType.CONCAT) {
      // In concat, the target data uri gets also the source selectors
      // We get only the last one
      List<String> targetDataUris = cliParser.getStrings(TARGET_DATA_URI);
      if (!targetDataUris.isEmpty()) {
        targetUriArgument = targetDataUris.get(targetDataUris.size() - 1);
      }
      if (targetUriArgument == null) {
        pipelineSelectPart.addStep(
          PrintPipelineStep
            .builder()
            .setFormat(PrinterPrintFormat.PIPE)
        );
        return;
      }
    }
    DataUriNode targetDataUri;
    switch (builder.commandType) {

      case FILL:

        /**
         * The target uri is derived from the target selector
         * as being the parent
         */
        final List<DataUriNode> targetDataUriSelectors = cliParser.getStrings(TARGET_SELECTOR)
          .stream()
          .map(this.builder.tabular::createDataUri)
          .collect(Collectors.toList());
        Set<DataUriNode> dataUriSet = new HashSet<>();
        for (DataUriNode dataUri : targetDataUriSelectors) {
          /**
           * Case when a specific file is asked
           */
          if (!dataUri.isGlobPattern()) {
            dataUriSet.add(dataUri);
            continue;
          }
          String path = null;
          try {
            path = dataUri.getPath();
            // Parent
            List<String> parentSeparators = List.of("/", "\\", ".");
            for (String parentSeparator : parentSeparators) {
              if (path.contains(parentSeparator)) {
                path = path.substring(0, path.lastIndexOf(parentSeparator));
              }
            }
          } catch (NoPathFoundException e) {
            // no path
          }
          if (path != null) {
            dataUriSet.add(
              DataUriNode
                .builder()
                .setConnection(dataUri.getConnection())
                .setPath(path)
                .build()
            );
          } else {
            dataUriSet.add(DataUriNode.createFromConnection(dataUri.getConnection()));
          }
        }
        if (dataUriSet.size() > 1) {
          throw new IllegalArgumentException("The fill command does not support multiple target data uris. Actual target uris: " + dataUriSet.stream().map(DataUriNode::toString).collect(Collectors.joining(", ")));
        }
        targetDataUri = dataUriSet.iterator().next();

        /**
         * Hack
         * With dependencies means that we will get more than one target
         * with only one target uri
         * We need to use the connection
         */
        Boolean withDependencies = cliParser.getBoolean(WITH_DEPENDENCIES_PROPERTY);
        if (withDependencies) {
          // with dependencies will add
          targetDataUri = DataUriNode.createFromConnection(targetDataUri.getConnection());
        }
        /**
         * Hack
         * ie `*@connection` cannot be a target
         */
        if (targetDataUri.isGlobPattern()) {
          targetDataUri = DataUriNode.createFromConnection(targetDataUri.getConnection());
        }
        break;
      case CONCAT:
      case DEFAULT:
      default:
        if (targetUriArgument != null) {
          targetDataUri = this.builder.tabular.createDataUri(targetUriArgument);
        } else {
          targetDataUri = this.builder.tabular.getDefaultUri();
        }
        break;
    }


    final Map<String, String> targetAttributesRaw = cliParser.getProperties(TARGET_ATTRIBUTE);
    final Map<KeyNormalizer, String> targetAttributes;
    try {
      targetAttributes = Casts.castToNewMap(targetAttributesRaw, KeyNormalizer.class, String.class);
    } catch (CastException e) {
      throw new IllegalArgumentException("The validation of the values of the option " + TARGET_ATTRIBUTE + " returns an error: " + e.getMessage(), e);
    }

    pipelineSelectPart.addStep(
      TransferPipelineStep.builder()
        .setTransferPropertiesSystem(getSystemTransferProperties())
        .setTransferCrossProperties(getCrossTransferProperties())
        .setOutput(StepOutputArgument.RESULTS)
        .setTargetDataUri(targetDataUri)
        .setTargetDataDef(targetAttributes)
    );
  }


  private void addFillTransferOptions() {

    CliCommand childCommand = builder.childCommand;
    /**
     * Source / Target map creation options
     */
    String fillOptionsGroupName = "Fill Options";
    childCommand.addProperty(GENERATOR_SELECTOR)
      .setDescription("One or more data selector that selects one or more data resource generator ( " + GeneratorMediaType.DATA_GEN.getExtension() + " )")
      .setValueName("generatorSelector...")
      .setShortName("-gs")
      .setGroup(fillOptionsGroupName);
    childCommand.addArg(TARGET_SELECTOR)
      .setDescription("One or more data selectors that will select the target data resources to be filled.")
      .setMandatory(true);
    childCommand.addFlag(WITH_DEPENDENCIES_PROPERTY)
      .setDescription("If this flag is present, the dependencies of the selected target tables (ie parent/foreign tables) will be also be filled with data")
      .setDefaultValue(false);
    childCommand.addProperty(TARGET_ATTRIBUTE);

    /**
     * Row count to cap
     */
    childCommand.addProperty(MAX_RECORD_COUNT)
      .setDescription("This option defines the maximum total number of record that the data resource(s) must have when no data resource generator was found.")
      .setDefaultValue(GenDataPathAttribute.MAX_RECORD_COUNT.getDefaultValue())
      .setGroup(fillOptionsGroupName)
      .setValueName("maxRecordCount")
      .setShortName("-mrc");


    /**
     * Transfer Arguments
     */
    for (TransferPipelineStepArgument value : values()) {
      /**
       * There is no source in fill
       */
      if (value != SOURCE_OPERATION) {
        addTransferOptionToCommand(value);
      }
    }

    /**
     * Transfer operation
     */
    // When working on data generation, the default transfer operation is upsert
    childCommand.getLocalWord(TRANSFER_OPERATION_OPTION)
      .setDefaultValue(DEFAULT_FILL_TRANSFER_OPERATION);

  }

  public CliParser getParser() {
    return this.cliParser;
  }

  public static class Config {
    private final CliCommand childCommand;
    private final TransferOperation transferOperation;
    private final TabulDataTransferCommandType commandType;
    private final Tabular tabular;
    private TransferResourceOperations sourceOperation;
    private TransferResourceOperations targetOperation;

    public Config(Tabular tabular, CliCommand childCommand, TransferOperation transferOperation, TabulDataTransferCommandType commandType) {
      this.childCommand = childCommand;
      this.transferOperation = transferOperation;
      this.tabular = tabular;
      this.commandType = commandType;
    }

    public Config setSourceOperation(TransferResourceOperations transferResourceOperations) {
      this.sourceOperation = transferResourceOperations;
      return this;
    }

    public Config setTargetOperation(TransferResourceOperations transferResourceOperations) {
      this.targetOperation = transferResourceOperations;
      return this;
    }

    public TabulDataTransferManager build() {

      return new TabulDataTransferManager(this);
    }


  }
}
