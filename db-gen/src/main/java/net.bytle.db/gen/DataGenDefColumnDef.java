package net.bytle.db.gen;

import net.bytle.db.model.ColumnDef;
import net.bytle.type.Maps;

import java.util.HashMap;
import java.util.Map;

/**
 * A wrapper around a columnDef with set/get of the data generation property
 */
public class DataGenDefColumnDef {


    public static final String GENERATOR_PROPERTY_KEY = "DataGenerator";
    private final ColumnDef columnDef;
    final Map<String,Object> generatorProperties;

    private DataGenDefColumnDef(ColumnDef columnDef) {
        this.columnDef = columnDef;
        // When read from a data definition file into the column property
        final Map<String,Object> generatorColumnProperties = (Map<String, Object>) Maps.getPropertyCaseIndependent(this.columnDef.getProperties(),GENERATOR_PROPERTY_KEY);
        if (generatorColumnProperties != null){
            generatorProperties = generatorColumnProperties;
        } else {
            generatorProperties = new HashMap<>();
            this.columnDef.addProperty(GENERATOR_PROPERTY_KEY,generatorProperties);
        }

    }

    public static DataGenDefColumnDef get(ColumnDef columnDef) {
        return new DataGenDefColumnDef(columnDef);
    }


    public DataGenDefColumnDef setGeneratorType(String generatorType) {
        this.generatorProperties.put("name",generatorType);
        return this;
    }

    public ColumnDef getColumnDef() {
        return columnDef;
    }

    public DataGenDefColumnDef put(String key, Object value) {
        this.generatorProperties.put(key.toLowerCase(),value);
        return this;
    }

    public String getGeneratorName() {
        return (String) Maps.getPropertyCaseIndependent(this.generatorProperties, "name");
    }


    public Map<String,Object> getProperties() {
        return this.generatorProperties;
    }

    public Object getPropertyCaseIndependent(String key) {
        return Maps.getPropertyCaseIndependent(this.generatorProperties,key);
    }
}
