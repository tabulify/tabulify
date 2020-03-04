package net.bytle.db.gen;

import net.bytle.db.gen.generator.*;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.TableDef;
import net.bytle.type.Arrayss;
import net.bytle.type.Maps;
import net.bytle.type.Typess;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A wrapper/extension around a {@link ColumnDef}
 * that map a {@link ColumnDef} to a {@link CollectionGeneratorOnce}
 *
 * @param <T>
 */
public class GenColumnDef<T> extends ColumnDef<T> {

  /**
   * The {@link TableDef#getProperty(String)} key giving the data generator data
   */
  public static final String GENERATOR_PROPERTY_KEY = "DataGenerator";
  public static final String GENERATORY_TYPE_PROPERTY = "type";
  private final GenDataDef genDataDef;
  private CollectionGenerator<T> generator;

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
   *
   * @return - the data generation properties or an empty map
   */
  @Override
  public Map<String, Object> getProperties() {
    Object generatorProperty = super.getProperty(GENERATOR_PROPERTY_KEY);
    if (generatorProperty == null) {
      generatorProperty = new HashMap<String, Object>();
      super.addProperty(GENERATOR_PROPERTY_KEY, generatorProperty);
    }
    try {
      return (Map<String, Object>) generatorProperty;
    } catch (ClassCastException e) {
      throw new RuntimeException("The values of the property (" + GENERATOR_PROPERTY_KEY + ") for the column (" + this.toString() + ") should be a map value. Bad values:" + generatorProperty);
    }
  }

  public static <T> GenColumnDef<T> of(GenDataDef genDataDef, String columnName, Class<T> clazz) {
    assert columnName.length() < 100;
    return new GenColumnDef<T>(genDataDef, columnName, clazz);
  }

  public CollectionGenerator getGenerator() {

    if (generator == null) {

      // When read from a data definition file into the column property
      final String nameProperty = Typess.safeCast(getProperty(GENERATORY_TYPE_PROPERTY), String.class);
      if (nameProperty == null) {
        return null;
      }

      String name = nameProperty.toLowerCase();
      switch (name) {
        case "sequence":
        case "unique":
          generator = SequenceGenerator.of(this);
          break;
        case DerivedCollectionGenerator.TYPE:
          // Parent Generator
          final String columnParentKeyProperty = "ColumnParent";
          String columnParentName = Typess.safeCast(this.getProperty(columnParentKeyProperty), String.class);
          if (columnParentName == null) {
            throw new IllegalArgumentException("The parent column is not defined in the '" + columnParentKeyProperty + "' properties for the column " + this.getFullyQualifiedName());
          }
          GenColumnDef columnParent = this.getDataDef().getColumnDef(columnParentName);
          if (this.equals(columnParent)) {
            throw new RuntimeException("The column (" + this.getFullyQualifiedName() + " has a derived generator and derived from itself creating a loop. Please choose another column as derived (parent) column.");
          }
          CollectionGenerator parentGenerator = columnParent.getGenerator();
          CollectionGeneratorOnce parentCollectionGenerator;
          if (parentGenerator instanceof CollectionGeneratorOnce) {
            parentCollectionGenerator = (CollectionGeneratorOnce) parentGenerator;
          } else {
            throw new RuntimeException("Derived generator are working only with a scalar generator. The generator (" + parentGenerator + ") generates values for a pair of columns");
          }

          // Formula
          String formula = Typess.safeCast(this.getProperty("formula"), String.class);
          if (formula == null) {
            throw new RuntimeException("The 'formula' property is mandatory to create a derived data generator and is missing for the column (" + this.getFullyQualifiedName() + ")");
          }

          // New Instance
          generator = new DerivedCollectionGenerator<>(this, parentCollectionGenerator, formula);
          break;
        case "random":
        case "distribution":
        case "uniform":
          Object min = this.getProperty("min");
          Object max = this.getProperty("max");
          generator = new UniformCollectionGenerator<>(this, min, max);
          break;
        case HistogramCollectionGenerator.TYPE:
          Map<Object, Double> buckets = (Map<Object, Double>) this.getProperty("buckets");
          parentGenerator = HistogramCollectionGenerator.of(this, buckets);
          break;
        case ProvidedDataGenerator.TYPE:
          Object values = this.getProperty("values");
          generator = new ProvidedDataGenerator(this, values);
          break;
        default:
          throw new RuntimeException("The generator (" + name + ") defined for the column (" + this.toString() + ") is unknown");
      }
    }
    return generator;
  }

  public SequenceGenerator<T> addSequenceGenerator() {
    SequenceGenerator<T> sequenceGenerator = SequenceGenerator.of(this);
    generator = sequenceGenerator;
    return sequenceGenerator;
  }

  public DerivedCollectionGenerator<T> addDerivedGeneratorFrom(String parentColumn, String formula) {

    if (parentColumn.equals(this.getColumnName())) {
      throw new RuntimeException("The parent column name (" + parentColumn + ") cannot be the name of the column itself (" + this.getColumnName() + ")");
    }

    CollectionGenerator parentGenerator = this.getDataDef().getColumnDef(parentColumn).getGenerator();
    if (parentGenerator == null) {
      throw new RuntimeException("The parent column (" + parentColumn + ") has no collection generator defined");
    }
    final DerivedCollectionGenerator derivedCollectionGenerator = new DerivedCollectionGenerator<>(this, parentGenerator, formula);
    generator = derivedCollectionGenerator;
    return derivedCollectionGenerator;
  }

  /**
   * Add a generator property
   *
   * @param key
   * @param value
   * @return
   */
  @Override
  public GenColumnDef addProperty(String key, Object value) {
    if (!key.toLowerCase().equals(GENERATOR_PROPERTY_KEY.toLowerCase())) {
      Map<String, Object> properties = getProperties();
      properties.put(key, value);
    } else {
      super.addProperty(key, value);
    }
    return this;
  }

  /**
   * @param key - a key
   * @return a generator properties
   */
  @Override
  public Object getProperty(String key) {
    Map<String, Object> properties = getProperties();
    // From the yaml parser, we may get an dependent map
    return Maps.getPropertyCaseIndependent(properties,key);
  }

  @Override
  public GenColumnDef precision(Integer precision) {
    super.precision(precision);
    return this;
  }

  public HistogramCollectionGenerator<T> addHistogramGenerator(Map<Object, Double> buckets) {
    HistogramCollectionGenerator<T> histogramCollectionGenerator = HistogramCollectionGenerator.of(this, buckets);
    generator = histogramCollectionGenerator;
    return histogramCollectionGenerator;
  }

  public HistogramCollectionGenerator<T> addHistogramGenerator(Object element, Object... elements) {
    if (element instanceof Map) {
      return addHistogramGenerator((Map<Object, Double>) element);
    } else {
      Map<Object, Double> buckets = Arrays.stream(Arrayss.concat(element, elements))
        .collect(Collectors.toMap(e -> e, e -> 1.0));
      HistogramCollectionGenerator<T> histogramCollectionGenerator = HistogramCollectionGenerator.of(this, buckets);
      generator = histogramCollectionGenerator;
      return histogramCollectionGenerator;
    }
  }


  public SequenceGenerator<T> getSequenceGenerator() {

    if (generator == null) {
      throw new RuntimeException("The column (" + this + ") has no generator");
    }
    if (getGenerator().getClass() != SequenceGenerator.class) {
      throw new RuntimeException("The column (" + this + ") has a generator that is not a sequence generator but " + generator.getClass());
    }
    return (SequenceGenerator<T>) generator;

  }

  /**
   * @param collectionGenerator
   * @return
   */
  public GenColumnDef setGenerator(CollectionGenerator collectionGenerator) {
    generator = collectionGenerator;
    return this;
  }

  public NameGenerator addNameGenerator() {
    NameGenerator nameGenerator = new NameGenerator(this);
    generator = nameGenerator;
    return nameGenerator;
  }

  public UniformCollectionGenerator<T> addUniformDistributionGenerator() {
    UniformCollectionGenerator uniformCollectionGenerator = UniformCollectionGenerator.of(this);
    generator = uniformCollectionGenerator;
    return uniformCollectionGenerator;
  }

  public UniformCollectionGenerator<T> addUniformDistributionGenerator(T min, T max) {
    UniformCollectionGenerator<T> uniformCollectionGenerator = new UniformCollectionGenerator<>(this, min, max);
    generator = uniformCollectionGenerator;
    return uniformCollectionGenerator;
  }

  public ProvidedDataGenerator<T> addPredefinedDataGenerator(T... values) {
    ProvidedDataGenerator<T> providedDataGenerator = new ProvidedDataGenerator<>(this, values);
    generator = providedDataGenerator;
    return providedDataGenerator;
  }
}
