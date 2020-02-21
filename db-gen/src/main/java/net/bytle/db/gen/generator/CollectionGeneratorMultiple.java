package net.bytle.db.gen.generator;

import net.bytle.db.model.ColumnDef;

import java.util.List;

/**
 * When a generator generate data for multiple columns at once
 * @param <T>
 */
public interface CollectionGeneratorMultiple<T> extends CollectionGenerator {

  /**
   * of the actual value of a column
   *
   * @return a generated value (used in case of derived data
   */
  T getActualValue(ColumnDef columnDef);



  /**
   * of a new value for a column
   *
   * @return a new generated data object every time it's called
   */
  <T> T getNewValue(ColumnDef columnDef);

  /**
   *
   * @return the columns
   */
  public List<ColumnDef> getColumns();

}
