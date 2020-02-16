package net.bytle.db.gen;

import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.TableDef;
import net.bytle.type.Maps;

import java.util.Map;

public class GenColumnDef<T> extends ColumnDef<T> {

  /**
   * The {@link TableDef#getProperty(String)} key giving the data generator data
   */
  public static final String GENERATOR_PROPERTY_KEY = "DataGenerator";
  private final GenDataDef genDataDef;

  /**
   * Only called by the function of of a TableDef
   * To construct a column use TableDef.of
   *
   * @param dataDef
   * @param columnName
   * @param clazz
   */
  public GenColumnDef(GenDataDef dataDef, String columnName, Class clazz) {
    super(dataDef, columnName, clazz);
    this.genDataDef = dataDef;
  }

  @Override
  public GenDataDef getDataDef() {
    return this.genDataDef;
  }

  /**
   * Extract the data generator properties
   *
   * @param columnDef
   * @return - the data generation properties or null
   */
  public static <T> Map<String, Object> getProperties(ColumnDef<T> columnDef) {
    Map<String, Object> properties = columnDef.getProperties();
    final Object generatorProperty = Maps.getPropertyCaseIndependent(properties, GENERATOR_PROPERTY_KEY);
    Map<String, Object> generatorColumnProperties = null;
    if (generatorProperty != null) {
      try {
        generatorColumnProperties = (Map<String, Object>) generatorProperty;
      } catch (ClassCastException e) {
        throw new RuntimeException("The values of the property (" + GENERATOR_PROPERTY_KEY + ") for the column (" + columnDef.toString() + ") should be a map value. Bad values:" + generatorProperty);
      }
    }
    return generatorColumnProperties;
  }

  public static <T> GenColumnDef<T> of(GenDataDef genDataDef, String columnName, Class<T> clazz) {
    assert columnName.length() < 100;
    return new GenColumnDef<T>(genDataDef, columnName, clazz);
  }

  public DataGenerator getGenerator() {
    return null;
  }

}
