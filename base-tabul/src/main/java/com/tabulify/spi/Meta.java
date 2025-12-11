package com.tabulify.spi;

import com.tabulify.conf.Attribute;
import com.tabulify.conf.AttributeEnum;
import com.tabulify.model.RelationDef;
import com.tabulify.stream.InsertStream;
import com.tabulify.exception.NoVariableException;
import com.tabulify.type.KeyNormalizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * An interface that represents an object with metadata (ie {@link Attribute}
 * This interface is used in the `Meta` Data Supplier
 * and in other place to get the attributes
 */
public interface Meta {

  /**
   * @param attribute - the attribute
   * @return the variable
   * @throws NoVariableException - when the variable was not found
   */
  default Attribute getAttribute(AttributeEnum attribute) throws NoVariableException {
    return getAttribute(KeyNormalizer.createSafe(attribute));
  }


  /**
   * @param name the attribute name
   * @return the variable
   * @throws NoVariableException - when the variable was not found
   *                             Why string and not {@link AttributeEnum} because variable may be created dynamically
   *                             (for instance backref of regexp), and therefore may be not known in advance
   */
  Attribute getAttribute(KeyNormalizer name) throws NoVariableException;


  Set<Attribute> getAttributes();

  /**
   * Same as {@link #getAttribute(AttributeEnum)} but without compile exception
   */
  default Attribute getAttributeSafe(AttributeEnum attribute) {
    try {
      return getAttribute(attribute);
    } catch (NoVariableException e) {
      throw new IllegalArgumentException("The attribute (" + attribute + ") was not found in the resource (" + this + ")", e);
    }
  }

  DataPath toAttributesDataPath();

  /**
   * @return the attributes / properties in a data path format
   */
  default DataPath toAttributesDataPath(DataPath dataPath) {

    RelationDef variablesDataPath = dataPath
      .getOrCreateRelationDef()
      .addColumn(KeyNormalizer.createSafe(AttributeProperties.ATTRIBUTE).toSqlCase())
      .addColumn(KeyNormalizer.createSafe(AttributeProperties.VALUE).toSqlCase())
      .addColumn(KeyNormalizer.createSafe(AttributeProperties.DESCRIPTION).toSqlCase());

    try (InsertStream insertStream = variablesDataPath.getDataPath().getInsertStream()) {
      for (com.tabulify.conf.Attribute attribute : this.getAttributes()) {
        List<Object> row = new ArrayList<>();
        row.add(KeyNormalizer.createSafe(attribute.getAttributeMetadata().toString()).toCamelCase());
        row.add(attribute.getValueOrDefault());
        row.add(attribute.getAttributeMetadata().getDescription());
        insertStream.insert(row);
      }
    }
    return variablesDataPath.getDataPath();
  }


}
