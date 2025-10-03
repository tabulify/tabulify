package com.tabulify.flow.operation;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.connection.Connection;
import com.tabulify.engine.ForeignKeyDag;
import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.flow.engine.PipelineStepBuilderStreamSupplier;
import com.tabulify.flow.engine.PipelineStepSupplierDataPath;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.Tabulars;
import com.tabulify.template.TemplateMetas;
import com.tabulify.template.TemplateString;
import com.tabulify.uri.DataUriNode;
import com.tabulify.uri.DataUriStringNode;
import net.bytle.dag.Dag;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.type.*;

import java.util.*;

import static java.util.stream.Collectors.*;

public class SelectPipelineStep extends PipelineStepBuilderStreamSupplier {

  public static final KeyNormalizer SELECT = KeyNormalizer.createSafe("select");
  SelectPipelineStepArgumentOrder order = (SelectPipelineStepArgumentOrder) SelectPipelineStepArgument.ORDER.getDefaultValue();
  private PipelineStepProcessingType processingType = (PipelineStepProcessingType) SelectPipelineStepArgument.PROCESSING_TYPE.getDefaultValue();


  @Override
  public Boolean acceptOperation(KeyNormalizer operationName) {

    return SELECT.equals(operationName);

  }

  @Override
  public KeyNormalizer getOperationName() {

    return SELECT;

  }

  @Override
  public Set<KeyNormalizer> getAcceptedCommandNames() {
    return Collections.singleton(SELECT);
  }

  @Override
  public SelectPipelineStep createStepBuilder() {
    return new SelectPipelineStep();
  }

  /**
   * If a data selector does not return any data
   * an error is thrown
   * <p>
   * Used when the next operation is a drop
   * By default, a drop/truncate tabli command is strict (ie not ifExist drop)
   * in a flow, this is not the case
   */
  boolean isStrict = (boolean) SelectPipelineStepArgument.STRICT_SELECTION.getDefaultValue();

  /**
   * Selection with dependencies
   */
  Boolean withDependencies = (Boolean) SelectPipelineStepArgument.WITH_DEPENDENCIES.getDefaultValue();
  /**
   * The data selectors
   */
  List<DataUriNode> dataSelectors = new ArrayList<>();

  MediaType mediaType = null;
  String logicalName;
  Map<KeyNormalizer, ?> inlineDataAttributes;


  public SelectPipelineStep setDataSelector(DataUriNode dataSelector) {
    this.dataSelectors = List.of(dataSelector);
    return this;
  }

  public SelectPipelineStep setDataSelectors(List<DataUriNode> dataSelectors) {
    this.dataSelectors = dataSelectors;
    return this;
  }

  public SelectPipelineStep setWithDependencies(Boolean withDependencies) {
    if (withDependencies != null) {
      this.withDependencies = withDependencies;
    }
    return this;
  }

  /**
   * @param dataDef - set the data attributes in a data def map format
   */
  public SelectPipelineStep setDataDef(Map<KeyNormalizer, ?> dataDef) {
    this.inlineDataAttributes = dataDef;
    return this;
  }

  public SelectPipelineStep setLogicalName(String logicalName) {
    this.logicalName = logicalName;
    return this;
  }

  public SelectPipelineStep setProcessingType(PipelineStepProcessingType processingType) {
    this.processingType = processingType;
    return this;
  }

  public SelectPipelineStep setMediaType(MediaType mediaType) {
    this.mediaType = mediaType;
    return this;
  }

  public SelectPipelineStep setStrictSelection(boolean strictness) {
    this.isStrict = strictness;
    return this;
  }

  @Override
  public List<Class<? extends ArgumentEnum>> getArgumentEnums() {
    ArrayList<Class<? extends ArgumentEnum>> list = new ArrayList<>(super.getArgumentEnums());
    list.add(SelectPipelineStepArgument.class);
    return list;
  }

  public static SelectPipelineStep builder() {
    return new SelectPipelineStep();
  }


  public PipelineStepSupplierDataPath build() {

    if (this.processingType == PipelineStepProcessingType.BATCH) {
      return new SelectPipelineStepBatch(this);
    }
    return new SelectPipelineStepStream(this);

  }

  @Override
  public SelectPipelineStep setArgument(KeyNormalizer key, Object value) {


    SelectPipelineStepArgument selectArgument;
    try {
      selectArgument = Casts.cast(key, SelectPipelineStepArgument.class);
    } catch (CastException e) {
      throw new IllegalArgumentException("The argument (" + key + ") is not a valid argument for the step (" + this + "). You can choose one of: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(SelectPipelineStepArgument.class));
    }

    Tabular tabular = this.getTabular();

    Attribute attribute;
    try {
      attribute = tabular.getVault()
        .createVariableBuilderFromAttribute(selectArgument)
        .setOrigin(Origin.PIPELINE)
        .build(value);
      this.setArgument(attribute);
    } catch (CastException e) {
      throw new IllegalArgumentException("The " + selectArgument + " value (" + value + ") of the step (" + this + ") is not conform . Error: " + e.getMessage(), e);
    }

    switch (selectArgument) {
      case DATA_SELECTOR:
        this.setDataSelector(this.getPipelineBuilder().getDataUri((DataUriStringNode) attribute.getValueOrDefault()));
        break;
      case DATA_SELECTORS:
        List<DataUriNode> dataUris;
        try {
          dataUris = Casts.castToNewList(attribute.getValueOrDefault(), String.class)
            .stream()
            .map(tabular::createDataUri)
            .collect(toList());
        } catch (CastException e) {
          throw new IllegalArgumentException("The argument (" + key + ") for the step (" + this + ") has a value (" + value + ") that is not valid. Error: " + e.getMessage(), e);
        }
        this.setDataSelectors(dataUris);
        break;
      case WITH_DEPENDENCIES:
        this.withDependencies = attribute.getValueOrDefaultCastAsSafe(Boolean.class);
        break;
      case STRICT_SELECTION:
        this.setStrictSelection(attribute.getValueOrDefaultCastAsSafe(Boolean.class));
        break;
      case LOGICAL_NAME:
        this.setLogicalName(attribute.getValueOrDefaultAsStringNotNull());
        break;
      case ORDER:
        try {
          this.setOrder(attribute.getValueOrDefaultCastAs(SelectPipelineStepArgumentOrder.class));
        } catch (CastException e) {
          throw new IllegalArgumentException("The argument (" + key + ") for the step (" + this + ") has a value (" + value + ") that is not valid. Error: " + e.getMessage(), e);
        }
        break;
      case DATA_ATTRIBUTES:
        Map<KeyNormalizer, Object> dataDef;
        try {
          dataDef = Casts.castToNewMap(value, KeyNormalizer.class, Object.class);
        } catch (CastException e) {
          throw new IllegalArgumentException("The data definition attribute of the step " + this + " is not valid. Error: " + e.getMessage(), e);
        }
        this.setDataDef(dataDef);
        break;
      case PROCESSING_TYPE:
        try {
          this.setProcessingType(attribute.getValueOrDefaultCastAs(PipelineStepProcessingType.class));
        } catch (CastException e) {
          throw new IllegalArgumentException("The argument (" + key + ") for the step (" + this + ") has a value (" + value + ") that is not valid. Error: " + e.getMessage(), e);
        }
        break;
      default:
        throw new InternalException("The argument (" + key + ") for the step (" + this + ") should have a branch in the switch");
    }

    return this;

  }


  public SelectPipelineStep setOrder(SelectPipelineStepArgumentOrder order) {

    this.order = order;
    return this;

  }


  public List<DataPath> produceDataPath() {

    /**
     * Deep Nested Data Uri support
     * Example:
     * tabul data print ((archive/world-sql--archive-entry.yml@howto)@tmp)@mysql
     * <p>
     * How? Data Selectors may be just data uri locator
     * We don't support search with nested data uri,
     * but we may return them if they have no pattern
     */
    if (!this.isPatternSelection() && !withDependencies) {
      List<DataPath> dataPaths = new ArrayList<>();
      for (DataUriNode dataUri : dataSelectors) {
        DataPath dataPath = this.getTabular().getDataPath(dataUri, mediaType);
        if (!Tabulars.exists(dataPath)) {
          if (isStrict) {
            throw new SelectionStrictException("The data path (" + dataPath + ") does not exist");
          }
          continue;
        }
        /**
         * Apply the data definition if any
         */
        if (inlineDataAttributes != null) {
          dataPath.mergeDataDefinitionFromYamlMap(inlineDataAttributes);
        }
        dataPaths.add(dataPath);
      }
      return dataPaths;
    }

    /**
     * Pattern selection
     */
    List<DataPath> selectedDataPathList = this.getTabular().select(dataSelectors, isStrict, mediaType);

    /**
     * Have we selected something ?
     *
     */
    if (selectedDataPathList.isEmpty()) {

      return selectedDataPathList;

    }

    /**
     * Apply the logical name if any
     */
    if (logicalName != null) {
      TemplateString templateString = TemplateString.builder(logicalName)
        .isStrict(this.getPipeline().isStrict())
        .build();
      for (DataPath dataPath : selectedDataPathList) {

        String logicalName = templateString.apply(
          TemplateMetas
            .builder()
            .addInputDataPath(dataPath)
        );
        dataPath.setLogicalName(logicalName);

      }
    }

    /**
     * Apply the data definition if any
     */
    if (inlineDataAttributes != null) {
      for (DataPath dataPath : selectedDataPathList) {
        dataPath.mergeDataDefinitionFromYamlMap(inlineDataAttributes);
      }
    }

    /**
     * By connection, the data sets are not dependent
     */
    List<DataPath> dataPathsToReturned = new ArrayList<>();
    Map<Connection, List<DataPath>> mapDataPathsByConnection = selectedDataPathList
      .stream()
      .collect(
        groupingBy(
          DataPath::getConnection,
          mapping(e -> e, toList()))
      );
    for (List<DataPath> dataPaths : mapDataPathsByConnection.values()) {

      /**
       * Do we need a dag?
       * No dag by default please as we need to get the metadata
       * and if there is some view with bad query, it just fails
       * Dag only when dependencies are asked
       */
      boolean noDagSort = !withDependencies && order == SelectPipelineStepArgumentOrder.NATURAL;
      if (noDagSort) {
        return dataPaths
          .stream()
          .sorted()
          .collect(toList());
      }

      /**
       * We create 2 sets of data resources
       * We split them between data path with dependencies
       * and without
       * Why? I don't remember. May be to send down the stream independent data set?
       * This class does not care about order
       * Each step sort in function of its function, For instance, the drop step sorts in drop order,
       */
      Dag<DataPath> dag = ForeignKeyDag.createFromPaths(dataPaths);
      dag.setWithDependency(withDependencies);
      List<DataPath> orderedDataPaths;
      switch (order) {
        case CREATE:
          orderedDataPaths = dag.getCreateOrdered();
          break;
        case DROP:
          orderedDataPaths = dag.getDropOrdered();
          break;
        case NATURAL:
          orderedDataPaths = dag.getCreateOrdered()
            .stream()
            .sorted((s1, s2) -> Sorts.naturalSortComparator(s1.getName(), s2.getName()))
            .collect(toList());
          break;
        default:
          throw new InternalException("The order (" + order + ") was not implemented");
      }
      dataPathsToReturned.addAll(orderedDataPaths);
    }
    return dataPathsToReturned;
  }


  /**
   * @return if this selection is a pattern selection (ie the selector contains glob pattern)
   */
  private boolean isPatternSelection() {
    for (DataUriNode dataSelector : dataSelectors) {
      if (dataSelector.isGlobPattern()) {
        return true;
      }
    }
    return false;
  }

  public PipelineStepProcessingType getProcessingType() {
    return this.processingType;
  }

}
