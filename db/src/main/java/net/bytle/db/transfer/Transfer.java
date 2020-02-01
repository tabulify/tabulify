package net.bytle.db.transfer;

import net.bytle.db.engine.Relational;
import net.bytle.db.spi.DataPath;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A transfer may contains more than one source target
 * <p>
 * This was introduced for tpcds. See {@link Relational#getSelectStreamDependency()}
 */
public class Transfer {

  private TransferProperties transferProperties = TransferProperties.of();

  List<TransferSourceTarget> sourceTargets = new ArrayList<>();

  public static Transfer of() {
    return new Transfer();
  }


  public List<TransferSourceTarget> getSourceTargets() {
    return sourceTargets;
  }

  public TransferProperties getTransferProperties() {
    return transferProperties;
  }

  public List<DataPath> getSources() {
    return sourceTargets.stream().map(TransferSourceTarget::getSourceDataPath).collect(Collectors.toList());
  }

  public Transfer addSourceTargetDataPath(TransferSourceTarget transferSourceTarget) {
    this.sourceTargets.add(transferSourceTarget);
    return this;
  }

  @Override
  public String toString() {
    return sourceTargets.toString();
  }

}
