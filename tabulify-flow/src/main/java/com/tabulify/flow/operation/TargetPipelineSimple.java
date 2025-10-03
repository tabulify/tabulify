package com.tabulify.flow.operation;

import com.tabulify.Tabular;
import com.tabulify.flow.engine.Pipeline;
import com.tabulify.spi.DataPath;
import com.tabulify.template.TemplateUriFunction;
import com.tabulify.transfer.TransferListener;
import com.tabulify.transfer.TransferPropertiesSystem;
import com.tabulify.uri.DataUriNode;
import net.bytle.type.KeyNormalizer;
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
  DataUriNode sourceDataSelector;
  DataUriNode targetDataUri;
  Boolean withDependency;
  TransferPropertiesSystem.TransferPropertiesSystemBuilder transferPropertiesSystem;
  private Map<KeyNormalizer, ?> sourceAttributes = new HashMap<>();

  private MediaType mediaType = null;

  public TargetPipelineSimple setSourceAttributes(Map<KeyNormalizer, ?> sourceAttributes) {
    this.sourceAttributes = sourceAttributes;
    return this;
  }

  public TargetPipelineSimple setTargetAttributes(Map<KeyNormalizer, ?> targetAttributes) {
    this.targetAttributes = targetAttributes;
    return this;
  }

  private Map<KeyNormalizer, ?> targetAttributes = new HashMap<>();

  public TargetPipelineSimple(Tabular tabular) {
    this.tabular = tabular;
  }

  public static TargetPipelineSimple create(Tabular tabular) {
    return new TargetPipelineSimple(tabular);
  }

  public List<TransferListener> executeTransfer() {

    List<TransferListener> transferListeners = new ArrayList<>();
    Pipeline
      .builder(tabular)
      .setTabular(tabular)
      .addStep(
        SelectPipelineStep
          .builder()
          .setDataSelector(sourceDataSelector)
          .setWithDependencies(withDependency)
          .setDataDef(sourceAttributes)
          .setMediaType(mediaType)
      )
      .addStep(
        TransferPipelineStep
          .builder()
          .setTransferPropertiesSystem(transferPropertiesSystem)
          .injectTransferListenersReference(transferListeners)
          .setTargetDataUri(targetDataUri)
      )
      .build()
      .execute();

    return transferListeners;

  }

  public TargetPipelineSimple setTargetUri(DataUriNode targetDataUri) {
    this.targetDataUri = targetDataUri;
    return this;
  }

  public TargetPipelineSimple setDataSelector(DataUriNode dataSelector) {
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

    TemplateUriFunction targetDataUriFunction = TemplateUriFunction
      .builder(this.tabular)
      .setTargetUri(targetDataUri)
      .setTargetDataDef(targetAttributes)
      .build();

    List<DataPath> downStreamDataPaths = Pipeline.builder(tabular)
      .addStep(SelectPipelineStep
        .builder()
        .setDataSelector(sourceDataSelector)
        .setWithDependencies(withDependency)
        .setDataDef(sourceAttributes)
        .setMediaType(mediaType)

      ).build()
      .execute()
      .getDownStreamDataPaths();

    return downStreamDataPaths.stream()
      .collect(
        Collectors.toMap(
          s -> s,
          targetDataUriFunction
        ));
  }

  public TargetPipelineSimple createTargets() {
    Pipeline
      .builder(tabular)
      .addStep(
        SelectPipelineStep.builder()
          .setDataSelector(this.sourceDataSelector)
          .setWithDependencies(withDependency)
      )
      .addStep(
        CreatePipelineStep
          .builder()
          .setTargetDataUri(this.targetDataUri)
      )
      .build()
      .execute();
    return this;
  }

  public TargetPipelineSimple setTransferProperties(TransferPropertiesSystem.TransferPropertiesSystemBuilder transferPropertiesSystem) {
    this.transferPropertiesSystem = transferPropertiesSystem;
    return this;
  }


  public TargetPipelineSimple setMediaType(MediaType mediaType) {
    this.mediaType = mediaType;
    return this;
  }
}
