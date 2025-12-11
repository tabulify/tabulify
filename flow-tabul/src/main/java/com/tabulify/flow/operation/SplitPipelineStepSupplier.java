package com.tabulify.flow.operation;

import com.tabulify.flow.engine.PipelineStepConsumerSupplierBuilderAbs;
import com.tabulify.flow.engine.PipelineStepIntermediateSupplier;
import com.tabulify.flow.engine.PipelineStepSupplierDataPathAbs;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.SelectException;
import com.tabulify.stream.SelectStream;
import com.tabulify.template.TemplateMetas;
import com.tabulify.template.TemplateString;
import com.tabulify.type.KeyNormalizer;

public class SplitPipelineStepSupplier extends PipelineStepSupplierDataPathAbs {
  private final SplitPipelineStepSupplierBuilder splitBuilder;
  private SelectStream stream;
  private TemplateString templateString;
  private DataPath nextDataPath;

  public SplitPipelineStepSupplier(SplitPipelineStepSupplierBuilder splitSupplierBuilder) {

    super(splitSupplierBuilder);
    this.splitBuilder = splitSupplierBuilder;
  }

  @Override
  public boolean hasNext() {

    if (!stream.next()) {
      return false;
    }
    buildNextDataPath();
    return true;

  }


  private void buildNextDataPath() {

    DataPath inputDatapath = splitBuilder.datapath;

    // Target name
    if (templateString != null) {

      String name = templateString.apply(
        TemplateMetas.builder()
          .addInputDataPath(inputDatapath)
          .addSelectStream(stream)
      );
      nextDataPath = this.getTabular()
        .getMemoryConnection()
        .getDataPath(name);
    } else {
      /**
       * They stay the same name so that if there is any error
       * they are parked in the same file
       */
      nextDataPath = this.getTabular()
        .getMemoryConnection()
        .getDataPath(splitBuilder.datapath.getName());
    }

    // Merge Data Def
    nextDataPath
      .createEmptyRelationDef()
      .getDataPath()
      .mergeDataDefinitionFrom(inputDatapath);

    // Insert data
    nextDataPath.getInsertStream()
      .insert(stream.getObjects())
      .close();
  }


  @Override
  public void onStart() {

    DataPath datapath = splitBuilder.datapath;
    if (datapath == null) {
      return;
    }
    try {
      stream = datapath.getSelectStream();
    } catch (SelectException e) {
      throw new RuntimeException("Unable to get the record stream of  " + datapath, e);
    }
    String targetTemplate = splitBuilder.splitStep.getSplitBuilder().getTargetTemplate();
    if (targetTemplate != null) {
      this.templateString = TemplateString
        .builder(targetTemplate)
        .isStrict(this.getPipeline().isStrict())
        .build();
    }

  }

  @Override
  public void onComplete() {
    stream.close();
  }


  @Override
  public DataPath get() {


    return nextDataPath;

  }

  static public SplitPipelineStepSupplierBuilder builder() {
    return new SplitPipelineStepSupplierBuilder();
  }

  @Override
  public PipelineStepIntermediateSupplier getIntermediateSupplier() {
    return this.splitBuilder.splitStep;
  }


  public static class SplitPipelineStepSupplierBuilder extends PipelineStepConsumerSupplierBuilderAbs {


    public static final KeyNormalizer SPLIT_SUPPLIER = KeyNormalizer.createSafe("split-supplier");
    private DataPath datapath;
    private SplitPipelineStep splitStep;


    @Override
    public SplitPipelineStepSupplier build() {
      // data path may be null (ie last element of the stream)
      assert this.splitStep != null : "split step should not be null";
      return new SplitPipelineStepSupplier(this);
    }

    @Override
    public KeyNormalizer getOperationName() {
      return SPLIT_SUPPLIER;
    }

    public SplitPipelineStepSupplierBuilder setDataPath(DataPath dataPath) {
      this.datapath = dataPath;
      return this;
    }

    public SplitPipelineStepSupplierBuilder setSplitStep(SplitPipelineStep splitPipelineStep) {
      this.splitStep = splitPipelineStep;
      return this;
    }
  }
}
