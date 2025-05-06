package com.tabulify.tabli;

import com.tabulify.Tabular;
import com.tabulify.connection.Connection;
import com.tabulify.flow.engine.Pipeline;
import com.tabulify.flow.step.SelectSupplier;
import com.tabulify.flow.step.TargetArguments;
import com.tabulify.flow.step.TargetPipelineSimple;
import com.tabulify.flow.step.TransferArgumentProperty;
import com.tabulify.gen.DataGenerator;
import com.tabulify.gen.GenDataPath;
import com.tabulify.gen.GenDataPathAttribute;
import com.tabulify.gen.GenDataPathType;
import com.tabulify.spi.DataPath;
import com.tabulify.transfer.*;
import com.tabulify.uri.DataUri;
import net.bytle.cli.CliCommand;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliWord;
import net.bytle.exception.CastException;
import net.bytle.exception.IllegalArgumentExceptions;
import net.bytle.exception.NullValueException;
import net.bytle.type.Casts;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.MediaType;
import net.bytle.type.MediaTypes;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tabulify.flow.step.TransferArgumentProperty.SOURCE_OPERATION;
import static com.tabulify.flow.step.TransferArgumentProperty.values;
import static com.tabulify.tabli.TabliWords.*;
import static java.util.stream.Collectors.toSet;

public class TabliDataTransferManager {


  public static final TransferOperation DEFAULT_FILL_TRANSFER_OPERATION = TransferOperation.UPSERT;


  public enum TransferCommandType {
    FILL, // the fill transfer has other option
    DEFAULT // All other transfer
  }


  /**
   * Utility function to add all transfer options needed
   * in every case except with the {@link TabliDataFill} command
   */
  public static void addAllTransferOptions(CliCommand childCommand, TransferOperation defaultTransferOperation) {

    /**
     * Source creation
     */
    childCommand.addArg(SOURCE_SELECTOR)
      .setDescription("A data selector that select the data resources to transfer")
      .setMandatory(true);
    childCommand.addProperty(SOURCE_ATTRIBUTE);
    childCommand.addFlag(WITH_DEPENDENCIES_PROPERTY);
    childCommand.addProperty(VIRTUAL_COLUMN_PROPERTY);
    childCommand.addProperty(TYPE_PROPERTY);

    /**
     * Target creation
     */
    childCommand.addArg(TabliWords.TARGET_DATA_URI)
      .setDescription(TargetArguments.TARGET_DATA_URI.getDescription())
      .setMandatory(false);
    childCommand.addProperty(TARGET_ATTRIBUTE_PROPERTY);

    /**
     * Transfer Options
     */
    for (TransferArgumentProperty value : values()) {

      addTransferOptionToCommand(childCommand, value);

    }

    /**
     * Default transfer operation
     */
    childCommand.getLocalWord(TRANSFER_OPERATION_OPTION)
      .setDefaultValue(defaultTransferOperation.toString());

  }


  /**
   * Add a transfer argument option to the command
   *
   * @param childCommand             the child command
   * @param transferArgumentProperty the transfer property
   * @return the word
   */
  private static CliWord addTransferOptionToCommand(CliCommand childCommand, TransferArgumentProperty transferArgumentProperty) {

    Object defaultValue = transferArgumentProperty.getDefaultValue();
    CliWord cliWord;
    if (defaultValue != null) {
      if (defaultValue instanceof Boolean) {
        cliWord = childCommand.addFlag(CliParser.PREFIX_LONG_OPTION + KeyNormalizer.createSafe(transferArgumentProperty).toCliLongOptionName());
      } else {
        cliWord = childCommand.addProperty(CliParser.PREFIX_LONG_OPTION + KeyNormalizer.createSafe(transferArgumentProperty).toCliLongOptionName());
      }
    } else {
      cliWord = childCommand.addProperty(CliParser.PREFIX_LONG_OPTION + KeyNormalizer.createSafe(transferArgumentProperty).toCliLongOptionName());
    }
    String shortOption;
    switch (transferArgumentProperty) {
      case TRANSFER_OPERATION:
        // to not conflict with target operation
        shortOption = "op";
        break;
      case STEP_OUTPUT:
        // to no conflict with source-operation
        shortOption = "out";
        break;
      default:
        shortOption = KeyNormalizer.createSafe(transferArgumentProperty).toCliShortOptionName();
    }

    cliWord.setGroup("Cross Data Transfer Options")
      .setDescription(transferArgumentProperty.getDescription())
      .setShortName(CliParser.PREFIX_SHORT_OPTION + shortOption)
      .setValueName(transferArgumentProperty.toString());
    if (defaultValue != null) {
      cliWord.setDefaultValue(transferArgumentProperty.getDefaultValue());
    }
    return cliWord;

  }


  public static TransferProperties getTransferProperties(Tabular tabular, CliParser cliParser) {


    Integer batchSize = cliParser.getInteger(TARGET_BATCH_SIZE_OPTION);
    Integer fetchSize = cliParser.getInteger(SOURCE_FETCH_SIZE_OPTION);
    Integer commitFrequency = cliParser.getInteger(TARGET_COMMIT_FREQUENCY_OPTION);
    int targetWorkerCount = cliParser.getInteger(TabliWords.TARGET_WORKER_OPTION);
    Integer bufferSize = cliParser.getInteger(BUFFER_SIZE_OPTION);
    Boolean withBindVariables = cliParser.getBoolean(TabliWords.WITH_BIND_VARIABLES);
    if (bufferSize == null) {
      bufferSize = 2 * targetWorkerCount * fetchSize;
      TabliLog.LOGGER_TABLI.info(BUFFER_SIZE_OPTION + " parameter NOT found. Using default : " + bufferSize);
    }

    TransferProperties transferProperties = TransferProperties.create()
      .setBufferSize(bufferSize)
      .setTargetWorkerCount(targetWorkerCount)
      .setFetchSize(fetchSize)
      .setBatchSize(batchSize)
      .setCommitFrequency(commitFrequency)
      .setWithBindVariablesStatement(withBindVariables);

    String transferOperation = cliParser.getString(TRANSFER_OPERATION_OPTION);
    if (transferOperation != null) {
      transferProperties.setOperation(TransferOperation.createFrom(transferOperation));
    }

    String metricsDataUri = cliParser.getString(METRICS_DATA_URI_OPTION);
    if (metricsDataUri != null) {
      DataPath metricsDataPath = tabular.getDataPath(metricsDataUri);
      transferProperties.setMetricsDataPath(metricsDataPath);
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
    transferProperties.addTargetOperations(targetOperations);

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
      transferProperties.addSourceOperations(sourceOperations);
    }


    return transferProperties;

  }


  /**
   * The source target is created from the selectors if it was not injected such as in the {@link TabliDataFill}
   */
  public static Map<? extends DataPath, ? extends DataPath> getSourceTarget(Tabular tabular, CliParser cliParser, TransferCommandType transferCommandType) {


    if (transferCommandType == TransferCommandType.DEFAULT) {

      /**
       * The Data Uri are not created from the
       */
      final DataUri dataSelectors = tabular.createDataUri(cliParser.getString(SOURCE_SELECTOR));
      String targetUriArgument = cliParser.getString(TabliWords.TARGET_DATA_URI);
      DataUri targetDataUri;
      if (targetUriArgument != null) {
        targetDataUri = tabular.createDataUri(targetUriArgument);
      } else {
        targetDataUri = tabular.getDefaultUri();
      }
      final Boolean withDependencies = cliParser.getBoolean(WITH_DEPENDENCIES_PROPERTY);
      final Map<String, String> sourceAttributes = cliParser.getProperties(SOURCE_ATTRIBUTE);
      final Map<String, String> targetAttributes = cliParser.getProperties(TARGET_ATTRIBUTE_PROPERTY);
      final Map<String, String> virtualColumns = cliParser.getProperties(VIRTUAL_COLUMN_PROPERTY);

      /**
       * The source target
       */
      TargetPipelineSimple pipeline = TargetPipelineSimple.create(tabular)
        .setTargetUri(targetDataUri)
        .setDataSelector(dataSelectors)
        .setWithDependencies(withDependencies)
        .setSourceAttributes(sourceAttributes)
        .setTargetAttributes(targetAttributes)
        .setVirtualColumns(virtualColumns);


      try {
        String string = cliParser.getString(TYPE_PROPERTY);
        final MediaType mediaType = MediaTypes.createFromMediaTypeString(string);
        pipeline.setMediaType(mediaType);
      } catch (NullValueException e) {
        // ok, null
      }


      return pipeline.getSourceTargets();

    } else if (transferCommandType == TransferCommandType.FILL) {

      final Boolean withDependencies = cliParser.getBoolean(WITH_DEPENDENCIES_PROPERTY);
      final long maxRowCount = cliParser.getInteger(MAX_RECORD_COUNT).longValue();
      List<String> genDataUris = cliParser.getStrings(GENERATOR_SELECTOR);

      final Set<DataUri> targetDataUriSelectors = cliParser.getStrings(TARGET_SELECTOR)
        .stream()
        .map(tabular::createDataUri)
        .collect(Collectors.toSet());

      /**
       * Target data resource to fill initialization
       */
      Set<DataPath> targetDataPaths =
        Pipeline.create(tabular)
          .addStepToGraph(
            SelectSupplier.create()
              .setTabular(tabular)
              .setDataSelectors(targetDataUriSelectors)
              .setWithDependencies(withDependencies)
          )
          .execute()
          .getDownStreamDataPaths();

      Set<Connection> connections = targetDataPaths
        .stream()
        .map(DataPath::getConnection)
        .collect(toSet());
      if (connections.size() > 1) {
        throw new RuntimeException("Filling multiple target from different connection are not yet supported. We found the following connections " + connections.stream().sorted().map(Connection::getName).collect(Collectors.joining(", ")));
      }

      // Do we have selected something
      if (targetDataPaths.isEmpty()) {
        TabliLog.LOGGER_TABLI.fine("The data uri selectors (" + targetDataUriSelectors.stream().map(DataUri::toString).collect(Collectors.joining(", ")) + ") are not selectors that select data resources, we use them as data uri");
        targetDataPaths = targetDataUriSelectors.stream()
          .map(du -> tabular.getDataPath(du.toString()))
          .collect(Collectors.toSet());
      }


      /**
       * Fill gen data def if any
       */
      Set<DataPath> genDataPaths = new HashSet<>();
      if (genDataUris != null) {
        genDataPaths = SelectSupplier.create()
          .setTabular(tabular)
          .setDataSelectors(
            genDataUris.stream()
              .map(tabular::createDataUri)
              .collect(Collectors.toSet())
          )
          .setWithDependencies(false)
          .getAll();

      }
      Map<String, DataPath> genDataPathMap = genDataPaths
        .stream()
        .collect(Collectors.toMap(DataPath::getLogicalName, Function.identity()));


      /**
       * Building the data generation object
       */
      DataGenerator dataGenerator = DataGenerator.create(tabular);
      for (DataPath targetDataPath : targetDataPaths) {

        GenDataPath sourceDataPath = (GenDataPath) genDataPathMap.get(targetDataPath.getLogicalName());
        if (sourceDataPath != null) {
          if (sourceDataPath.getMaxRecordCount() == null) {
            sourceDataPath.setMaxRecordCount(maxRowCount);
          }
          dataGenerator.addTransfer(sourceDataPath, targetDataPath);
        } else {
          if (targetDataPath.getOrCreateRelationDef().getColumnsSize() == 0) {
            StringBuilder s = new StringBuilder();
            s.append("The target to fill (")
              .append(targetDataPath)
              .append(") has no column definitions.");
            if (!genDataPaths.isEmpty()) {
              s.append(" We found the following generators (");
              s.append(genDataPaths
                .stream()
                .map(dp -> dp.toDataUri().toString())
                .collect(Collectors.joining(","))
              );
              s.append(") but they don't match the target logical name (")
                .append(targetDataPath.getLogicalName())
                .append(")");
            } else {
              if (genDataUris == null || genDataUris.isEmpty()) {
                s.append(" You should use the (" + GENERATOR_SELECTOR + ") option to add a generator.");
              } else {
                s.append(" The given generator selectors (")
                  .append(String.join(", ", genDataUris))
                  .append(") does not select any generators");
              }
            }
            throw new IllegalStateException(s.toString());
          }
          dataGenerator.addDummyTransfer(targetDataPath, maxRowCount);
        }
      }


      String withOrWithoutParent = "without the dependencies (the parent/foreign table)";
      if (withDependencies) {
        withOrWithoutParent = "with the dependencies (the parent/foreign table)";
        // The selection has already taken into account the dependency, no need to add to the data generation
        // object the option withDependencies
      }

      String dataStore = "";
      TabliLog.LOGGER_TABLI.info("Starting filling the tables for the data store " + dataStore + " " + withOrWithoutParent);

      /**
       * Return
       */
      return dataGenerator.generateSourceTargetMap().getSourceTarget();
    } else {

      throw new IllegalStateException("The transfer command type (" + transferCommandType + ") is unknown.");
    }

  }


  public static Set<DataPath> runAndGetFeedBacks(Tabular tabular, CliParser cliParser, TransferCommandType transferCommandType) {
    Map<? extends DataPath, ? extends DataPath> sourceTargets = getSourceTarget(tabular, cliParser, transferCommandType);
    TransferProperties transferProperties = TabliDataTransferManager.getTransferProperties(tabular, cliParser);
    List<TransferListener> transferListeners = Transfers.transfers(tabular, sourceTargets, transferProperties);
    return Transfers.transfersListenersToDataPath(tabular, transferListeners);
  }


  static public void addFillTransferOptions(CliCommand childCommand) {


    /**
     * Source / Target map creation options
     */
    String fillOptionsGroupName = "Fill Options";
    childCommand.addProperty(GENERATOR_SELECTOR)
      .setDescription("One or more data selector that selects one or more data resource generator ( " + GenDataPathType.DATA_GEN.getExtension() + " )")
      .setValueName("generatorSelector...")
      .setShortName("-gs")
      .setGroup(fillOptionsGroupName);
    childCommand.addArg(TARGET_SELECTOR)
      .setDescription("One or more data selectors that will select the target data resources to be filled.")
      .setMandatory(true);
    childCommand.addFlag(WITH_DEPENDENCIES_PROPERTY)
      .setDescription("If this flag is present, the dependencies of the selected target tables (ie parent/foreign tables) will be also be filled with data")
      .setDefaultValue(false);
    childCommand.addProperty(TARGET_ATTRIBUTE_PROPERTY);

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
    for (TransferArgumentProperty value : values()) {
      /**
       * There is no source in fill
       */
      if (value != SOURCE_OPERATION) {
        addTransferOptionToCommand(childCommand, value);
      }
    }

    /**
     * Transfer operation
     */
    // When working on data generation, the default transfer operation is upsert
    childCommand.getLocalWord(TRANSFER_OPERATION_OPTION)
      .setDefaultValue(DEFAULT_FILL_TRANSFER_OPERATION);

  }
}
