package com.tabulify.gen;

import com.tabulify.Tabular;
import com.tabulify.csv.CsvDataPath;
import com.tabulify.gen.generator.*;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.ColumnDefBase;
import com.tabulify.model.RelationDef;
import com.tabulify.model.SqlDataType;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataPathAttribute;
import net.bytle.exception.CastException;
import net.bytle.exception.NoColumnException;
import net.bytle.exception.NoValueException;
import net.bytle.type.Arrayss;
import net.bytle.type.Casts;
import net.bytle.type.Enums;
import net.bytle.type.KeyNormalizer;

import java.util.*;
import java.util.stream.Collectors;

import static com.tabulify.gen.GenColumnAttribute.HIDDEN;

/**
 * A wrapper/extension around a {@link ColumnDefBase}
 * that map a {@link ColumnDefBase} to a {@link CollectionGenerator}
 */
public class GenColumnDef<T> extends ColumnDefBase<T> implements ColumnDef<T> {


  private final RelationDef relationDataDef;
  private CollectionGenerator<T> generator;
  // Attribute value may be string, map or list
  private Map<DataSupplierAttribute, Object> dataSupplierAttributeValueMap;


  /**
   * Use {@link #createOf(RelationDef, String, SqlDataType)}
   *
   * @param relationDef - the relation - normally a {@link GenRelationDef} but it may also a normal relationDef from the {@link com.tabulify.gen.flow.enrich.EnrichDataPath}
   * @param columnName  the column name
   * @param sqlDataType the data type
   */
  private GenColumnDef(RelationDef relationDef, String columnName, SqlDataType<T> sqlDataType) {
    super(relationDef, columnName, sqlDataType);
    this.relationDataDef = relationDef;
    this.addVariablesFromEnumAttributeClass(GenColumnAttribute.class);
  }

  @Override
  public GenRelationDef getRelationDef() {
    return (GenRelationDef) this.relationDataDef;
  }

  public GenRelationDef getGenRelationDef() {
    if (!(this.relationDataDef instanceof GenRelationDef)) {
      throw new IllegalStateException("The relation def (" + this.relationDataDef + ") is not a generator relation def");
    }
    return (GenRelationDef) this.relationDataDef;
  }

  public static <T> GenColumnDef<T> createOf(RelationDef relationDataDef, String columnName, SqlDataType<T> sqlDataType) {
    assert columnName.length() < 100;
    return new GenColumnDef<>(relationDataDef, columnName, sqlDataType);
  }


  public CollectionGenerator<T> getOrCreateGenerator() {

    if (generator != null) {
      return (generator)
        .setColumnDef(this);
    }
    return createGenerator();

  }

  public SequenceGenerator<T> addSequenceGenerator() {

    SequenceGenerator<T> sequenceGenerator = (SequenceGenerator<T>) SequenceGenerator.create(this.getClazz())
      .setColumnDef(this);
    generator = sequenceGenerator;
    return sequenceGenerator;

  }


  public ExpressionGenerator<T> addExpressionGenerator(String expression, String... parentColumns) {

    /**
     * Does the parent columns contains the name of this column ?
     *
     */
    String foundCol = Arrays.stream(parentColumns)
      .filter(c -> c.equals(this.getColumnName()))
      .findFirst()
      .orElse(null);
    if (foundCol != null) {
      throw new RuntimeException("The parent columns name (" + Arrayss.toJoinedStringWithComma(parentColumns) + ") cannot contain the name of the column itself (" + this.getColumnName() + ")");
    }

    List<CollectionGenerator<?>> parentGenerators = new ArrayList<>();
    for (String parentColumn : parentColumns) {
      GenColumnDef<?> columnDef;
      try {
        columnDef = this.getRelationDef().getColumnDef(parentColumn);
      } catch (NoColumnException e) {
        throw new RuntimeException("The parent column (" + parentColumn + ") does not exist in the data resources (" + this.getRelationDef().getDataPath() + ").");
      }
      if (columnDef != null) {
        CollectionGenerator<?> parentGenerator = columnDef.getOrCreateGenerator();
        if (parentGenerator == null) {
          throw new RuntimeException("The parent column (" + parentColumn + ") has no collection generator defined");
        } else {
          parentGenerators.add(parentGenerator);
        }
      }
    }
    ExpressionGenerator<T> expressionGenerator = (ExpressionGenerator<T>) (new ExpressionGenerator<>(this.getClazz(), expression, parentGenerators))
      .setColumnDef(this);


    generator = expressionGenerator;
    return expressionGenerator;
  }


  @Override
  public GenColumnDef<T> setPrecision(int precision) {
    super.setPrecision(precision);
    return this;
  }

  public HistogramGenerator<T> addHistogramGenerator(Map<T, Double> buckets) {

    HistogramGenerator<T> histogramGenerator = (HistogramGenerator<T>) HistogramGenerator.create(this.getClazz(), buckets)
      .setColumnDef(this);
    generator = histogramGenerator;
    return histogramGenerator;
  }

  @SafeVarargs
  public final HistogramGenerator<T> addHistogramGenerator(T element, T... elements) {


    if (element instanceof Map) {
      Map<T, Double> castedElement;
      try {
        castedElement = Casts.castToSameMap(element, this.getClazz(), Double.class);
      } catch (CastException e) {
        throw new RuntimeException("An histogram element is not a double", e);
      }
      return addHistogramGenerator(castedElement);
    } else {
      Map<T, Double> buckets = Arrays.stream(Arrayss.concat(element, elements))
        .collect(Collectors.toMap(e -> e, e -> 1.0));
      HistogramGenerator<T> histogramGenerator = (HistogramGenerator<T>) HistogramGenerator.create(this.getClazz(), buckets)
        .setColumnDef(this);
      generator = histogramGenerator;
      return histogramGenerator;
    }
  }


  /**
   * @param collectionGenerator the collection generator
   * @return the column def
   */
  public GenColumnDef<T> setGenerator(CollectionGenerator<T> collectionGenerator) {
    generator = collectionGenerator;
    return this;
  }

  public DataSetGenerator<T> addDataSetEntityGenerator(String name) {
    return addDataSetEntityGenerator(name, name);
  }


  public DataSetGenerator<T> addDataSetEntityGenerator(String entityName, String entityColumnName) {

    Tabular tabular = this.relationDataDef.getDataPath().getConnection().getTabular();
    CsvDataPath csvDataPath = DataSetGenerator.getEntityPath(tabular, entityName, null);
    DataSetGenerator<T> dataSetGenerator = (DataSetGenerator<T>) DataSetGenerator.create(this.getClazz(), csvDataPath, entityColumnName)
      .setColumnDef(this);
    this.generator = dataSetGenerator;
    return dataSetGenerator;

  }

  public RandomGenerator<T> addRandomGenerator() {

    RandomGenerator<T> randomGenerator = (RandomGenerator<T>) RandomGenerator
      .of(this.getClazz())
      .setColumnDef(this);
    generator = randomGenerator;
    return randomGenerator;
  }

  public RandomGenerator<T> addRandomGenerator(T min, T max) {
    RandomGenerator<T> randomGenerator = (RandomGenerator<T>) (new RandomGenerator<>(this.getClazz(), min, max))
      .setColumnDef(this);
    generator = randomGenerator;
    return randomGenerator;
  }


  public DataSetMetaColumnGenerator<T> addDataSetColumnGenerator(String parentColumnName, String columnName) {

    GenColumnDef<?> parentColumnDef;
    try {
      parentColumnDef = this.getRelationDef().getColumnDef(parentColumnName);
    } catch (NoColumnException e) {
      throw new IllegalStateException("The column (" + parentColumnName + ") does not exist on the data resource (" + this.getRelationDef().getDataPath() + ")");
    }
    CollectionGenerator<?> parentGenerator = parentColumnDef.getOrCreateGenerator();
    if (parentGenerator == null) {
      throw new IllegalStateException("The parent column (" + parentColumnName + ") does have an generator.");
    }
    if (!(parentGenerator instanceof DataSetGenerator)) {
      throw new IllegalStateException("The parent column (" + parentColumnName + ") has a generator that is not an entity generator but " + parentGenerator);
    }
    DataSetMetaColumnGenerator<T> dataSetMetaColumnGenerator = (DataSetMetaColumnGenerator<T>) DataSetMetaColumnGenerator.create(this.getClazz(), columnName, (DataSetGenerator<?>) parentGenerator)
      .setColumnDef(this);
    this.generator = dataSetMetaColumnGenerator;
    return dataSetMetaColumnGenerator;

  }

  /**
   * The first element is to be sure to have one element
   * (otherwise, you can pass null to the T... objects argument
   */
  @SuppressWarnings("unchecked")
  public SequenceGenerator<T> addSequenceGenerator(T... objects) {
    SequenceGenerator<T> sequence = (SequenceGenerator<T>) SequenceGenerator.createFromValues(this.getClazz(), objects)
      .setColumnDef(this);
    this.setGenerator(sequence);
    return sequence;
  }

  public SequenceGenerator<T> addSequenceGenerator(List<T> objects) {
    T[] array = Casts.castToArray(objects);
    return (SequenceGenerator<T>) (this.addSequenceGenerator(array))
      .setColumnDef(this);
  }


  @Override
  public ColumnDef<T> setVariable(String key, Object value) {
    GenColumnAttribute attribute;
    try {
      attribute = Casts.cast(key, GenColumnAttribute.class);
    } catch (CastException e) {
      // there is no
      throw new IllegalArgumentException("The attribute (" + key + ") of the column (" + this + ") is not a valid generator column attribute. Possible values: " + String.join(", ", this.variables.keySet()));
    }
    if (attribute.equals(HIDDEN)) {
      Boolean isHidden;
      try {
        isHidden = Casts.cast(value, Boolean.class);
      } catch (CastException e) {
        throw new IllegalArgumentException("The value " + value + " of the hidden attribute of the column (" + this + ") is not a valid boolean");
      }
      setIsHidden(isHidden);
      return this;
    }
    setVariable(attribute, value);
    return this;
  }

  /**
   * Do we hide this column
   * <p>
   * Does this column is hidden (not printed)
   */
  public GenColumnDef<T> setIsHidden(boolean b) {

    if (b) {

      if (!this.isNotHidden()) {
        GenLog.LOGGER.fine("The column (" + this + ") is already hidden and was not hidden again.");
        return this;
      }
      /**
       * Reset the column position
       */
      for (int i = this.getColumnPosition() + 1; i <= this.getRelationDef().getColumnDefs().size(); i++) {
        GenColumnDef<?> columnDef = this.getRelationDef().getColumnDef(i);
        if (columnDef != null) {
          if (columnDef.isNotHidden()) {
            columnDef.setColumnPosition(columnDef.getColumnPosition() - 1);
          }
        }
      }

      /**
       * Hidden action
       * <p>
       * !!! After the loop processing above. !!!
       * This is important for the above loop
       * otherwise you miss the next column
       * because the {@link GenFsDataDef#getColumnDefs() function}
       * is sensitive to hidden
       */
      super.setVariable(HIDDEN, true);
      this.setColumnPosition(-1);
    }
    return this;
  }

  public boolean isNotHidden() {
    Boolean hiddenProperty;
    try {
      hiddenProperty = super.getVariable(HIDDEN).getValueOrDefaultCastAs(Boolean.class);
    } catch (CastException e) {
      throw new RuntimeException("Should not happen. We have a default value and check at set", e);
    }
    if (hiddenProperty != null) {
      return !hiddenProperty;
    }
    return true;

  }

  public RegexpGenerator<T> addRegexpGenerator(String s, Long seed) {
    RegexpGenerator<T> regexpGenerator = (RegexpGenerator<T>) (new RegexpGenerator<>(this.getClazz(), s, seed))
      .setColumnDef(this);
    this.generator = regexpGenerator;
    return regexpGenerator;
  }


  public CollectionGenerator<?> getGenerator() {
    return this.generator;
  }

  public Object getDataSupplierAttributeValue(DataSupplierAttribute dataGenAttribute) {
    return this.dataSupplierAttributeValueMap.get(dataGenAttribute);
  }

  public <V> V getDataSupplierAttributeValue(DataSupplierAttribute dataSupplierAttribute, Class<V> aClass) throws CastException {
    Object value = this.dataSupplierAttributeValueMap.get(dataSupplierAttribute);
    if (value == null) {
      return null;
    }
    return Casts.cast(value, aClass);
  }

  @Override
  public Boolean isNullable() {
    /**
     * may be null with a direct cast such as
     * List<GenDataPath> genDataPaths = fsDataPaths
     *   .stream()
     *   .map(GenFsDataPath.class::cast)
     *   .collect(Collectors.toList());
     */
    if (generator == null) {
      generator = getOrCreateGenerator();
    }
    return generator.isNullable();
  }

  public MetaAttributeGenerator<?> addDataResourceAttributeGenerator(DataPath dataPath, DataPathAttribute dataPathAttribute) {

    return addDataResourceAttributeGenerator(dataPath, KeyNormalizer.createSafe(dataPathAttribute));

  }

  public MetaAttributeGenerator<?> addDataResourceAttributeGenerator(DataPath dataPath, KeyNormalizer dataPathAttribute) {

    this.generator = new MetaAttributeGenerator<>(this.getClazz(), dataPath, dataPathAttribute);
    this.generator.setColumnDef(this);

    return (MetaAttributeGenerator<?>) this.generator;

  }

  public DataPathStreamGenerator<?> addDataPathStreamGenerator() {
    this.generator = new DataPathStreamGenerator<>(this.getClazz());
    this.generator.setColumnDef(this);
    return (DataPathStreamGenerator<?>) this.generator;
  }

  public CollectionGenerator<T> createGenerator() {
    Object dataGeneratorAsObject;
    try {
      dataGeneratorAsObject = this.getVariable(GenColumnAttribute.DATA_SUPPLIER).getValue();
    } catch (NoValueException e) {
      /**
       * We don't return an error because
       * it permits to create the generators
       * recursively from the Yaml properties if any
       */
      return null;
    }

    // When read from a data definition file into the column property
    try {
      this.dataSupplierAttributeValueMap = Casts.castToNewMap(
        dataGeneratorAsObject,
        DataSupplierAttribute.class,
        Object.class
      );
    } catch (CastException e) {
      throw new IllegalArgumentException("The data of the attribute " + GenColumnAttribute.DATA_SUPPLIER + " of the column (" + this + ") is not a conform map. Error: " + e.getMessage() + ". Value: " + dataGeneratorAsObject, e);
    }

    DataGenType dataGenType;
    String dataGenTypeAsString = (String) dataSupplierAttributeValueMap.get(DataSupplierAttribute.TYPE);
    try {
      dataGenType = Casts.cast(dataGenTypeAsString, DataGenType.class);
    } catch (CastException e) {
      throw new IllegalArgumentException("The generator type value (" + dataGenTypeAsString + ") of the column " + this + " is not valid. You can use one of " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(DataGenType.class), e);
    }
    Class<T> clazz = this.getClazz();
    switch (dataGenType) {
      case DATA_SET:
        generator = DataSetGenerator.createFromArguments(clazz, this);
        break;
      case ENTITY:
        generator = DataSetGenerator.createFromArguments(clazz, this);
        break;
      case DATA_SET_META:
        generator = DataSetMetaColumnGenerator.createFromProperties(clazz, this);
        break;
      case FOREIGN_COLUMN:
        generator = ForeignColumnGenerator.createFromProperties(clazz, this);
        break;
      case EXPRESSION:
        generator = ExpressionGenerator.createFromProperties(clazz, this);
        break;
      case RANDOM:
        generator = RandomGenerator.createFromProperties(clazz, this);
        break;
      case REGEXP:
        generator = RegexpGenerator.createFromProperties(clazz, this);
        break;
      case HISTOGRAM:
        generator = HistogramGenerator.createFromProperties(clazz, this);
        break;
      case SEQUENCE:
        generator = SequenceGenerator.createFromArguments(clazz, this);
        break;
      case META:
        generator = MetaAttributeGenerator.createFromProperties(clazz, this);
        break;
      default:
        throw new IllegalArgumentException("The generator (" + dataGenType + ") defined for the column (" + this + ") does not exist.");
    }

    return (generator).setColumnDef(this);
  }

  public void deleteGenerator() {
    generator = null;
  }

  public <A> Map<A, Object> getDataSupplierArgument(Class<A> clazz) {
    Object dataSupplierAttributeValue = this.getDataSupplierAttributeValue(DataSupplierAttribute.ARGUMENTS);
    if (dataSupplierAttributeValue == null) {
      /**
       * Some data supplier may have only optional arguments
       * such as DataSet as entity
       */
      return new HashMap<>();
    }
    try {
      return Casts.castToNewMap(
        dataSupplierAttributeValue,
        clazz,
        Object.class
      );
    } catch (CastException e) {
      throw new RuntimeException("The arguments of the sequence supplier for the column (" + this + ") is not conform. Error: " + e.getMessage() + ". \nValid arguments: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(clazz), e);
    }
  }
}
