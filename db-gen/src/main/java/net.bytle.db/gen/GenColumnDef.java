package net.bytle.db.gen;

import net.bytle.db.gen.generator.CollectionGenerator;
import net.bytle.db.gen.generator.DerivedCollectionGenerator;
import net.bytle.db.gen.generator.SequenceCollectionGenerator;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.TableDef;

import java.util.HashMap;
import java.util.Map;

/**
 * A wrapper/extension around a {@link ColumnDef}
 * that map a {@link ColumnDef} to a {@link CollectionGenerator}
 * @param <T>
 */
public class GenColumnDef<T> extends ColumnDef<T> {

  /**
   * The {@link TableDef#getProperty(String)} key giving the data generator data
   */
  public static final String GENERATOR_PROPERTY_KEY = "DataGenerator";
  private final GenDataDef genDataDef;
  private CollectionGenerator generator;

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
   * Add them if it does not exist
   * @return - the data generation properties or an empty map
   */
  @Override
  public Map<String, Object> getProperties() {
    Object generatorProperty = super.getProperty(GENERATOR_PROPERTY_KEY);
    Map<String, Object> generatorColumnProperties = new HashMap<>();
    if (generatorProperty != null) {
      try {
        generatorColumnProperties = (Map<String, Object>) generatorProperty;
      } catch (ClassCastException e) {
        throw new RuntimeException("The values of the property (" + GENERATOR_PROPERTY_KEY + ") for the column (" + this.toString() + ") should be a map value. Bad values:" + generatorProperty);
      }
    } else {
      super.addProperty(GENERATOR_PROPERTY_KEY,generatorColumnProperties);
    }
    return generatorColumnProperties;
  }

  public static <T> GenColumnDef<T> of(GenDataDef genDataDef, String columnName, Class<T> clazz) {
    assert columnName.length() < 100;
    return new GenColumnDef<T>(genDataDef, columnName, clazz);
  }

  public CollectionGenerator getGenerator() {
    return generator;
  }

  public SequenceCollectionGenerator<T> addSequenceGenerator() {
    SequenceCollectionGenerator<T> sequenceGenerator = SequenceCollectionGenerator.of(this);
    generator = sequenceGenerator;
    return sequenceGenerator;
  }

  public DerivedCollectionGenerator<T> addDerivedGeneratorFrom(String parentColumn, String formula) {

    if (parentColumn.equals(this.getColumnName())){
      throw new RuntimeException("The parent column name ("+parentColumn+") cannot be the name of the column itself ("+this.getColumnName()+")");
    }

    CollectionGenerator parentGenerator = this.getDataDef().getColumnDef(parentColumn).getGenerator();
    if (parentGenerator==null){
      throw new RuntimeException("The parent column ("+parentColumn+") has no collection generator defined");
    }
    final DerivedCollectionGenerator derivedCollectionGenerator = new DerivedCollectionGenerator<>(this, parentGenerator, formula);
    generator = derivedCollectionGenerator;
    return derivedCollectionGenerator;
  }

  /**
   * Add a generator property
   * @param key
   * @param value
   * @return
   */
  @Override
  public ColumnDef addProperty(String key, Object value) {
    Map<String, Object> properties = getProperties();
    properties.put(key,value);
    return this;
  }

  /**
   *
   * @param key - a key
   * @return a generator properties
   */
  @Override
  public Object getProperty(String key) {
    Map<String, Object> properties = getProperties();
    return properties.get(key);
  }

  @Override
  public GenColumnDef precision(Integer precision) {
    super.precision(precision);
    return this;
  }
}
