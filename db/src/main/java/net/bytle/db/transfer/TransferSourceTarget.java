package net.bytle.db.transfer;

import net.bytle.db.spi.DataPath;

/**
 * A class that model a transfer:
 *   * make a relation between a source and a target
 *   * make a relation between columns
 *   * and got the properties
 */
public class TransferSourceTarget {


  private final DataPath target;
  private final DataPath source;
  private TransferProperties transferProperties;


  public TransferSourceTarget(DataPath sourceDataPath, DataPath targetDataPath) {
    this.source = sourceDataPath;
    this.target = targetDataPath;
  }

  public DataPath getSourceDataPath() {
    return source;
  }

  public DataPath getTargetDataPath() {
    return target;
  }

  @Override
  public String toString() {
    return " "+source + " > "+target+" ";
  }

  public TransferProperties getTransferProperties() {
    return transferProperties;
  }

  public TransferSourceTarget setProperty(TransferProperties transferProperties) {
    this.transferProperties = transferProperties;
    return this;
  }
}
