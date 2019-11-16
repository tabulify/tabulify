package net.bytle.db.transfer;

import net.bytle.db.spi.DataPath;

public class Transfer {

    private TransferProperties transferProperties;
    private DataPath sourceDataPath;
    private DataPath targetDataPath;


    public static Transfer of() {
        return new Transfer();
    }

    public DataPath getSourceDataPath() {

        return sourceDataPath;

    }

    public TransferProperties getTransferProperties() {

        return transferProperties;

    }

    public DataPath getTargetDataPath() {
        return targetDataPath;
    }

    public Transfer setSourceDataPath(DataPath sourceDataPath) {
        this.sourceDataPath = sourceDataPath;
        return this;
    }

    public Transfer setTargetDataPath(DataPath targetDataPath) {
        this.targetDataPath = targetDataPath;
        return this;
    }

    public Transfer setTransferProperties(TransferProperties transferProperties) {
        this.transferProperties = transferProperties;
        return this;
    }
}
