package net.bytle.db.transfer;

import net.bytle.db.spi.DataPath;

/**
 * A class that makes a relation between a source and a target
 */
public class TransferSourceTarget {


  private final DataPath target;
  private final DataPath source;

  public TransferSourceTarget(DataPath sourceDataPath, DataPath targetDataPath) {
    this.source = sourceDataPath;
    this.target = targetDataPath;
  }

  public static TransferSourceTarget of(DataPath sourceDataPath, DataPath targetDataPath) {
    return new TransferSourceTarget(sourceDataPath,targetDataPath);
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
}
