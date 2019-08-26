package net.bytle.db.gen;

import net.bytle.db.model.ColumnDef;
import net.bytle.type.Maps;


import java.util.Map;

/**
 * A wrapper around a columnDef that will build and link the data generator
 */
public class DataGenDefColumnDef<T> {

    private final ColumnDef<T> columnDef;

    /**
     * The properties key in a DataDefinition file
     */
    public static final String GENERATOR_PROPERTY_KEY = "DataGenerator";

    /**
     * The data generator
     */
    private DataGenerator dataGenerator;

    /**
     *
     *
     * @param columnDef
     */
    private DataGenDefColumnDef(DataGenDef dataGenDef, ColumnDef<T> columnDef) {

        this.columnDef = columnDef;

        // When read from a data definition file into the column property
        final Object generatorProperty = Maps.getPropertyCaseIndependent(columnDef.getProperties(), GENERATOR_PROPERTY_KEY);
        if (generatorProperty != null) {

            final Map<String, Object> generatorColumnProperties;
            try {
                generatorColumnProperties = (Map<String, Object>) generatorProperty;
            } catch (ClassCastException e) {
                throw new RuntimeException("The values of the property (" + GENERATOR_PROPERTY_KEY + ") for the column (" + columnDef.getFullyQualifiedName() + ") should be a map value. Bad values:" + generatorProperty);
            }

            final String nameProperty = (String) Maps.getPropertyCaseIndependent(generatorColumnProperties, "name");
            if (nameProperty == null) {
                throw new RuntimeException("The name property of the generator was not found within the property (" + GENERATOR_PROPERTY_KEY + ") of the column " + columnDef.getFullyQualifiedName() + ".");
            }
            String name = nameProperty.toLowerCase();
            switch (name) {
                case "sequence":
                    dataGenerator = SequenceGenerator.of(columnDef);
                case "unique":
                    dataGenerator = SequenceGenerator.of(columnDef);
                case "derived":
//                    dataGenerator = DerivedGenerator.of(columnDef, generatorColumnProperties, dataGeneration);
                case "random":
                    dataGenerator = DistributionGenerator.of(columnDef, generatorColumnProperties);
                case "distribution":
                    dataGenerator = DistributionGenerator.of(columnDef, generatorColumnProperties);
                default:
                    throw new RuntimeException("The generator (" + name + ") defined for the column (" + columnDef.getFullyQualifiedName() + ") is unknown");
            }

        }

    }

    public static <T> DataGenDefColumnDef<T> of(DataGenDef dataGenDef, ColumnDef<T> columnDef) {
        return new DataGenDefColumnDef<>(dataGenDef, columnDef);
    }




    public ColumnDef<T> getColumnDef() {
        return columnDef;
    }


    public DataGenDefColumnDef setGenerator(DataGenerator dataGenerator) {
        this.dataGenerator = dataGenerator;
        return this;
    }

    public DataGenerator getDataGenerator() {
        return dataGenerator;
    }
}
