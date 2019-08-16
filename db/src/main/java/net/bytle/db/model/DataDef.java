package net.bytle.db.model;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * This class is used to store the data definition of a tabular structure
 *
 * There is only metadata in their. This is the counterpart of {@link java.sql.ResultSetMetaData}
 *
 * The location of the table is a runtime information and is then not part of it.
 *
 * Plugin may used it to add information its yaml file representation
 *
 * For instance, the data loader plugin will add:
 *   * the number of rows to be generated
 *   * a generator for each column that gives the data generation rules
 *
 */
public class DataDef {

    private final String name;

    /**
     * Yaml build object and doesn't check the data type
     * of the map.
     * In the value of the columns properties for instance,
     * if you set string and that you have 1, it will see it as an integer
     */
    public Map<String, Map<String, Object>> Columns = new HashMap<>();

    public DataDef(String name) {
        this.name = name;
    }


    public DataDef add(String columnName, String property, String value) {
        Map<String, Object> properties = Columns.get(columnName);
        if (properties==null){
            properties = new HashMap<>();
            Columns.put(columnName,properties);
        }
        properties.put(property, value);

        return this;
    }

    public String getName() {
        return name;
    }

    public Map<String, Map<String, Object>> getColumns() {
        return Columns;
    }

    public void setColumns(Map<String, Map<String, Object>> columns) {
        this.Columns = columns;
    }


}
