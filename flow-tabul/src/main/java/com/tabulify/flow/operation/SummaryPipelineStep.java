package com.tabulify.flow.operation;

import com.tabulify.flow.engine.PipelineStepBuilder;
import com.tabulify.flow.engine.PipelineStepBuilderTarget;
import com.tabulify.flow.engine.PipelineStepIntermediateManyToManyAbs;
import com.tabulify.flow.engine.PipelineStepSupplierDataPath;
import com.tabulify.model.SqlDataTypeAnsi;
import com.tabulify.spi.DataPath;
import com.tabulify.type.KeyNormalizer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * A state object for collecting statistics such as:
 * * count,
 * * size (min, max, sum, and average).
 *
 *
 *
 * <p>{@code DataPathSummaryStatistics} can be used as a
 * {@linkplain java.util.stream.Stream#collect(Collector) reduction}
 * target for a {@linkplain java.util.stream.Stream stream}. For example:
 * <p>
 * <p>
 * <p>
 * Based on {@link java.util.IntSummaryStatistics}
 *
 * <p>This class was designed to work with (though does not require)
 * {@linkplain java.util.stream streams}.
 * For example, you can compute
 * summary statistics on a stream of DataPath with:
 * <pre> {@code
 * DataPathSummaryStatistics stats = Stream.collect(DataPathSummaryStatistics::new,
 *                                                DataPathSummaryStatistics::accept,
 *                                                DataPathSummaryStatistics::combine);
 * }</pre>
 */
public class SummaryPipelineStep extends PipelineStepIntermediateManyToManyAbs implements Collector<DataPath, SummaryPipelineStep, DataPath> {


  public static final String TOTAL_COLUMN_NAME = "total";
  public static final String SUM_COUNT_COLUMN_NAME = "sum_count";
  private List<DataPath> acceptedDataPaths = new ArrayList<>();


  public SummaryPipelineStep(SummaryPipelineStepBuilder summarySupplierBuilder) {

    super(summarySupplierBuilder);
    reset();

  }

  /**
   * The initial state is at {@link #reset}
   */
  private long total;
  private long sumSize;
  private Long minSize;
  private Long maxSize;
  private Long sumCount;
  private Long minCount;
  private Long maxCount;


  /**
   * Records a new value into the summary information
   *
   * @param dataPath the input value
   */
  @Override
  public void accept(DataPath dataPath) {

    this.acceptedDataPaths.add(dataPath);

    Long size = dataPath.getSize();
    ++total;

    if (dataPath.getSize() == null) {
      size = 0L;
    }
    sumSize += size;
    minSize = Math.min(minSize, size);
    maxSize = Math.max(maxSize, size);

    Long count = dataPath.getCount();
    sumCount += count;
    minCount = Math.min(minCount, count);
    maxCount = Math.max(maxCount, count);

  }

  /**
   * Combines the state of another {@code DataPathSummaryStatistics} into this one.
   *
   * @param other another {@code DataPathSummaryStatistics}
   * @throws NullPointerException if {@code other} is null
   */
  public void combine(SummaryPipelineStep other) {
    total += other.total;
    sumSize += other.sumSize;
    minSize = Math.min(minSize, other.minSize);
    maxSize = Math.max(maxSize, other.maxSize);
    sumCount += other.sumCount;
    minCount = Math.min(minCount, other.minCount);
    maxCount = Math.max(maxCount, other.maxCount);
  }

  /**
   * Returns the count of values recorded.
   *
   * @return the count of values
   */
  public final Long getTotal() {
    return total;
  }

  /**
   * Returns the sum of values recorded, or zero if no values have been
   * recorded.
   *
   * @return the sum of values, or zero if none
   */
  public final Long getSumSize() {
    return sumSize;
  }

  /**
   * Returns the minimum value recorded, or {@code Integer.MAX_VALUE} if no
   * values have been recorded.
   *
   * @return the minimum size value, or null if none
   */
  public final Long getMinSize() {
    return minSize;
  }

  /**
   * Returns the maximum size value recorded, or null if no
   * values have been recorded.
   *
   * @return the maximum value, or null if none
   */
  public final Long getMaxSize() {
    return maxSize;
  }

  /**
   * Returns the arithmetic mean of values recorded, or zero if no values have been
   * recorded.
   *
   * @return the arithmetic mean of values, or zero if none
   */
  public final Double getAverageSize() {
    return getTotal() > 0 ? (double) getSumSize() / getTotal() : 0.0d;
  }

  public final Double getAverageCount() {
    return getSumCount() > 0 ? (double) getSumCount() / getTotal() : 0.0d;
  }

  @Override
  /**
   * {@inheritDoc}
   *
   * Returns a non-empty string representation of this object suitable for
   * debugging. The exact presentation format is unspecified and may vary
   * between implementations and versions.
   */
  public String toString() {
    return String.format(
      "%s{count=%d, sum=%d, min=%d, average=%f, max=%d}",
      this.getClass().getSimpleName(),
      getTotal(),
      getSumSize(),
      getMinSize(),
      getAverageSize(),
      getMaxSize());
  }

  @Override
  public Supplier<SummaryPipelineStep> supplier() {
    return () -> SummaryPipelineStep.builder().build();
  }

  @Override
  public BiConsumer<SummaryPipelineStep, DataPath> accumulator() {
    return SummaryPipelineStep::accept;
  }

  @Override
  public BinaryOperator<SummaryPipelineStep> combiner() {
    return (s, s2) -> {
      s.combine(s2);
      return s;
    };
  }

  @Override
  public Function<SummaryPipelineStep, DataPath> finisher() {
    return summaryToDataPath();
  }

  private Function<SummaryPipelineStep, DataPath> summaryToDataPath() {
    return summary -> {

      DataPath dataPath = this.getTabular().getMemoryConnection().getAndCreateRandomDataPath()
        .setLogicalName("data_resource_summary")
        .setComment("Summary of a list of data resources")
        .getOrCreateRelationDef()
        .addColumn(TOTAL_COLUMN_NAME, SqlDataTypeAnsi.INTEGER)
        .addColumn(SUM_COUNT_COLUMN_NAME, SqlDataTypeAnsi.INTEGER)
        .addColumn("avg_count", SqlDataTypeAnsi.INTEGER)
        .addColumn("min_count", SqlDataTypeAnsi.NUMERIC)
        .addColumn("max_count", SqlDataTypeAnsi.NUMERIC)
        .addColumn("sum_size", SqlDataTypeAnsi.NUMERIC)
        .addColumn("avg_size", SqlDataTypeAnsi.NUMERIC)
        .addColumn("min_size", SqlDataTypeAnsi.NUMERIC)
        .addColumn("max_size", SqlDataTypeAnsi.NUMERIC)
        .getDataPath();

      dataPath
        .getInsertStream()
        .insert(
          summary.getTotal(),
          summary.getSumCount(),
          summary.getAverageCount(),
          summary.getMinCount(),
          summary.getMaxCount(),
          summary.getSumSize(),
          summary.getAverageSize(),
          summary.getMinSize(),
          summary.getMaxSize()
        )
        .close();
      return dataPath;

    };
  }

  private Long getMinCount() {
    return minCount;
  }

  private Long getMaxCount() {
    return maxCount;
  }

  public Long getSumCount() {
    return sumCount;
  }

  @Override
  public Set<Characteristics> characteristics() {
    return new HashSet<>();
  }

  static public SummaryPipelineStepBuilder builder() {
    return new SummaryPipelineStepBuilder();
  }

  @Override
  public PipelineStepSupplierDataPath get() {
    DataPath dataPath = summaryToDataPath().apply(this);
    return (PipelineStepSupplierDataPath) DefinePipelineStep.builder()
      .addDataPath(dataPath)
      .setIntermediateSupplier(this)
      .build();
  }

  @Override
  public void reset() {
    acceptedDataPaths = new ArrayList<>();
    total = 0;
    sumSize = 0;
    minSize = Long.MAX_VALUE;
    maxSize = Long.MIN_VALUE;
    sumCount = 0L;
    minCount = Long.MAX_VALUE;
    maxCount = Long.MIN_VALUE;
  }

  @Override
  public List<DataPath> getDataPathsBuffer() {
    return acceptedDataPaths;
  }

  public static class SummaryPipelineStepBuilder extends PipelineStepBuilderTarget {

    static final private KeyNormalizer SUMMARY = KeyNormalizer.createSafe("summary");

    @Override
    public PipelineStepBuilder createStepBuilder() {
      return new SummaryPipelineStepBuilder();
    }

    public SummaryPipelineStep build() {
      return new SummaryPipelineStep(this);
    }

    @Override
    public KeyNormalizer getOperationName() {
      return SUMMARY;
    }

  }
}
