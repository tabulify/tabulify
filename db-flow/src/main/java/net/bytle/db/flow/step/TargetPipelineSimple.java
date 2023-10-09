package net.bytle.db.flow.step;

import net.bytle.db.Tabular;
import net.bytle.db.flow.engine.Pipeline;
import net.bytle.db.flow.stream.DataPathStream;
import net.bytle.db.spi.DataPath;
import net.bytle.db.transfer.TransferListener;
import net.bytle.db.transfer.TransferProperties;
import net.bytle.db.uri.DataUri;
import net.bytle.type.MediaType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A wrapper around a flow that executes the most common operation such as:
 * * transfer data resources
 * * returns the source/target map
 * * create the target
 */
public class TargetPipelineSimple {

  Tabular tabular;
  DataUri sourceDataSelector;
  DataUri targetDataUri;
  Boolean withDependency;
  TransferProperties transferProperties = TransferProperties.create();
  private Map<String, ?> sourceAttributes = new HashMap<>();
  private Map<String, String> virtualColumns = new HashMap<>();
  private MediaType mediaType = null;

  public TargetPipelineSimple setSourceAttributes(Map<String, ?> sourceAttributes) {
    this.sourceAttributes = sourceAttributes;
    return this;
  }

  public TargetPipelineSimple setTargetAttributes(Map<String, ?> targetAttributes) {
    this.targetAttributes = targetAttributes;
    return this;
  }

  private Map<String, ?> targetAttributes = new HashMap<>();

  public TargetPipelineSimple(Tabular tabular) {
    this.tabular = tabular;
  }

  public static TargetPipelineSimple create(Tabular tabular) {
    return new TargetPipelineSimple(tabular);
  }

  public List<TransferListener> executeTransfer() {

    List<TransferListener> transferListeners = new ArrayList<>();
    Pipeline
      .createFrom(
        tabular
      )
      .addStepToGraph(
        SelectSupplier
          .create()
          .setDataSelector(sourceDataSelector)
          .setWithDependencies(withDependency)
          .setAttributes(sourceAttributes)
          .setMediaType(mediaType)
      )
      .addStepToGraph(
        TransferStep
          .create()
          .setTransferProperties(transferProperties)
          .injectTransferListenersReference(transferListeners)
          .setTargetUri(targetDataUri)
      )
      .execute();

    return transferListeners;

  }

  public TargetPipelineSimple setTargetUri(DataUri targetDataUri) {
    this.targetDataUri = targetDataUri;
    return this;
  }

  public TargetPipelineSimple setDataSelector(DataUri dataSelector) {
    this.sourceDataSelector = dataSelector;
    return this;
  }

  public TargetPipelineSimple setWithDependencies(Boolean withDependencies) {
    this.withDependency = withDependencies;
    return this;
  }

  /**
   * @return the source target maps
   */
  public Map<? extends DataPath, ? extends DataPath> getSourceTargets() {
    return DataPathStream
      .createFrom(
        SelectSupplier
          .create()
          .setTabular(this.tabular)
          .setDataSelector(sourceDataSelector)
          .setWithDependencies(withDependency)
          .setAttributes(sourceAttributes)
          .setMediaType(mediaType)
      )
      .map(
        EnrichStep.create()
          .addVirtualColumns(virtualColumns)
      )
      .map(
        SourceTargetHelperFunction
          .create(this.tabular)
          .setTargetUri(targetDataUri)
          .setTargetDataDef(targetAttributes)
      )
      .collect(Collectors.toSet())
      .stream()
      .flatMap(e -> e.entrySet().stream())
      .collect(
        Collectors.toMap(
          Map.Entry::getKey,
          Map.Entry::getValue
        ));
  }

  public TargetPipelineSimple createTargets() {
    Pipeline
      .createFrom(
        tabular
      )
      .addStepToGraph(
        SelectSupplier.create()
          .setDataSelector(this.sourceDataSelector)
          .setWithDependencies(withDependency)
      )
      .addStepToGraph(
        CreateTargetFunction
          .create()
          .setTargetUri(this.targetDataUri)
      )
      .execute();
    return this;
  }

  public TargetPipelineSimple setTransferProperties(TransferProperties transferProperties) {
    this.transferProperties = transferProperties;
    return this;
  }

  public TargetPipelineSimple setVirtualColumns(Map<String, String> virtualColumns) {
    this.virtualColumns = virtualColumns;
    return this;
  }

  public TargetPipelineSimple setMediaType(MediaType mediaType) {
    this.mediaType = mediaType;
    return this;
  }
}
