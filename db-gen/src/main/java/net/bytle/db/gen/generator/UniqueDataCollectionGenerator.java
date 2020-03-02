package net.bytle.db.gen.generator;


import net.bytle.db.engine.Columns;
import net.bytle.db.gen.GenColumnDef;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.SqlDataType;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;


public class UniqueDataCollectionGenerator implements CollectionGeneratorMultiple {


  private Map<GenColumnDef, CollectionGeneratorOnce> dataGeneratorMap = new HashMap<>();

  Integer position = new Integer(0);

  /**
   *
   *
   * @param columnDefs
   */
  public UniqueDataCollectionGenerator(List<GenColumnDef> columnDefs) {

    // long numberOfValueToGenerateByColumn = Math.floorDiv((long) numberOfRowToInsert,(long) columnDefs.size());

    // Creating a data generator by column
    // and adding it to the data generator map variable
    for (GenColumnDef columnDef : columnDefs) {

      if (SqlDataType.timeTypes.contains(columnDef.getDataType().getTypeCode())) {

        // With date, we are going in the past
        GenColumnDef<Date> dateColumn = (GenColumnDef<Date>) Columns.safeCast(columnDef, Date.class);
        Date minDate = getMinSafely(dateColumn, Date.valueOf(LocalDate.now()));
        dataGeneratorMap.put(columnDef, SequenceGenerator.of(dateColumn).start(minDate).step(-1));

      } else if (SqlDataType.numericTypes.contains(columnDef.getDataType().getTypeCode())) {

        if (columnDef.getClazz() == BigDecimal.class) {
          GenColumnDef<BigDecimal> bigDecimalColumnDef = (GenColumnDef<BigDecimal>) Columns.safeCast(columnDef, BigDecimal.class);
          BigDecimal intCounter = getMaxSafely(bigDecimalColumnDef,BigDecimal.ZERO);
          dataGeneratorMap.put(columnDef, SequenceGenerator.of(bigDecimalColumnDef).start(intCounter).step(1));
        } else {
          GenColumnDef<Integer> integerColumn = (GenColumnDef<Integer>) Columns.safeCast(columnDef, Integer.class);
          Integer intCounter = getMaxSafely(integerColumn,0);
          dataGeneratorMap.put(columnDef, SequenceGenerator.of(integerColumn).start(intCounter).step(1));
        }

      } else if (SqlDataType.characterTypes.contains(columnDef.getDataType().getTypeCode())) {

        GenColumnDef<String> stringColumn = (GenColumnDef<String>) Columns.safeCast(columnDef, String.class);
        String s = getMaxSafely(stringColumn,"");
        dataGeneratorMap.put(columnDef, SequenceGenerator.of(stringColumn).start(s));

      } else {

        throw new RuntimeException("The data type (" + columnDef.getDataType().getTypeCode() + "," + columnDef.getDataType().getTypeNames() + ") is not yet implemented for the column " + columnDef.getFullyQualifiedName() + ").");

      }

    }


  }

  /**
   *
   * @param columnDef
   * @param otherwise
   * @param <T>
   * @return the min or otherwise if the processing engine does not support it
   */
  private <T> T getMinSafely(ColumnDef<T> columnDef, T otherwise) {
    try {
      return Columns.getMin(columnDef);
    } catch (RuntimeException e) {
      // java.lang.RuntimeException: A processing engine is not yet supported
      return otherwise;
    }
  }

  /**
   * Processing engine may be not supported everywhere
   * @param columnDef
   * @param otherwise
   * @param <T>
   * @return the max or otherwise
   */
  private <T> T getMaxSafely(ColumnDef<T> columnDef, T otherwise) {
    try {
      return Columns.getMax(columnDef);
    } catch (RuntimeException e) {
      // java.lang.RuntimeException: A processing engine is not yet supported
      return otherwise;
    }

  }





  /**
   * Get a new value for a column
   * <p>
   * This generator is row based. You need to call each column
   * in order to have a unique set
   *
   * @param columnDef
   * @return a new generated data object every time it's called
   */
  @Override
  public Object getNewValue(ColumnDef columnDef) {


    final CollectionGeneratorOnce dataCollectionGenerator = dataGeneratorMap.get(columnDef);
    return dataCollectionGenerator.getNewValue();

  }

  /**
   * of the actual value of a column
   *
   * @param columnDef
   * @return a generated value (used in case of derived data
   */
  @Override
  public Object getActualValue(ColumnDef columnDef) {

    return dataGeneratorMap.get(columnDef).getActualValue();
  }

  /**
   * @return the columns attached to this generator
   */
  @Override
  public List<ColumnDef> getColumns() {
    List<ColumnDef> columnDefs = new ArrayList<>(dataGeneratorMap.keySet());
    Collections.sort(columnDefs);
    return columnDefs;
  }

  @Override
  public long getMaxGeneratedValues() {
    // Hack
    return Long.MAX_VALUE;
  }


  @Override
  public String toString() {
    return "UniqueDataGenerator{" + dataGeneratorMap + '}';
  }
}
