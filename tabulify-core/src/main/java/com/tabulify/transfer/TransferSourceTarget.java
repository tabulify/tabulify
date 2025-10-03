package com.tabulify.transfer;

import com.tabulify.spi.DataPath;
import net.bytle.exception.InternalException;

/**
 * A builder object for a final {@link TransferSourceTargetOrder}
 * It was created to be able to map easily a source to a target
 * and finally to inject the transfer properties
 */
public class TransferSourceTarget {
  private final DataPath targetDataPath;
  private final DataPath sourceDataPath;

  public DataPath getTargetDataPath() {
    return targetDataPath;
  }

  public DataPath getSourceDataPath() {
    return sourceDataPath;
  }

  private TransferSourceTarget(DataPath sourceDataPath, DataPath targetDataPath) {
    this.sourceDataPath = sourceDataPath;
    this.targetDataPath = targetDataPath;
  }

  public static TransferSourceTarget create(DataPath sourceDataPath, DataPath targetDataPath) {
    return new TransferSourceTarget(sourceDataPath, targetDataPath);
  }


  public TransferSourceTargetOrder buildOrder(TransferPropertiesSystem.TransferPropertiesSystemBuilder transferProperties) {

    return new TransferSourceTargetOrder(sourceDataPath, targetDataPath, transferProperties.build());
  }

  /**
   * Insert Order with all default value
   */
  public TransferSourceTargetOrder buildInsertOrder() {

    return buildOrder(TransferPropertiesSystem.builder().setOperation(TransferOperation.INSERT).build());
  }

  /**
   * Used to build the buffer queue
   * And to pass a modified {@link TransferPropertiesSystem} at the last time
   * ie no drop,
   */
  public TransferSourceTargetOrder buildOrder(TransferPropertiesSystem transferPropertiesSystem) {
    if (transferPropertiesSystem.getOperation() == null) {
      throw new InternalException("The operation was not set. Use buildOrder with builder");
    }
    return new TransferSourceTargetOrder(sourceDataPath, targetDataPath, transferPropertiesSystem);
  }

  @Override
  public String toString() {
    return sourceDataPath + " -> " + targetDataPath;
  }
}
