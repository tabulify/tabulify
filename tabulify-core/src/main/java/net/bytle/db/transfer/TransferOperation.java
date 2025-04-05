package net.bytle.db.transfer;

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
   *
   * It's not a synonym of {@link #MERGE} because a MERGE can also delete records
   */
  UPSERT,

  /**
   * !!!! A merge is not yet implemented !!!!
   * It's an operation that can update, insert and delete at the same time
   * Ir an {@link #UPSERT} and {@link #DELETE} at the same time
   */
  MERGE,

  /**
   * Move
   * * rename operation on the same same system
   * * or transfer operation
   * * where the target data path will:
   * * have the same structure (columns),
   * * contain at the end of the operations the same data set
   * * where the source data path will be deleted
   */
  MOVE,

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
   * @return
   */
  boolean requireSameStructureBetweenSourceAndTarget() {
    if (this.equals(COPY) || this.equals(MOVE)) {
      return true;
    } else {
      return false;
    }
  }


  public static TransferOperation createFrom(String name) {
    String lowercaseName = name.toLowerCase();
    switch (lowercaseName) {
      case "append":
      case "insert":
        return INSERT;
      case "copy":
        return COPY;
      case "upsert":
        return UPSERT;
      case "update":
        return UPDATE;
      case "delete":
        return DELETE;
      case "move":
        return MOVE;
      default:
        String acceptedValues = Arrays.stream(TransferOperation.values())
          .map(TransferOperation::toString)
          .collect(Collectors.joining(", "));
        throw new IllegalStateException("The string (" + name + ") is not valid transfer operation. You may choose one of: " + acceptedValues + ")");
    }
  }


  @Override
  public String toString() {
    return super.toString().toLowerCase();
  }
}


