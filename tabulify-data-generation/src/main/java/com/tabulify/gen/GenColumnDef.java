package com.tabulify.gen;

import com.tabulify.Tabular;
import com.tabulify.csv.CsvDataPath;
import com.tabulify.gen.generator.*;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.ColumnDefBase;
import com.tabulify.model.SqlDataType;
import net.bytle.exception.CastException;
import net.bytle.exception.NoColumnException;
import net.bytle.exception.NoValueException;
import net.bytle.type.Arrayss;
import net.bytle.type.Casts;
import net.bytle.type.Enums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tabulify.gen.GenColumnAttribute.HIDDEN;

/**
 * A wrapper/extension around a {@link ColumnDefBase}
 * that map a {@link ColumnDefBase} to a {@link CollectionGenerator}
 */
public class GenColumnDef extends ColumnDefBase implements ColumnDef {


  private final GenRelationDef genDataDef;
  private CollectionGenerator<?> generator;
  // Attribute value may be string, map or list
  private Map<DataGenAttribute, Object> dataGeneratorMetaData;


  /**
   * Use {@link #createOf(GenRelationDef, String, SqlDataType, Class)}
   *
   * @param genRelationDef - the relation
   * @param columnName     the column name
   * @param sqlDataType    the data type
   * @param clazz          the class is here to have a sort of type checking (not yet full proof)
   */
  private GenColumnDef(GenRelationDef genRelationDef, String columnName, SqlDataType sqlDataType, Class<?> clazz) {
    super(genRelationDef, columnName, clazz, sqlDataType);
    this.genDataDef = genRelationDef;
    this.addVariablesFromEnumAttributeClass(GenColumnAttribute.class);
  }

  @Override
  public GenRelationDef getRelationDef() {
    return this.genDataDef;
  }


  public static GenColumnDef createOf(GenRelationDef genDataDef, String columnName, SqlDataType sqlDataType, Class<?> clazz) {
    assert columnName.length() < 100;
    return new GenColumnDef(genDataDef, columnName, sqlDataType, clazz);
  }


  public CollectionGenerator<?> getOrCreateGenerator() {
    return getOrCreateGenerator(this.getClazz());
  }

  public <T> CollectionGenerator<T> getOrCreateGenerator(Class<T> clazz) {

    if (generator != null) {
      //noinspection unchecked
      return (CollectionGenerator<T>) (generator)
        .setColumnDef(this);
    }
    Object dataGeneratorAsObject;
    try {
      dataGeneratorAsObject = this.getVariable(GenColumnAttribute.DATA_GENERATOR).getValue();
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
      this.dataGeneratorMetaData = Casts.castToNewMap(
        dataGeneratorAsObject,
        DataGenAttribute.class,
        Object.class
      );
    } catch (CastException e) {
      throw new RuntimeException("The data of the attribute " + GenColumnAttribute.DATA_GENERATOR + " of the column (" + this + ") is not a conform map. Error: " + e.getMessage() + ". Value: " + dataGeneratorAsObject, e);
    }

    DataGenType dataGenType;
    String dataGenTypeAsString = (String) dataGeneratorMetaData.get(DataGenAttribute.TYPE);
    try {
      dataGenType = Casts.cast(dataGenTypeAsString, DataGenType.class);
    } catch (CastException e) {
      throw new RuntimeException("The generator type value (" + dataGenTypeAsString + ") of the column " + this + " is not valid. You can use one of " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(DataGenType.class), e);
    }
    switch (dataGenType) {
      case DATA_SET:
        generator = DataSetGenerator.createFromProperties(clazz, this);
        break;
      case DATA_SET_COLUMN:
        generator = DataSetColumnGenerator.createFromProperties(clazz, this);
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
        generator = SequenceGenerator.createFromProperties(clazz, this);
        break;
      default:
        throw new RuntimeException("The generator (" + dataGenType + ") defined for the column (" + this + ") does not exist.");
    }
    //noinspection unchecked
    return (CollectionGenerator<T>) (generator)
      .setColumnDef(this);

  }

  public SequenceGenerator<?> addSequenceGenerator() {

    SequenceGenerator<?> sequenceGenerator = (SequenceGenerator<?>) SequenceGenerator.create(this.getClazz())
      .setColumnDef(this);
    generator = sequenceGenerator;
    return sequenceGenerator;

  }

  public <T> SequenceGenerator<T> addSequenceGenerator(Class<T> clazz) {
    this.clazz = clazz;
    SequenceGenerator<T> sequenceGenerator = (SequenceGenerator<T>) SequenceGenerator.create(clazz)
      .setColumnDef(this);
    generator = sequenceGenerator;
    return sequenceGenerator;
  }

  public ExpressionGenerator<?> addExpressionGenerator(String expression, String... parentColumns) {
    return this.addExpressionGenerator(this.getClazz(), expression, parentColumns);
  }

  public <T> ExpressionGenerator<T> addExpressionGenerator(Class<T> clazz, String expression, String... parentColumns) {

    this.clazz = clazz;

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
      GenColumnDef columnDef;
      try {
        columnDef = this.getRelationDef().getColumnDef(parentColumn);
      } catch (NoColumnException e) {
        throw new RuntimeException("The parent column (" + parentColumn + ") does not exist in the data resources (" + this.getRelationDef().getDataPath() + ").");
      }
      CollectionGenerator<?> parentGenerator = columnDef.getOrCreateGenerator(clazz);
      if (parentGenerator == null) {
        throw new RuntimeException("The parent column (" + parentColumn + ") has no collection generator defined");
      } else {
        parentGenerators.add(parentGenerator);
      }
    }
    ExpressionGenerator<T> expressionGenerator = (ExpressionGenerator<T>) (new ExpressionGenerator<>(clazz, expression, parentGenerators))
      .setColumnDef(this);


    generator = expressionGenerator;
    return expressionGenerator;
  }


  @Override
  public GenColumnDef precision(Integer precision) {
    super.precision(precision);
    return this;
  }

  public <T> HistogramGenerator<T> addHistogramGenerator(Class<T> clazz, Map<T, Double> buckets) {
    this.clazz = clazz;
    HistogramGenerator<T> histogramGenerator = (HistogramGenerator<T>) HistogramGenerator.create(clazz, buckets)
      .setColumnDef(this);
    generator = histogramGenerator;
    return histogramGenerator;
  }

  @SuppressWarnings("unchecked")
  public <T> HistogramGenerator<T> addHistogramGenerator(Class<T> clazz, T element, T... elements) {
    this.clazz = clazz;

    if (element instanceof Map) {
      Map<T, Double> castedElement;
      try {
        castedElement = Casts.castToSameMap(element, clazz, Double.class);
      } catch (CastException e) {
        throw new RuntimeException("An histogram element is not a double", e);
      }
      return addHistogramGenerator(clazz, castedElement);
    } else {
      Map<T, Double> buckets = Arrays.stream(Arrayss.concat(element, elements))
        .collect(Collectors.toMap(e -> e, e -> 1.0));
      HistogramGenerator<T> histogramGenerator = (HistogramGenerator<T>) HistogramGenerator.create(clazz, buckets)
        .setColumnDef(this);
      generator = histogramGenerator;
      return histogramGenerator;
    }
  }


  /**
   * @param collectionGenerator the collection generator
   * @return the column def
   */
  public GenColumnDef setGenerator(CollectionGenerator<?> collectionGenerator) {
    generator = collectionGenerator;
    return this;
  }

  public <T> DataSetGenerator<T> addEntityGenerator(Class<T> clazz, String name) {
    return addEntityGenerator(clazz, name, name);
  }


  public <T> DataSetGenerator<T> addEntityGenerator(Class<T> clazz, String entityName, String entityColumnName) {
    this.clazz = clazz;
    Tabular tabular = this.genDataDef.getDataPath().getConnection().getTabular();
    CsvDataPath csvDataPath = DataSetGenerator.getEntityPath(tabular, entityName, null);
    DataSetGenerator<T> dataSetGenerator = (DataSetGenerator<T>) DataSetGenerator.create(clazz, csvDataPath, entityColumnName)
      .setColumnDef(this);
    this.generator = dataSetGenerator;
    return dataSetGenerator;

  }

  public <T> RandomGenerator<T> addRandomGenerator(Class<T> clazz) {
    this.clazz = clazz;
    RandomGenerator<T> randomGenerator = (RandomGenerator<T>) RandomGenerator
      .of(clazz)
      .setColumnDef(this);
    generator = randomGenerator;
    return randomGenerator;
  }

  public <T> RandomGenerator<T> addRandomGenerator(Class<T> clazz, T min, T max) {
    this.clazz = clazz;
    RandomGenerator<T> randomGenerator = (RandomGenerator<T>) (new RandomGenerator<>(clazz, min, max))
      .setColumnDef(this);
    generator = randomGenerator;
    return randomGenerator;
  }


  public <T> DataSetColumnGenerator<T> addEntityAttributeGenerator(Class<T> clazz, String parentColumnName, String columnName) {

    this.clazz = clazz;

    GenColumnDef columnDef;
    try {
      columnDef = this.getRelationDef().getColumnDef(parentColumnName);
    } catch (NoColumnException e) {
      throw new IllegalStateException("The column (" + parentColumnName + ") does not exist on the data resource (" + this.getRelationDef().getDataPath() + ")");
    }
    CollectionGenerator<T> parentGenerator = columnDef.getOrCreateGenerator(clazz);
    if (parentGenerator == null) {
      throw new IllegalStateException("The parent column (" + parentColumnName + ") does have an generator.");
    }
    if (!(parentGenerator instanceof DataSetGenerator)) {
      throw new IllegalStateException("The parent column (" + parentColumnName + ") has a generator that is not an entity generator but " + parentGenerator);
    }
    DataSetColumnGenerator<T> dataSetColumnGenerator = (DataSetColumnGenerator<T>) DataSetColumnGenerator.create(clazz, columnName, (DataSetGenerator<T>) parentGenerator)
      .setColumnDef(this);
    this.generator = dataSetColumnGenerator;
    return dataSetColumnGenerator;

  }

  /**
   * The first element is to be sure to have one element
   * (otherwise, you can pass null to the T... objects argument
   */
  @SuppressWarnings("unchecked")
  public <T> SequenceGenerator<T> addSequenceGenerator(Class<T> clazz, T... objects) {
    SequenceGenerator<T> sequence = (SequenceGenerator<T>) SequenceGenerator.createFromValues(clazz, objects)
      .setColumnDef(this);
    this.clazz = clazz;
    this.setGenerator(sequence);
    return sequence;
  }

  public <T> SequenceGenerator<T> addSequenceGenerator(Class<T> clazz, List<T> objects) {
    this.clazz = clazz;
    T[] array = Casts.castToArray(objects);
    return (SequenceGenerator<T>) (this.addSequenceGenerator(clazz, array))
      .setColumnDef(this);
  }


  @Override
  public ColumnDef setVariable(String key, Object value) {
    GenColumnAttribute attribute;
    try {
      attribute = Casts.cast(key, GenColumnAttribute.class);
    } catch (CastException e) {
      // there is no
      throw new RuntimeException("The attribute (" + key + ") of the column (" + this + ") is not a valid generator column attribute. Possible values: " + String.join(", ", this.variables.keySet()));
    }
    if (attribute.equals(HIDDEN)) {
      Boolean isHidden;
      try {
        isHidden = Casts.cast(value, Boolean.class);
      } catch (CastException e) {
        throw new RuntimeException("The value " + value + " of the hidden attribute of the column (" + this + ") is not a valid boolean");
      }
      setIsHidden(isHidden);
      return this;
    }
    setVariable(attribute, value);
    return this;
  }

  /**
   * Do we hid this column
   * <p>
   * Does this column is hidden (not printed)
   */
  public GenColumnDef setIsHidden(boolean b) {

    if (b) {

      if (!this.isNotHidden()) {
        GenLog.LOGGER.fine("The column (" + this + ") is already hidden and was not hidden again.");
        return this;
      }
      /**
       * Reset the column position
       */
      for (int i = this.getColumnPosition() + 1; i <= this.getRelationDef().getColumnDefs().size(); i++) {
        GenColumnDef genColumnDef = this.getRelationDef().getColumnDef(i);
        if (genColumnDef.isNotHidden()) {
          genColumnDef.setColumnPosition(genColumnDef.getColumnPosition() - 1);
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
    } catch (NoValueException | CastException e) {
      throw new RuntimeException("Should not happen. We have a default value and check at set", e);
    }
    if (hiddenProperty != null) {
      return !hiddenProperty;
    }
    return true;

  }

  public <T> RegexpGenerator<T> addRegexpGenerator(Class<T> clazz, String s) {
    RegexpGenerator<T> regexpGenerator = (RegexpGenerator<T>) (new RegexpGenerator<>(clazz, s))
      .setColumnDef(this);
    this.generator = regexpGenerator;
    return regexpGenerator;
  }


  public CollectionGenerator<?> getGenerator() {
    return this.generator;
  }

  public Object getDataGeneratorValue(DataGenAttribute dataGenAttribute) {
    return this.dataGeneratorMetaData.get(dataGenAttribute);
  }

  public <T> T getDataGeneratorValue(DataGenAttribute dataGenAttribute, Class<T> aClass) throws CastException {
    Object value = this.dataGeneratorMetaData.get(dataGenAttribute);
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
      generator = getOrCreateGenerator(clazz);
    }
    return generator.isNullable();
  }

}
