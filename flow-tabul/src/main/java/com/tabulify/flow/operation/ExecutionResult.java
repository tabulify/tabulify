package com.tabulify.flow.operation;

import com.tabulify.spi.DataPath;
import com.tabulify.exception.MissingSwitchBranch;

import com.tabulify.type.Strings;
import com.tabulify.type.time.DurationShort;
import com.tabulify.type.time.Timer;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static com.tabulify.flow.operation.ExecuteResultAttribute.*;

public class ExecutionResult {


    private final ExecutionResultBuilder builder;

    public ExecutionResult(ExecutionResultBuilder executionResultBuilder) {
        this.builder = executionResultBuilder;
    }

    public List<?> getRecord(List<ExecuteResultAttribute> executeResultAttributes) {
        List<Object> record = new ArrayList<>();
        for (ExecuteResultAttribute executeResultAttribute : executeResultAttributes) {
            Timer timer = builder.timer;
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
                case DATA_URI:
                    if (builder.result != null) {
                        record.add(builder.result.toDataUri());
                        continue;
                    }
                    if (builder.errorDataPath != null) {
                        record.add(builder.errorDataPath.toDataUri());
                        continue;
                    }
                    record.add(builder.result);
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
                    /**
                     * Add a message to see the error if not already in the output
                     */
                    if (builder.errorDataPath != null && !(executeResultAttributes.contains(DATA_URI) || executeResultAttributes.contains(ERROR_DATA_URI))) {
                        messageSuffix = ". See " + builder.errorDataPath;
                        stringLength -= messageSuffix.length();
                    }
                    String message = builder.error.getMessage();
                    if (message == null) {
                        message = builder.error.getClass().getName();
                    }
                    if (message.length() > stringLength) {
                        message = message.substring(0, stringLength - 2) + "..";
                    }
                    record.add(Strings.onOneLine(message + messageSuffix));
                    break;
                case COUNT:
                    record.add(builder.targetRecordCount);
                    break;
                case LATENCY:
                    String isoDuration = null;
                    /**
                     * Timer may be null if not executed
                     */
                    if (timer != null) {
                        isoDuration = DurationShort.create(timer.getDuration()).toIsoDuration();
                    }
                    record.add(isoDuration);
                    break;
                case LATENCY_MILLIS:
                    Long isoDurationMs = null;
                    /**
                     * Timer may be null if not executed
                     */
                    if (timer != null) {
                        isoDurationMs = timer.getDuration().toMillis();
                    }
                    record.add(isoDurationMs);
                    break;
                case START_TIME:
                    Timestamp startTime = null;
                    /**
                     * Timer may be null if not executed
                     */
                    if (timer != null) {
                        startTime = Timestamp.from(timer.getStartTime());
                    }
                    record.add(startTime);
                    break;
                case END_TIME:
                    Timestamp endTime = null;
                    /**
                     * Timer may be null if not executed
                     */
                    if (timer != null) {
                        endTime = Timestamp.from(timer.getEndTime());
                    }
                    record.add(endTime);
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
