package com.tabulify.flow.step;


import com.tabulify.Tabular;
import com.tabulify.flow.engine.FilterRunnable;
import com.tabulify.flow.engine.FilterStepAbs;
import com.tabulify.flow.engine.OperationStep;
import com.tabulify.spi.DataPath;

import java.sql.Types;
import java.util.HashSet;
import java.util.Set;
import java.util.function.*;
import java.util.stream.Collector;

/**
 * A state object for collecting statistics such as:
 * * count,
 * * size (min, max, sum, and average).
 *
 * <p>This class is designed to work with (though does not require)
 * {@linkplain java.util.stream streams}. For example, you can compute
 * summary statistics on a stream of DataPath with:
 * <pre> {@code
 * DataPathSummaryStatistics stats = Stream.collect(DataPathSummaryStatistics::new,
 *                                                DataPathSummaryStatistics::accept,
 *                                                DataPathSummaryStatistics::combine);
 * }</pre>
 *
 * <p>{@code DataPathSummaryStatistics} can be used as a
 * {@linkplain java.util.stream.Stream#collect(Collector) reduction}
 * target for a {@linkplain java.util.stream.Stream stream}. For example:
 * <p>
 * <p>
 * <p>
 * <p>
 * Based on {@link java.util.IntSummaryStatistics}
 */
public class SummaryStep extends FilterStepAbs implements Consumer<DataPath>, OperationStep, Collector<DataPath, SummaryStep, DataPath> {


  private long total;
  private long sumSize;
  private Long minSize = Long.MAX_VALUE;
  private Long maxSize = Long.MIN_VALUE;
  private Long sumCount = 0L;
  private Long minCount = Long.MAX_VALUE;
  private Long maxCount = Long.MIN_VALUE;


  /**
   * Construct an empty instance with zero count, zero sum,
   * {@code Integer.MAX_VALUE} min, {@code Integer.MIN_VALUE} max and zero
   * average.
   */
  public static SummaryStep create() {
    return new SummaryStep();
  }

  /**
   * Records a new value into the summary information
   *
   * @param value the input value
   */
  @Override
  public void accept(DataPath value) {

    Long size = value.getSize();
    ++total;

    if (value.getSize() == null) {
      size = 0L;
    }
    sumSize += size;
    minSize = Math.min(minSize, size);
    maxSize = Math.max(maxSize, size);

    Long count = value.getCount();
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
  public void combine(SummaryStep other) {
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
  public Supplier<SummaryStep> supplier() {
    return SummaryStep::new;
  }

  @Override
  public BiConsumer<SummaryStep, DataPath> accumulator() {
    return SummaryStep::accept;
  }

  @Override
  public BinaryOperator<SummaryStep> combiner() {
    return (s, s2) -> {
      s.combine(s2);
      return s;
    };
  }

  @Override
  public Function<SummaryStep, DataPath> finisher() {
    return summaryToDataPath();
  }

  private Function<SummaryStep, DataPath> summaryToDataPath() {
    return summary -> {

      DataPath dataPath = tabular.getMemoryDataStore().getAndCreateRandomDataPath()
        .setLogicalName("data_resource_summary")
        .setDescription("Summary of a list of data resources")
        .getOrCreateRelationDef()
        .addColumn("total", Types.INTEGER)
        .addColumn("sum_count", Types.INTEGER)
        .addColumn("avg_count", Types.INTEGER)
        .addColumn("min_count", Types.NUMERIC)
        .addColumn("max_count", Types.NUMERIC)
        .addColumn("sum_size", Types.NUMERIC)
        .addColumn("avg_size", Types.NUMERIC)
        .addColumn("min_size", Types.NUMERIC)
        .addColumn("max_size", Types.NUMERIC)
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

  @Override
  public String getOperationName() {
    return "summary";
  }

  @Override
  public FilterRunnable createRunnable() {
    return new SummaryFilterRunnable(this);
  }

  @Override
  public SummaryStep setTabular(Tabular tabular) {
    return (SummaryStep) super.setTabular(tabular);
  }




}
