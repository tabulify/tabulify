package com.tabulify.model;

import java.util.Map;

/**
 *
 *
 * A metadata class about column information
 *
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

  ColumnDef setNullable(int nullable);

  ColumnDef setNullable(Boolean nullable);

  String getFullyQualifiedName();

  @Override
  int compareTo(ColumnDef o);

  ColumnDef precision(Integer precision);

  ColumnDef setIsAutoincrement(String isAutoincrement);

  ColumnDef setIsGeneratedColumn(String isGeneratedColumn);

  ColumnDef scale(Integer scale);

  Object getDefault();

  String getDescription();

  ColumnDef setComment(String comment);

  /**
   * Retrieve a property
   *
   * @param name  - the first name of the path
   * @param names - the path to the property
   * @return Example:
   * if the total names are `generator`,`type`
   * this function will return the key `type` in the namespace `generator`
   */
  <T> T getVariable(Class<T> clazz, String name, String... names);

  /**
   * Retrieve a map property
   *
   * @param keyClazz   - the class of the key
   * @param valueClazz - the class of the value
   * @param name       - the first name of the path
   * @param names      - the path to the property
   * @return Example:
   * if the total names are `generator`,`buckets` with keyClazz of string and a value clazz of double
   * this function will return the key `buckets` in the namespace `generator`
   * with a `Map<String,Double>`
   */
  <K, V> Map<K, V> getMapProperty(Class<K> keyClazz, Class<V> valueClazz, String name, String... names);

  ColumnDef setVariable(String key, Object value);

  ColumnDef setVariable(Object value, String name, String... names);

  Map<String, Object> getProperties();

  String getComment();

  /**
   *
   * @return the class of the value
   */
  Class<?> getClazz();

  Integer getPrecisionOrMax();

  ColumnDef setAllPropertiesFrom(ColumnDef source);

  ColumnDef setPrecision(Integer precision);

  ColumnDef setScale(Integer scale);

}
