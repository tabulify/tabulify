package com.tabulify.transfer;


import com.tabulify.spi.DataPath;
import com.tabulify.spi.Tabulars;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


/**
 * A transfer manager that
 * * pertains the state between transfer
 * * and manages the Transfer
 * <p>
 * * You set your properties via {@link TransferManagerBuilder#setTransferCrossProperties(TransferPropertiesCross)}
 * * You say if you also want to transfer the source foreign table via {@link TransferManagerBuilder#withDependency(boolean)}
 * <p>
 * You build and you {@link #createOrder(List)}
 */
public class TransferManager {

    public static final Logger LOGGER = Logger.getLogger(TransferManager.class.getName());

    private final TransferManagerBuilder transferManagerBuilder;


    static public TransferManagerBuilder builder() {
        return new TransferManagerBuilder();
    }

    /**
     * A list of target already seen in a transfer (ie ordered)
     * <p>
     * Why ? the operation where the target operation have already run against
     * the target
     * Ie if we have several transfer with the same target
     * We create, truncate the target only once (not for each transfer)
     * To avoid deleting multiple time a target
     * on a concat operation
     * Cardinality being pretty low
     */
    final List<DataPath> targetsSeenInOrders = new ArrayList<>();


    /**
     * A utility function to start only one transfer
     *
     * @param source - the source
     * @param target - the target
     * @return the transfer listener
     */
    public static TransferListener transfer(DataPath source, DataPath target) {
        TransferManagerResult start = builder().build().createOrder(source, target).execute();
        return start.getTransferListeners().get(0);
    }

    public static TransferManager buildDefault() {
        return TransferManager.builder().build();
    }


    public TransferManagerOrder createOrder(List<TransferSourceTarget> transfersList) {

        return new TransferManagerOrder(this, transfersList);

    }


    private TransferManager(TransferManagerBuilder transferManagerBuilder) {
        this.transferManagerBuilder = transferManagerBuilder;
    }


    /**
     * @param source - the source of the transfer
     * @param target - if the target is a container, the target will become a child of it with the name of the source
     * @return the object for chaining
     */
    public TransferManagerOrder createOrder(DataPath source, DataPath target) {
        assert source != null : "The source cannot be null in a transfer";
        assert target != null : "The target cannot be null in a transfer";

        return createOrder(createSourceTarget(source, target));
    }


    public TransferManagerOrder createOrder(List<? extends DataPath> sources, DataPath target) {
        List<TransferSourceTarget> transferSourceTargetList = new ArrayList<>();
        for (DataPath sourceDataPath : sources) {
            transferSourceTargetList.add(createSourceTarget(sourceDataPath, target));
        }
        return createOrder(transferSourceTargetList);
    }

    /**
     * Private utility class to handle the case when the target is a directory
     *
     * @param sourceDataPath - the source
     * @param target         - the target
     * @return the source target
     */
    private TransferSourceTarget createSourceTarget(DataPath sourceDataPath, DataPath target) {

        if (Tabulars.isContainer(target)) {
            DataPath targetFromContainer = target.getConnection().getDataSystem().getTargetFromSource(sourceDataPath, null, target);
            return TransferSourceTarget.create(sourceDataPath, targetFromContainer);
        }
        return TransferSourceTarget.create(sourceDataPath, target);
    }


    public TransferManagerOrder createOrder(TransferSourceTarget transferSourceTarget) {
        return createOrder(List.of(transferSourceTarget));
    }

    public TransferManagerBuilder getMeta() {
        return this.transferManagerBuilder;
    }


    public static class TransferManagerBuilder {

        /**
         * The transfer properties for all transfers
         * on a {@link TransferSourceTargetOrder}
         */
        TransferPropertiesCross transferPropertiesCross = TransferPropertiesCross.create();

        /**
         * If the select stream can only be generated
         * after another, this select stream is dependent
         */
        boolean withDependencies = false;
        TransferPropertiesSystem.TransferPropertiesSystemBuilder transferPropertiesSystemBuilder;

        public TransferManagerBuilder setTransferCrossProperties(TransferPropertiesCross transferPropertiesCross) {
            this.transferPropertiesCross = transferPropertiesCross;
            return this;
        }

        /**
         * If we try to load from a select stream that is dependent on another
         * the dependent select stream will also be loaded, created
         * otherwise an error is thrown
         *
         * @param b - with or without dependency
         * @return the object for chaining
         */
        public TransferManagerBuilder withDependency(boolean b) {
            this.withDependencies = b;
            return this;
        }

        public TransferManager build() {
            if (transferPropertiesSystemBuilder == null) {
                transferPropertiesSystemBuilder = TransferPropertiesSystem.builder();
            }
            return new TransferManager(this);
        }

        public TransferManagerBuilder setTransferPropertiesSystem(TransferPropertiesSystem.TransferPropertiesSystemBuilder transferPropertiesSystem) {
            this.transferPropertiesSystemBuilder = transferPropertiesSystem;
            return this;
        }
    }
}
