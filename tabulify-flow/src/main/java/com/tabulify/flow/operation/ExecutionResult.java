package com.tabulify.flow.operation;

import com.tabulify.spi.DataPath;
import net.bytle.exception.MissingSwitchBranch;
import net.bytle.log.Log;
import net.bytle.timer.Timer;
import net.bytle.type.time.DurationShort;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static com.tabulify.flow.operation.ExecuteResultAttribute.ERROR_MESSAGE;

public class ExecutionResult {


  private final ExecutionResultBuilder builder;

  public ExecutionResult(ExecutionResultBuilder executionResultBuilder) {
    this.builder = executionResultBuilder;
  }

  public List<?> getRecord(List<ExecuteResultAttribute> executeResultAttributes) {
    List<Object> record = new ArrayList<>();
    for (ExecuteResultAttribute executeResultAttribute : executeResultAttributes) {
      switch (executeResultAttribute) {
        case RUNTIME_DATA_URI:
          record.add(builder.runtime.toDataUri());
          break;
        case RUNTIME_EXECUTABLE_PATH:
          record.add(builder.runtime.getExecutableDataPath().getCompactPath());
          break;
        case RUNTIME_CONNECTION:
          record.add(builder.runtime.getConnection().getName());
          break;
        case RESULT_DATA_URI:
          if (builder.result != null) {
            record.add(builder.result.toDataUri());
            continue;
          }
          record.add(builder.result);
          break;
        case EXIT_CODE:
          if (builder.error == null) {
            record.add(0);
            continue;
          }
          record.add(1);
          break;
        case ERROR_MESSAGE:
          if (builder.error == null) {
            record.add(null);
            continue;
          }

          int stringLength = ERROR_MESSAGE.getPrecision();
          String messageSuffix = "";
          if (builder.errorDataPath != null) {
            messageSuffix = " . See " + builder.errorDataPath;
            stringLength -= messageSuffix.length();
          }
          String message = builder.error.getMessage();
          if (message == null) {
            message = builder.error.getClass().getName();
          }
          if (message.length() > stringLength) {
            message = message.substring(0, stringLength);
          }
          record.add(Log.onOneLine(message + messageSuffix));
          break;
        case COUNT:
          record.add(builder.targetRecordCount);
          break;
        case LATENCY:
          record.add(DurationShort.create(builder.timer.getDuration()).toIsoDuration());
          break;
        case LATENCY_MILLIS:
          record.add(builder.timer.getDuration().toMillis());
          break;
        case START_TIME:
          record.add(Timestamp.from(builder.timer.getStartTime()));
          break;
        case END_TIME:
          record.add(Timestamp.from(builder.timer.getEndTime()));
          break;
        case EXECUTION_TYPE:
          record.add(builder.executionMode.toString());
          break;
        case ERROR_DATA_URI:
          record.add(builder.errorDataPath.toString());
          break;
        default:
          throw new MissingSwitchBranch("execution column", executeResultAttribute);
      }
    }
    return record;

  }

  public static ExecutionResultBuilder build() {
    return new ExecutionResultBuilder();
  }

  public Exception getError() {
    return this.builder.error;
  }


  public static class ExecutionResultBuilder {


    private DataPath runtime;
    private Long targetRecordCount;
    private Timer timer;
    private DataPath result;
    private Exception error;
    private ExecutionMode executionMode;
    private DataPath errorDataPath;

    public ExecutionResultBuilder setRuntime(DataPath runtimeDataPath) {
      this.runtime = runtimeDataPath;
      return this;
    }

    public ExecutionResultBuilder setCount(Long recordCount) {
      this.targetRecordCount = recordCount;
      return this;
    }

    public ExecutionResultBuilder setTimer(Timer timer) {
      this.timer = timer;
      if (!timer.hasStopped()) {
        timer.stop();
      }
      return this;
    }

    public ExecutionResult build() {
      return new ExecutionResult(this);
    }

    public ExecutionResultBuilder setResult(DataPath result) {
      this.result = result;
      return this;
    }

    public ExecutionResultBuilder setError(Exception e) {
      this.error = e;
      return this;
    }

    public ExecutionResultBuilder setExecutionType(ExecutionMode executionMode) {
      this.executionMode = executionMode;
      return this;
    }

    public ExecutionResultBuilder setErrorDataPath(DataPath errorDataPath) {
      this.errorDataPath = errorDataPath;
      return this;
    }
  }
}
