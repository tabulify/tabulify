package com.tabulify.transfer;

import com.tabulify.DbLoggers;
import com.tabulify.Tabular;
import com.tabulify.memory.MemoryConnection;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import net.bytle.log.Log;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Static Utility function
 */
public class Transfers {


  /**
   * The main entry property where the signature is what a user should enter
   *
   * @param tabular the tabular context
   * @param sourceTargets a source target map
   * @param transferProperties the transfer properties to apply
   * @return the transfer listener
   */
  public static List<TransferListener> transfers(
    Tabular tabular,
    Map<? extends DataPath, ? extends DataPath> sourceTargets,
    TransferProperties transferProperties
  ) {

    /**
     * Start building the transferManager object
     */
    TransferManager transferManager = TransferManager.create()
      .setTransferProperties(transferProperties);

    /**
     * Add the transfers
     */
    for (Map.Entry<? extends DataPath, ? extends DataPath> sourceTarget : sourceTargets.entrySet()) {
      transferManager.addTransfer(sourceTarget.getKey(), sourceTarget.getValue());
    }


    /**
     * Start
     */
    List<TransferListener> transferListeners = transferManager.run().getTransferListeners();

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

  public static Set<DataPath> transfersListenersToDataPath(Tabular tabular, List<TransferListener> transferListeners) {
    DataPath result = tabular.getAndCreateRandomMemoryDataPath();
    result.getOrCreateRelationDef()
      .addColumn("Source")
      .addColumn("Target")
      .addColumn("Latency (ms)")
      .addColumn("Row Count")
      .addColumn("Error")
      .addColumn("Message");
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
          transferListener.getTimer().getResponseTimeInMilliSeconds(),
          transferListener.getRowCount(),
          transferListener.getExitStatus() != 0 ? transferListener.getExitStatus() : "",
          Log.onOneLine(String.join(",", transferListener.getErrorMessages()))
        };
        insertStream.insert(row);
      }
    }
    return Collections.singleton(result);
  }


}
