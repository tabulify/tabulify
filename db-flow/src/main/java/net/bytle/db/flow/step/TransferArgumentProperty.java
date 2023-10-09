package net.bytle.db.flow.step;

import net.bytle.db.flow.Granularity;
import net.bytle.db.transfer.TransferOperation;
import net.bytle.db.transfer.TransferProperties;
import net.bytle.db.transfer.TransferResourceOperations;
import net.bytle.type.Attribute;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum TransferArgumentProperty implements Attribute {

  STEP_GRANULARITY(   "defines the granularity of the transfer", Granularity.RESOURCE, Granularity.class),
  STEP_OUTPUT(  "The resource that is passed as output", TransferOutputArgument.SOURCES, TransferOutputArgument.class),
  TRANSFER_OPERATION(  "defines the transfer operation (" + Arrays.stream(TransferOperation.values()).map(TransferOperation::toString).collect(Collectors.joining(", ")) + "). Default to `copy` for a file system and `insert` for a database.", null, TransferOperation.class),
  TRANSFER_MAPPING_METHOD( "defines the method used to map the source columns to the target columns", null, String.class),
  TRANSFER_COLUMN_MAPPING("defines the column mapping between the source and the target", null, String.class),
  TARGET_OPERATION( "defines the data operations (replace, truncate) on the existing target before transfer.", null, TransferResourceOperations.class),
  SOURCE_OPERATION("defines the data operation (drop or truncate) on the source after transfer. Note: A `move` operation will drop the source.", null, String.class),
  TARGET_COMMIT_FREQUENCY( "defines the commit frequency in number of batches against the target data resource", TransferProperties.DEFAULT_COMMIT_FREQUENCY, Integer.class),
  TARGET_BATCH_SIZE("defines the batch size against the target data resource", TransferProperties.DEFAULT_BATCH_SIZE,Integer.class),
  WITH_BIND_VARIABLES( "defines if bind variables are used in the SQL statement", true, Boolean.class),
  METRICS_DATA_URI( "defines a target data uri where the data metrics should be exported", null, String.class),
  SOURCE_FETCH_SIZE( "defines the size of the network message from the source to fetch the data", TransferProperties.DEFAULT_FETCH_SIZE, Integer.class),
  // No default because the buffer size is dependent on the fetch size,
  BUFFER_SIZE("defines the size of the memory buffer between the source and target threads", null,Integer.class),
  TARGET_WORKER_COUNT( "defines the target number of thread against the target datastore",TransferProperties.DEFAULT_TARGET_WORKER_COUNT, Integer.class);


  private final String description;
  private final Object defaultValue;
  private final Class<?> clazz;

  TransferArgumentProperty(String description, Object defaultValue, Class<?> clazz) {

    this.description = description;
    this.defaultValue = defaultValue;
    this.clazz = clazz;

  }


  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public Class<?> getValueClazz() {
    return this.clazz;
  }


  /**
   *
   *
   * @return The default value if not defined
   */
  public Object getDefaultValue() {
    return defaultValue;
  }



}
