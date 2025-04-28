package com.tabulify.model;

import net.bytle.type.Attribute;
import net.bytle.type.Variable;

import java.sql.DatabaseMetaData;
import java.util.Set;

/**
 * A metadata class about column information
 */
public interface ColumnDef extends Comparable<ColumnDef> {

  Boolean isGeneratedColumn();


  Boolean isNullable();

  Boolean isAutoincrement();

  String getColumnName();

  Integer getPrecision();

  Integer getScale();

  RelationDef getRelationDef();

  SqlDataType getDataType();

  ColumnDef setColumnPosition(int columnPosition);

  Integer getColumnPosition();

  /**
   * Nullable should be one of:
   * {@link DatabaseMetaData#columnNullable},
   * {@link DatabaseMetaData#columnNoNulls},
   * {@link DatabaseMetaData#columnNullableUnknown}
   */
  ColumnDef setNullable(int nullable);

  ColumnDef setNullable(Boolean nullable);

  String getFullyQualifiedName();

  @SuppressWarnings("NullableProblems")
  @Override
  int compareTo(ColumnDef o);

  ColumnDef precision(Integer precision);


  ColumnDef setIsAutoincrement(Boolean isAutoincrement);

  ColumnDef setIsGeneratedColumn(String isGeneratedColumn);

  ColumnDef scale(Integer scale);

  Object getDefault();

  String getDescription();

  ColumnDef setComment(String comment);

  /**
   * Retrieve a variable
   *
   * @return Example:
   * if the total names are `generator`,`type`
   * this function will return the key `type` in the namespace `generator`
   */
  Variable getVariable(Attribute attribute);


  /**
   * Function used to pass unknown attribute/properties
   * so that it can be checked by the underlining data path, create an attribute
   * and use the {@link #setVariable(Attribute, Object)}
   */
  ColumnDef setVariable(String key, Object value);

  /**
   * The main entry to set a variable
   */
  ColumnDef setVariable(Attribute key, Object value);


  Set<Variable> getVariables();

  String getComment();

  /**
   * @return the class of the value
   */
  Class<?> getClazz();

  Integer getPrecisionOrMax();

  ColumnDef setAllVariablesFrom(ColumnDef source);

  ColumnDef setPrecision(Integer precision);

  ColumnDef setScale(Integer scale);

}
