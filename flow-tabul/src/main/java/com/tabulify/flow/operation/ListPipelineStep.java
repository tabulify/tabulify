package com.tabulify.flow.operation;

import com.tabulify.DbLoggers;
import com.tabulify.TabularLogLevel;
import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.flow.engine.*;
import com.tabulify.flow.stream.PipelineReduceOperation;
import com.tabulify.model.RelationDef;
import com.tabulify.model.SqlDataTypeAnsi;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataPathAttribute;
import com.tabulify.spi.SelectException;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.exception.CastException;
import com.tabulify.exception.NoVariableException;
import com.tabulify.type.Casts;
import com.tabulify.type.Enums;
import com.tabulify.type.KeyNormalizer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The list operation.
 * <p>
 * A function that takes a list of data path
 * and collect their attributes in a data path
 * <p>
 * The implementation is a little bit weird because it implements {@link PipelineReduceOperation}
 * It is a rest of the implementation of pipeline as Java Functional Programming
 */
public class ListPipelineStep extends PipelineStepIntermediateManyToManyAbs implements PipelineStep, PipelineReduceOperation {


    public static final String DEFAULT_TARGET_LOGICAL_NAME = "data_resource_list";
    protected final ListPipelineStepBuilder listBuilder;
    /**
     * The identity is the collector
     */
    private DataPath identity;
    private List<DataPath> acceptedDataPathList = new ArrayList<>();

    public ListPipelineStep(ListPipelineStepBuilder listBuilder) {
        super(listBuilder);
        this.listBuilder = listBuilder;

        // Lambda are built before constructor
        // because we need listBuilder, we build it here
        accumulator = (stream, dp) -> {
            List<Object> row = new ArrayList<>();
            for (KeyNormalizer attribute : this.listBuilder.getListAttributes()) {
                try {
                    row.add(dp.getAttribute(attribute).getValueOrDefault());
                } catch (NoVariableException e) {
                    // ok
                    row.add(null);
                }
            }
            stream.insert(row);
        };

        identity = buildIdentity();

    }

    private DataPath buildIdentity() {
        DataPath identity = this.getTabular()
                .getMemoryConnection()
                .getAndCreateRandomDataPath("list-")
                .setLogicalName(listBuilder.getTargetLogicalName())
                .setComment(listBuilder.getTargetDescription());

        RelationDef accumulatorRelationDef = identity.getOrCreateRelationDef();
        boolean countPresent = false;
        Set<KeyNormalizer> attributesAlreadySeen = new HashSet<>();
        List<KeyNormalizer> listAttributes = listBuilder.getListAttributes();
        for (int i = 0; i < listAttributes.size(); i++) {
            KeyNormalizer attribute = listAttributes.get(i);
            KeyNormalizer columnName;
            if (!attributesAlreadySeen.contains(attribute)) {
                columnName = attribute;
            } else {
                columnName = KeyNormalizer.createSafe(attribute + "_" + i);
            }
            attributesAlreadySeen.add(attribute);
            DataPathAttribute dataPathAttribute;
            try {
                dataPathAttribute = Casts.cast(attribute, DataPathAttribute.class);
            } catch (CastException e) {
                // may be a dynamic attribute (backref reference from a regexp, ...)
                accumulatorRelationDef.addColumn(columnName.toSqlCase(), SqlDataTypeAnsi.CHARACTER_VARYING);
                continue;
            }
            switch (dataPathAttribute) {
                case COUNT:
                    accumulatorRelationDef.addColumn(columnName.toSqlCase(), SqlDataTypeAnsi.INTEGER);
                    countPresent = true;
                    break;
                case SIZE:
                    accumulatorRelationDef.addColumn(columnName.toSqlCase(), SqlDataTypeAnsi.INTEGER);
                    break;
                default:
                    accumulatorRelationDef.addColumn(columnName.toSqlCase(), SqlDataTypeAnsi.CHARACTER_VARYING);
            }
        }
        if (!countPresent) {
            DbLoggers.LOGGER_DB_ENGINE.log(
                    TabularLogLevel.TIP.getLevel(),
                    "You can add the `count` attribute to see the number of records. Example: `-a name -a count`"
            );
        }
        return identity;
    }

    public static ListPipelineStepBuilder builder() {
        return new ListPipelineStepBuilder();
    }


    /**
     * Accumulator function: adds a DataPath element to the collector
     */
    @Override
    public DataPath accumulator(DataPath collector, DataPath element) {
        List<Object> row = new ArrayList<>();
        for (KeyNormalizer attribute : listBuilder.getListAttributes()) {
            try {
                row.add(element.getAttribute(attribute).getValueOrDefault());
            } catch (NoVariableException e) {
                // ok
                row.add(null);
            }
        }
        collector.getInsertStream().insert(row);
        return collector;
    }


    /**
     * The accumulator function
     */
    protected BiConsumer<InsertStream, DataPath> accumulator;

    /**
     * The finisher function that
     * transform the stream in data path
     */
    Function<InsertStream, DataPath> finisher = (stream) -> {
        stream.close();
        return stream.getDataPath();
    };


    @Override
    public void accept(DataPath dataPath) {

        this.accumulator.accept(identity().getInsertStream(), dataPath);
        this.acceptedDataPathList.add(dataPath);

    }


    @Override
    public DataPath identity() {
        return identity;
    }


    /**
     * Combiner function: combines two collectors into one
     */
    @Override
    public DataPath combiner(DataPath collector1, DataPath collector2) {
        InsertStream insertStream = collector2.getInsertStream();
        try (SelectStream selectStream = collector1.getSelectStream()) {
            while (selectStream.next()) {
                insertStream.insert(selectStream.getObjects());
            }
        } catch (SelectException e) {
            throw new RuntimeException(e);
        }
        return collector2;
    }

    @Override
    public PipelineStepSupplierDataPath get() {

        DataPath apply = finisher.apply(identity().getInsertStream());

        DefinePipelineStep.DefinePipelineStepBuilder builder = DefinePipelineStep.builder();
        /**
         * Don't send back empty memory data path
         */
        if (apply.getCount() != 0) {
            builder.addDataPath(apply);
        }
        return (PipelineStepSupplierDataPath) builder
                .setIntermediateSupplier(this)
                .setPipeline(this.getPipeline())
                .build();

    }

    @Override
    public void reset() {
        identity = buildIdentity();
        this.acceptedDataPathList = new ArrayList<>();
    }

    @Override
    public List<DataPath> getDataPathsBuffer() {
        return this.acceptedDataPathList;
    }

    public static class ListPipelineStepBuilder extends PipelineStepBuilderTarget {
        static final KeyNormalizer LIST = KeyNormalizer.createSafe("list");

        private List<KeyNormalizer> listAttributes = new ArrayList<>();
        private String targetLogicalName = DEFAULT_TARGET_LOGICAL_NAME;
        private String targetDescription = "";

        @Override
        public ListPipelineStepBuilder createStepBuilder() {
            return new ListPipelineStepBuilder();
        }

        @Override
        public List<Class<? extends ArgumentEnum>> getArgumentEnums() {
            return List.of(ListPipelineStepArgument.class);
        }

        public ListPipelineStepBuilder setTargetLogicalName(String logicalName) {
            this.targetLogicalName = logicalName;
            return this;
        }

        public ListPipelineStepBuilder setTargetDescription(String targetDescription) {
            this.targetDescription = targetDescription;
            return this;
        }

        @Override
        public ListPipelineStep build() {
            if (this.listAttributes.isEmpty()) {
                this.listAttributes = Stream.of(DataPathAttribute.DATA_URI, DataPathAttribute.MEDIA_TYPE)
                        .map(KeyNormalizer::createSafe)
                        .collect(Collectors.toList());
            }
            return new ListPipelineStep(this);
        }

        public ListPipelineStepBuilder setDataAttributes(List<KeyNormalizer> attributes) {
            this.listAttributes = attributes;
            return this;
        }


        @Override
        public ListPipelineStepBuilder setArgument(KeyNormalizer key, Object value) {

            ListPipelineStepArgument selectArgument;
            try {
                selectArgument = Casts.cast(key, ListPipelineStepArgument.class);
            } catch (CastException e) {
                throw new IllegalArgumentException("The argument (" + key + ") is not a valid argument for the step (" + this + "). You can choose one of: " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(ListPipelineStepArgument.class));
            }
            Attribute attribute;
            try {
                attribute = this.getPipeline().getTabular().getVault()
                        .createVariableBuilderFromAttribute(selectArgument)
                        .setOrigin(Origin.PIPELINE)
                        .build(value);
                this.setArgument(attribute);
            } catch (CastException e) {
                throw new IllegalArgumentException("The " + selectArgument + " value (" + value + ") of the step (" + this + ") is not conform . Error: " + e.getMessage(), e);
            }

            switch (selectArgument) {
                case ATTRIBUTES:
                    try {
                        this.setDataAttributes(Casts.castToNewList(value, KeyNormalizer.class));
                    } catch (CastException e) {
                        throw new IllegalArgumentException("The " + selectArgument + " value (" + value + ") of the step (" + this + ") is not conform . Error: " + e.getMessage(), e);
                    }
                    break;
            }
            return this;
        }

        @Override
        public KeyNormalizer getOperationName() {
            return LIST;
        }

        public List<KeyNormalizer> getListAttributes() {
            return this.listAttributes;
        }

        @Override
        public ListPipelineStepBuilder setPipeline(Pipeline pipeline) {
            return (ListPipelineStepBuilder) super.setPipeline(pipeline);
        }

        public String getTargetLogicalName() {
            return this.targetLogicalName;
        }

        public String getTargetDescription() {
            return this.targetDescription;
        }
    }
}
