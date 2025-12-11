package com.tabulify.model;

import com.tabulify.conf.Attribute;
import com.tabulify.conf.AttributeEnum;
import com.tabulify.type.KeyNormalizer;

import java.util.Set;

/**
 * A metadata class about column information
 */
public interface ColumnDef<T> extends Comparable<ColumnDef<T>> {

  Boolean isGeneratedColumn();


  Boolean isNullable();

  Boolean isAutoincrement();

  String getColumnName();

  KeyNormalizer getColumnNameNormalized();

  /**
   * @return the precision of a number, the precision of a timestamp or the length of a character
   * Not an integer but an int to matches the SQL JDBC specification
   * 0 means null
   */
  int getPrecision();

  /**
   * @return the scale.
   * Scale could be a short but as there is no short literal
   * and that the diff is minim, we keep with int
   * Not an integer but an int to matches the SQL JDBC specification
   * 0 means null
   */
  int getScale();

  /**
   * @return the definition of the relation this column belongs
   */
  RelationDef getRelationDef();

  /**
   * @return the data type of the value
   */
  SqlDataType<T> getDataType();

  ColumnDef<T> setColumnPosition(int columnPosition);

  /**
   * @return the column position
   * It can not be null, we use an integer to be able to do a comparison
   */
  int getColumnPosition();

  /**
   *
   */
  ColumnDef<T> setNullable(SqlDataTypeNullable nullable);

  ColumnDef<T> setNullable(Boolean nullable);

  String getFullyQualifiedName();

  @SuppressWarnings("NullableProblems")
  @Override
  int compareTo(ColumnDef o);

  ColumnDef<T> setIsAutoincrement(Boolean isAutoincrement);

  ColumnDef<T> setIsGeneratedColumn(Boolean isGeneratedColumn);


  T getDefault();


  ColumnDef<T> setComment(String comment);

  /**
   * Retrieve a variable
   *
   * @return Example:
   * if the total names are `generator`,`type`
   * this function will return the key `type` in the namespace `generator`
   */
  Attribute getVariable(AttributeEnum attribute);


  /**
   * Function used to pass unknown attribute/properties
   * so that it can be checked by the underlining data path, create an attribute
   * and use the {@link #setVariable(AttributeEnum, Object)}
   */
  ColumnDef<T> setVariable(String key, Object value);

  /**
   * The main entry to set a variable
   */
  ColumnDef<T> setVariable(AttributeEnum key, Object value);


  Set<Attribute> getVariables();

  /**
   * @return the comment
   * Not called a description to match with the SQL database name for a description
   */
  String getComment();

  /**
   * @return the class of the value
   */
  Class<T> getClazz();

  int getPrecisionOrMax();

  ColumnDef<T> setAllVariablesFrom(ColumnDef<?> source);

  ColumnDef<T> setPrecision(int precision);

  ColumnDef<T> setScale(int scale);

  /**
   * @return the final ansi type
   * (ie a bit(1) is a boolean)
   */
  SqlDataTypeAnsi getAnsiType();

}
