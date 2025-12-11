package com.tabulify.transfer;

import com.tabulify.DbLoggers;
import com.tabulify.Tabular;
import com.tabulify.memory.MemoryConnection;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import com.tabulify.type.Strings;
import com.tabulify.type.time.DurationShort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Static Utility function
 */
public class Transfers {


  /**
   * The name of the data path that returns the listeners
   * It should stay the same in a pipeline scope
   * so that the {@link com.tabulify.stream.Printer} see it as one logical table,
   * and it can be printed in a stream fashion
   */
  public static final String TRANSFER_LISTENERS_NAME = "transfer_results";

  /**
   * The main entry property where the signature is what a user should enter
   *
   * @param tabular            the tabular context
   * @param sourceTargets      a source target map
   * @param transferPropertiesCross the transfer properties to apply
   * @return the transfer listener
   */
  public static List<TransferListener> transfers(
    Tabular tabular,
    Map<? extends DataPath, ? extends DataPath> sourceTargets,
    TransferPropertiesCross transferPropertiesCross
  ) {

    /**
     * Start building the transferManager object
     */
    TransferManager transferManager = TransferManager.builder()
      .setTransferCrossProperties(transferPropertiesCross)
      .build();

    /**
     * Add the transfers
     */
    List<TransferSourceTarget> transferSourceTargetList = new ArrayList<>();
    for (Map.Entry<? extends DataPath, ? extends DataPath> sourceTarget : sourceTargets.entrySet()) {
      TransferSourceTarget transferSourceTarget = TransferSourceTarget.create(sourceTarget.getKey(), sourceTarget.getValue());
      transferSourceTargetList.add(transferSourceTarget);
    }


    /**
     * Start
     */
    List<TransferListener> transferListeners = transferManager
      .createOrder(transferSourceTargetList)
      .execute()
      .getTransferListeners();

    // Exit
    long exitStatus = transferListeners
      .stream()
      .mapToInt(TransferListener::getExitStatus)
      .sum();

    if (exitStatus != 0) {
      String msg = "Error ! (" + exitStatus + ") errors were seen.";
      DbLoggers.LOGGER_DB_ENGINE.severe(msg);
      tabular.setExitStatus(Math.toIntExact(exitStatus));
    }

    return transferListeners;
  }

  public static DataPath transfersListenersToDataPath(Tabular tabular, List<TransferListener> transferListeners) {
    DataPath result = tabular.getMemoryConnection().getDataPath(TRANSFER_LISTENERS_NAME)
      .setComment("Transfer results");
    result.getOrCreateRelationDef()
      .addColumn("input")
      .addColumn("target")
      .addColumn("latency")
      .addColumn("record_count", Integer.class)
      .addColumn("error_code", Integer.class)
      .addColumn("error_message");
    try (
      InsertStream insertStream = result.getInsertStream()
    ) {
      Collections.sort(transferListeners);
      for (TransferListener transferListener : transferListeners) {
        DataPath sourceDataPath = transferListener.getTransferSourceTarget().getSourceDataPath();
        String source = sourceDataPath.toDataUri().toString();
        if (sourceDataPath.getConnection() instanceof MemoryConnection) {
          source = sourceDataPath.getLogicalName() + "@" + sourceDataPath.getConnection().getName();
        }
        Object[] row = {
          source,
          transferListener.getTransferSourceTarget().getTargetDataPath().toString(),
          DurationShort.create(transferListener.getTimer().getDuration()).toIsoDuration(),
          transferListener.getRowCount(),
          transferListener.getExitStatus() != 0 ? transferListener.getExitStatus() : null,
          Strings.onOneLine(String.join(",", transferListener.getErrorMessages()))
        };
        insertStream.insert(row);
      }
    }
    return result;
  }


}
