package com.tabulify.gen.generator;


import com.tabulify.gen.DataGenType;
import com.tabulify.stream.SelectStream;
import com.tabulify.exception.CastException;
import com.tabulify.type.Casts;

import java.util.HashSet;
import java.util.Set;

/**
 * Generate (ie return) a value from a {@link com.tabulify.stream.SelectStream}
 * provided at runtime
 * Used by {@link com.tabulify.gen.flow.enrich.EnrichDataPath}
 * It's internal, there is no external documentation
 */
public class DataPathStreamGenerator<T> extends CollectionGeneratorAbs<T> implements CollectionGenerator<T>, java.util.function.Supplier<T> {


  /**
   * The stream from where the data comes from
   */
  private SelectStream selectStream;

  /**
   * @param selectStream - the select stream used to get the value
   */
  public void setSelectStream(SelectStream selectStream) {
    this.selectStream = selectStream;
  }

  public DataPathStreamGenerator(Class<T> clazz) {
    super(clazz);
  }


  /**
   * @return a new generated data object every time it's called
   */
  @Override
  public T getNewValue() {

    return getActualValue();

  }


  @Override
  public T getActualValue() {

    Object object = this.selectStream.getObject(this.getColumnDef().getColumnPosition());
    String columnName = this.getColumnDef().getColumnName();
    try {
      return Casts.cast(object, this.clazz);
    } catch (CastException e) {
      throw new RuntimeException("The value (" + object + ") of the column (" + columnName + ") could not be cast to " + this.clazz.getSimpleName() + ". Error: " + e.getMessage(), e);
    }

  }


  @Override
  public Set<CollectionGenerator<?>> getDependencies() {
    return new HashSet<>();
  }


  @Override
  public long getCount() {
    return Long.MAX_VALUE;
  }

  @Override
  public void reset() {

  }


  @Override
  public DataGenType getGeneratorType() {
    return DataGenType.DATA_PATH_STREAM;
  }

  @Override
  public Boolean isNullable() {
    return true;
  }

}
