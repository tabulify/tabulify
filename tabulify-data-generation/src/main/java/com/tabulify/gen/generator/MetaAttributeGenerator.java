package com.tabulify.gen.generator;


import com.tabulify.conf.Attribute;
import com.tabulify.gen.DataGenType;
import com.tabulify.gen.GenColumnDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.Meta;
import com.tabulify.spi.Tabulars;
import net.bytle.exception.CastException;
import net.bytle.exception.NoVariableException;
import net.bytle.type.Casts;
import net.bytle.type.KeyNormalizer;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Generate (ie return) the value of a attribute
 * ie {@link DataPath#getAttribute(KeyNormalizer)} the {@link Attribute#getPublicValue()}
 */
public class MetaAttributeGenerator<T> extends CollectionGeneratorAbs<T> implements CollectionGenerator<T>, java.util.function.Supplier<T> {

  public static final DataGenType TYPE = DataGenType.META;

  /**
   * We cache the value
   * (if it's a count, we don't want to perform it each time)
   */
  private T actualDataResourceAttributeValue;
  private Meta meta;
  private final KeyNormalizer dataResourceAttribute;


  public MetaAttributeGenerator(Class<T> clazz, Meta meta, KeyNormalizer dataResourceAttribute) {
    super(clazz);
    this.meta = meta;
    this.dataResourceAttribute = dataResourceAttribute;
  }

  /**
   * Instantiate an expression generator from the columns properties
   * This function is called via recursion by the function {@link GenColumnDef#getOrCreateGenerator(Class)}
   * Don't delete
   */
  public static <T> MetaAttributeGenerator<T> createFromProperties(Class<T> tClass, GenColumnDef genColumnDef) {

    Map<MetaAttributeArgument, Object> argumentMap = genColumnDef.getDataSupplierArgument(MetaAttributeArgument.class);

    // Data Uri (Maybe null) as the source is then the pipeline inputs
    String dataUri = (String) argumentMap.get(MetaAttributeArgument.DATA_URI);
    DataPath dataPath = null;
    if (dataUri != null) {
      dataPath = genColumnDef.getGenRelationDef().getDataPath().getConnection().getTabular().getDataPath(dataUri);
      if (!Tabulars.exists(dataPath)) {
        throw new IllegalArgumentException("The " + MetaAttributeArgument.DATA_URI + " attribute on the column " + genColumnDef + " specifies a data resource (" + dataUri + ") that does not exists.");
      }
    }

    KeyNormalizer dataResourceAttribute;
    Object dataGeneratorValue = argumentMap.get(MetaAttributeArgument.ATTRIBUTE);
    try {
      dataResourceAttribute = KeyNormalizer.create(dataGeneratorValue);
    } catch (CastException e) {
      throw new IllegalArgumentException("The " + MetaAttributeArgument.ATTRIBUTE + " attribute on the column " + genColumnDef + " has a value (" + dataGeneratorValue + ") that is not conform. Error: " + e.getMessage(), e);
    }
    // New Instance
    return (MetaAttributeGenerator<T>) (new MetaAttributeGenerator<>(tClass, dataPath, dataResourceAttribute))
      .setColumnDef(genColumnDef);

  }


  /**
   * @return a new generated data object every time it's called
   */
  @Override
  public T getNewValue() {

    Attribute attribute;
    try {
      attribute = this.meta.getAttribute(this.dataResourceAttribute);
    } catch (NoVariableException e) {
      return null;
    }
    // public, we don't want any breach
    Object value = attribute.getPublicValue();
    if (this.actualDataResourceAttributeValue != null) {
      return this.actualDataResourceAttributeValue;
    }
    try {
      T castedValue = Casts.cast(value, this.clazz);
      this.actualDataResourceAttributeValue = castedValue;
      return castedValue;
    } catch (CastException e) {
      throw new IllegalStateException("The value (" + value + ") of the attribute " + attribute + " could not be cast to " + this.clazz + ". Error: " + e.getMessage(), e);
    }


  }

  /**
   * @return a generated value
   */
  @Override
  public T getActualValue() {

    return actualDataResourceAttributeValue;
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
    this.actualDataResourceAttributeValue = null;
  }


  @Override
  public DataGenType getGeneratorType() {
    return DataGenType.META;
  }

  @Override
  public Boolean isNullable() {
    return true;
  }

  /**
   * A function to inject the meta-object at runtime
   *
   * @param meta - the object with meta attribute
   */
  public MetaAttributeGenerator<T> setMeta(Meta meta) {
    this.meta = meta;
    return this;
  }

}
