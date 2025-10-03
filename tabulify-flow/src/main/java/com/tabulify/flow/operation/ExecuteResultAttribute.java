package com.tabulify.flow.operation;

import net.bytle.type.KeyNormalizer;

import java.sql.Timestamp;

public enum ExecuteResultAttribute {


  RUNTIME_DATA_URI("The runtime data uri", String.class, null),
  ERROR_DATA_URI("The error data uri", String.class, null),
  RUNTIME_EXECUTABLE_PATH("The runtime executable path", String.class, null),
  RUNTIME_CONNECTION("The runtime connection", String.class, null),
  COUNT("The count of records", Long.class, null),
  LATENCY("The duration in human form", String.class, null),
  LATENCY_MILLIS("The duration in millisecond", Long.class, null),
  RESULT_DATA_URI("The target data uri", String.class, null),
  EXIT_CODE("The exit value", Integer.class, null),
  ERROR_MESSAGE("The error message", Integer.class, 100),
  START_TIME("The start time", Timestamp.class, null),
  END_TIME("The end time", Timestamp.class, null),
  EXECUTION_TYPE("The type of execution", String.class, null);

  private final String desc;
  private final Class<?> valueClazz;
  private final Integer precision;

  /**
   * @param desc            - the description
   * @param columnClazz     - the column type (class of the cell/column type)
   * @param columnPrecision - the column precisions
   */
  ExecuteResultAttribute(String desc, Class<?> columnClazz, Integer columnPrecision) {
    this.desc = desc;
    this.valueClazz = columnClazz;
    this.precision = columnPrecision;
  }

  public Class<?> getValueClazz() {
    return this.valueClazz;
  }

  public String getDesc() {
    return this.desc;
  }

  public KeyNormalizer toKeyNormalizer() {
    return KeyNormalizer.createSafe(this.name());
  }

  public Integer getPrecision() {
    return this.precision;
  }
}
