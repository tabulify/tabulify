package com.tabulify.transfer;

import java.util.Collections;
import java.util.List;

public class TransferManagerResult {

  @SuppressWarnings("FieldCanBeLocal")
  private final TransferManager transferManager;
  List<TransferListener> transferListeners;

  public TransferManagerResult(TransferManager transferManager, List<TransferListener> transferListeners) {
    this.transferListeners = transferListeners;
    this.transferManager = transferManager;
  }

  public List<TransferListener> getTransferListeners() {
    return transferListeners;
  }

  public int getExitStatus() {

    return transferListeners.stream().mapToInt(TransferListener::getExitStatus).sum();
  }

  public String getError() {

    return String.join(", ",
      transferListeners.stream()
        .map(TransferListener::getErrorMessages)
        .reduce(Collections.emptyList(), (a, b) -> {
          b.addAll(a);
          return b;
        }));

  }

}
