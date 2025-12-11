package com.tabulify.flow.operation;

import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.flow.engine.PipelineStepBuilderTarget;
import com.tabulify.flow.engine.PipelineStepIntermediateMapAbs;
import com.tabulify.model.RelationDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.SelectException;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.exception.CastException;
import com.tabulify.exception.InternalException;
import com.tabulify.type.Casts;
import com.tabulify.type.Enums;
import com.tabulify.type.KeyNormalizer;
import com.tabulify.type.Strings;

import java.util.List;
import java.util.function.Function;

/**
 * Extract the head element of source into target for a size of limit
 */
public class HeadPipelineStep extends PipelineStepIntermediateMapAbs implements Function<DataPath, DataPath> {


  private final HeadPipelineStepBuilder headPipelineStepBuilder;

  public HeadPipelineStep(HeadPipelineStepBuilder headPipelineStepBuilder) {
    super(headPipelineStepBuilder);
    this.headPipelineStepBuilder = headPipelineStepBuilder;
  }


  public static HeadPipelineStepBuilder builder() {
    return new HeadPipelineStepBuilder();
  }

  @Override
  public DataPath apply(DataPath source) {

    if (source.isRuntime()) {
      source = source.execute();
    }

    DataPath target = source.getConnection().getTabular()
      .getMemoryConnection().getDataPath("head_" + source.getLogicalName())
      .setComment("The first " + headPipelineStepBuilder.limit + " rows of the data resource (" + source + "): ");

    target.getOrCreateRelationDef().copyStruct(source);

    // Head
    try (
      SelectStream selectStream = source.getSelectStream();
      InsertStream insertStream = target.getInsertStream()
    ) {
      RelationDef sourceDataDef = selectStream.getRuntimeRelationDef();
      if (sourceDataDef.getColumnsSize() == 0) {
        // No row structure even at runtime
        throw new RuntimeException(Strings.createMultiLineFromStrings(
            "The data path (" + source + ") has no row structure. ",
            "To extract a head, a row structure is needed.",
            "Tip for intern developer: if it's a text file, create a line structure (one row, one cell with one line)")
          .toString());
      }

      int i = 0;
      while (selectStream.next() && i < headPipelineStepBuilder.limit) {
        i++;
        insertStream.insert(selectStream.getObjects());
      }
    } catch (SelectException e) {
      throw new RuntimeException(e);
    }

    return target;

  }


  public static class HeadPipelineStepBuilder extends PipelineStepBuilderTarget {

    static final KeyNormalizer HEAD = KeyNormalizer.createSafe("head");
    /**
     * the number of element returned
     */
    private Integer limit = (Integer) HeadPipelineStepArgument.LIMIT.getDefaultValue();

    @Override
    public HeadPipelineStepBuilder createStepBuilder() {
      return new HeadPipelineStepBuilder();
    }


    public HeadPipelineStepBuilder setLimit(Integer limit) {
      this.limit = limit;
      return this;
    }

    @Override
    public List<Class<? extends ArgumentEnum>> getArgumentEnums() {
      return List.of(HeadPipelineStepArgument.class);
    }

    @Override
    public HeadPipelineStep build() {
      return new HeadPipelineStep(this);
    }

    @Override
    public HeadPipelineStepBuilder setArgument(KeyNormalizer key, Object value) {

      HeadPipelineStepArgument argumentEnum;
      try {
        argumentEnum = Casts.cast(key, HeadPipelineStepArgument.class);
      } catch (CastException e) {
        throw new IllegalArgumentException("The argument (" + key + ") is not a valid argument for the step (" + this + "). You can choose one of: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(HeadPipelineStepArgument.class));
      }
      Attribute attribute;
      try {
        attribute = this.getPipeline().getTabular().getVault()
          .createVariableBuilderFromAttribute(argumentEnum)
          .setOrigin(Origin.PIPELINE)
          .build(value);
        this.setArgument(attribute);
      } catch (CastException e) {
        throw new IllegalArgumentException("The " + argumentEnum + " value (" + value + ") of the step (" + this + ") is not conform . Error: " + e.getMessage(), e);
      }

      switch (argumentEnum) {
        case LIMIT:
          this.setLimit(attribute.getValueOrDefaultCastAsSafe(Integer.class));
          break;
        default:
          throw new InternalException("The argument (" + argumentEnum + ") of the step (" + this + ") was not processed");
      }
      return this;
    }

    @Override
    public KeyNormalizer getOperationName() {
      return HEAD;
    }
  }
}
