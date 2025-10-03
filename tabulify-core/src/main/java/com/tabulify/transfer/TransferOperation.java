package com.tabulify.transfer;

import net.bytle.exception.CastException;
import net.bytle.type.Casts;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * How the data should be loaded in the target document
 */
public enum TransferOperation {


  /**
   * If the table already exists, the records will be added (Default)
   * The same as append (An alias)
   * <p>
   * INSERT was chosen in place of APPEND
   * because insert is the default operation for a database
   */
  INSERT,


  /**
   * If the records already exists, update it, don't insert it if it doesn't exist
   */
  UPDATE,

  /**
   * If the records already exists, delete it
   */
  DELETE,

  /**
   * If the records already exists, update them and insert them otherwise
   * <p>
   * It's not a synonym of {@link #MERGE} because a MERGE can also delete records
   */
  UPSERT,

  /**
   * !!!! A merge is not implemented !!!!
   * It's an operation that can update, insert and delete at the same time
   * It's an {@link #UPSERT} and {@link #DELETE} at the same time
   * We implement it still as {@link UpsertType#MERGE upsert type}
   */
  MERGE,


  /**
   * Same as insert but the target data resources:
   * * should not have any data
   * * should have the same structure
   */
  COPY;


  /**
   * Does this transfer operation require
   * to have the same columns (structure) between the source and the target
   *
   * @return true if the structure should be the same
   */
  boolean requireSameStructureBetweenSourceAndTarget() {
    return this.equals(COPY);

  }


  public static TransferOperation createFrom(String name) {
    try {
      return Casts.cast(name, TransferOperation.class);
    } catch (CastException e) {
      String acceptedValues = Arrays.stream(TransferOperation.values())
        .map(TransferOperation::toString)
        .collect(Collectors.joining(", "));
      throw new IllegalArgumentException("The string (" + name + ") is not valid transfer operation. You may choose one of: " + acceptedValues + ")");
    }
  }


  @Override
  public String toString() {
    return super.toString().toLowerCase();
  }

}


