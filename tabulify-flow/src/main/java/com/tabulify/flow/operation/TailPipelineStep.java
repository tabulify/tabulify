package com.tabulify.flow.operation;

import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.flow.engine.ArgumentEnum;
import com.tabulify.flow.engine.PipelineStep;
import com.tabulify.flow.engine.PipelineStepBuilder;
import com.tabulify.flow.engine.PipelineStepIntermediateMapAbs;
import com.tabulify.model.RelationDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.SelectException;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.type.*;

import java.util.List;

/**
 * Extract the tail element of data resource into target for a size of limit
 * <p>
 * Tail is also used in test to extract
 */
public class TailPipelineStep extends PipelineStepIntermediateMapAbs {


  private final TailPipelineStepBuilder tailBuilder;

  public TailPipelineStep(TailPipelineStepBuilder tailPipelineStepBuilder) {
    super(tailPipelineStepBuilder);
    this.tailBuilder = tailPipelineStepBuilder;
  }

  public static TailPipelineStepBuilder builder() {

    return new TailPipelineStepBuilder();

  }

  @Override
  public DataPath apply(DataPath source) {


    DataPath target = source.getConnection().getTabular().getMemoryConnection().getDataPath("tail_" + source.getLogicalName())
      .setComment("The last " + tailBuilder.limit + " records of the data resource (" + source + "): ");

    target.getOrCreateRelationDef().copyStruct(source);

    // Tail
    TailQueue<List<?>> queue = new TailQueue<>(tailBuilder.limit);

    // Collect it
    try (
      SelectStream selectStream = source.getSelectStream()
    ) {
      RelationDef dataDef = selectStream.getRuntimeRelationDef();
      if (dataDef.getColumnsSize() == 0) {
        // No row structure even at runtime
        throw new RuntimeException(Strings.createMultiLineFromStrings(
            "The data path (" + source + ") has no row structure. ",
            "To extract a tail, a row structure is needed.",
            "Tip for intern developer: if it's a text file, create a line structure (one row, one cell with one line)")
          .toString());
      }

      // Collect the tail
      while (selectStream.next()) {
        queue.add(selectStream.getObjects());
      }
    } catch (SelectException e) {
      throw new RuntimeException(e);
    }

    // Then insert in the target
    try (
      InsertStream insertStream = target.getInsertStream()
    ) {
      queue.forEach(insertStream::insert);
    }


    return target;

  }


  public static class TailPipelineStepBuilder extends PipelineStepBuilder {
    static final KeyNormalizer TAIL = KeyNormalizer.createSafe("tail");
    /**
     * The number of element returned
     */
    private Integer limit = (Integer) TailPipelineStepArgument.LIMIT.getDefaultValue();

    public TailPipelineStepBuilder setLimit(Integer limit) {
      this.limit = limit;
      return this;
    }


    @Override
    public TailPipelineStepBuilder createStepBuilder() {
      return new TailPipelineStepBuilder();
    }

    @Override
    public List<Class<? extends ArgumentEnum>> getArgumentEnums() {
      return List.of(TailPipelineStepArgument.class);
    }

    @Override
    public PipelineStep build() {
      return new TailPipelineStep(this);
    }

    @Override
    public KeyNormalizer getOperationName() {
      return TAIL;
    }

    @Override
    public PipelineStepBuilder setArgument(KeyNormalizer key, Object value) {

      TailPipelineStepArgument argumentEnum;
      try {
        argumentEnum = Casts.cast(key, TailPipelineStepArgument.class);
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
  }
}
