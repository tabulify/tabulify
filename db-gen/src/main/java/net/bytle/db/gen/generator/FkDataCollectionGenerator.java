package net.bytle.db.gen.generator;


import net.bytle.db.gen.GenColumnDef;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.SelectStream;

import java.util.HashMap;
import java.util.Map;


public class FkDataCollectionGenerator<T> implements CollectionGeneratorOnce {


  private final ForeignKeyDef foreignKeyDef;
  private final ColumnDef foreignColumnDef;
  private final GenColumnDef columnDef;

  private Object value;
  private HistogramCollectionGenerator enumCollection;

  /**
   * Get a random foreign value when the {@link #getNewValue()} is called
   */
  public  FkDataCollectionGenerator(GenColumnDef<T> columnDef, ForeignKeyDef foreignKeyDef) {

    this.foreignKeyDef = foreignKeyDef;
    this.columnDef = columnDef;

    // Building the map of value
    foreignColumnDef = foreignKeyDef.getForeignPrimaryKey().getColumns().get(0);
    try (
      SelectStream selectStream = Tabulars.getSelectStream(foreignKeyDef.getForeignPrimaryKey().getDataDef().getDataPath())
    ) {
      Map<Object,Double> histogram = new HashMap<>();
      while (selectStream.next()) {
        histogram.put(selectStream.getObject(foreignColumnDef.getColumnName(), columnDef.getClazz()),1.0);
      }
      if (histogram.size() == 0) {
        throw new RuntimeException("The foreign table (" + foreignColumnDef.getDataDef().getDataPath().toString() + ") has no data for the column (" + foreignKeyDef.getChildColumns().get(0) + ")");
      }
      enumCollection = new HistogramCollectionGenerator(columnDef,histogram);
    }

  }

  /**
   * @return a new generated data object every time it's called
   */
  @Override
  public Object getNewValue() {

    value = enumCollection.getNewValue();
    return value;

  }

  /**
   * @return a generated value (used in case of derived data
   */
  @Override
  public Object getActualValue() {
    return value;
  }

  /**
   * @return the column attached to this generator
   * It permits to create parent relationship between generators
   * when asking a value for a column, we may need to ask the value for another column before
   */
  @Override
  public GenColumnDef getColumn() {

    return columnDef;
  }



  @Override
  public long getMaxGeneratedValues() {

    return Long.MAX_VALUE;

  }


}
