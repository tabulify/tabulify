package com.tabulify.fs.runtime;

import com.tabulify.conf.AttributeEnum;
import com.tabulify.uri.DataUriStringNode;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.time.DurationShort;

import java.util.List;

public enum FsCommandAttribute implements AttributeEnum {

  ARGUMENTS("A list of arguments", List.class, List.of()),
  WORKING_DIRECTORY("A working directory path", String.class, null),
  TIME_OUT("A timeout in second", DurationShort.class, DurationShort.createSafe("60s")),
  STD_BUFFER_SIZE("The IO buffer size used when writing to standard stream", Integer.class, 8192),
  STDOUT_DATA_URI("The template data uri that defines where the standard output of the execution should be stored", DataUriStringNode.class, Constants.DEFAULT_STDOUT_DATA_URI),
  RESULT_DATA_URI("An optional template data uri that specifies the location of the result data path returned", DataUriStringNode.class, null),
  STDERR_DATA_URI("The template data uri that defines where the standard error of the execution should be stored", DataUriStringNode.class, Constants.DEFAULT_STDERR_DATA_URI),
  ;

  private final String desc;
  private final Class<?> aClass;
  private final Object defaultValue;

  FsCommandAttribute(String desc, Class<?> aClass, Object defaultValue) {
    this.desc = desc;
    this.aClass = aClass;
    this.defaultValue = defaultValue;
  }

  @Override
  public String getDescription() {
    return desc;
  }

  @Override
  public Object getDefaultValue() {
    return this.defaultValue;
  }

  @Override
  public Class<?> getValueClazz() {
    return this.aClass;
  }

  @Override
  public String toString() {
    return KeyNormalizer.createSafe(name()).toCliLongOptionName();
  }

  private static class Constants {
    public static final DataUriStringNode DEFAULT_STDOUT_DATA_URI = DataUriStringNode.createFromStringSafe("execute/${execution_start_time}-${executable_logical_name}.log@tmp");
    public static final DataUriStringNode DEFAULT_STDERR_DATA_URI = DataUriStringNode.createFromStringSafe("execute/${execution_start_time}-${executable_logical_name}-stderr.log@tmp");
  }
}
