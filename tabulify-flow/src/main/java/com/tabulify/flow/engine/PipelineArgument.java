package com.tabulify.flow.engine;

import com.tabulify.uri.DataUriStringNode;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.time.DurationShort;

/**
 * The parameter of a {@link Pipeline}
 */
public enum PipelineArgument implements ArgumentEnum {

  POLL_INTERVAL("The interval between poll in millisecond (a poll is a request for data resources)", false, DurationShort.class, DurationShort.createSafe("1s")),
  PUSH_INTERVAL("The interval between push in millisecond (a push is the sending of a data resource into the pipeline)", false, DurationShort.class, DurationShort.createSafe("0s")),
  WINDOW_INTERVAL("The interval between execution in millisecond of batch intermediate operation", false, DurationShort.class, DurationShort.createSafe("2s")),
  MAX_CYCLE_COUNT("The maximum cycle count that a stream pipeline may resolve", false, Long.class, Long.MAX_VALUE),
  TIMEOUT("A timeout Duration", false, DurationShort.class, null),
  TIMEOUT_TYPE("The type of timeout (duration or error)", false, PipelineTimeoutType.class, PipelineTimeoutType.ERROR),
  ON_ERROR_ACTION("The action taken in case of error", false, PipelineOnErrorAction.class, PipelineOnErrorAction.STOP),
  STRICT_EXECUTION("Strict mode (Fail conditions that are ambiguous)", false, Boolean.class, true),
  PARKING_TARGET_URI("The parking target uri", false, DataUriStringNode.class, Constants.DEFAULT_PARKING_TARGET_URI
  );

  private final String description;
  private final Boolean mandatory;
  private final Class<?> clazz;
  private final Object defaultValue;


  PipelineArgument(String description, Boolean mandatory, Class<?> clazz, Object defaultValue) {

    this.description = description;
    this.mandatory = mandatory;
    this.clazz = clazz;
    this.defaultValue = defaultValue;

  }


  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public Class<?> getValueClazz() {
    return clazz;
  }

  @Override
  public Object getDefaultValue() {
    return this.defaultValue;
  }


  public boolean getMandatory() {
    return mandatory;
  }

  @Override
  public String toString() {
    return KeyNormalizer.createSafe(name()).toCliLongOptionName();
  }

  private static class Constants {
    public static final DataUriStringNode DEFAULT_PARKING_TARGET_URI = DataUriStringNode.createFromStringSafe("parking/${pipeline_logical_name}/${pipeline_start_time}@data-home");
  }
}
