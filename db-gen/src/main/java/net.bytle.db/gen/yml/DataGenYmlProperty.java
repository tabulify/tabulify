package net.bytle.db.gen.yml;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * This class is used to read the YAML file
 *
 * #####################################################################################################################
 * Their must be a 1 on 1 matching between the name of the variables and the name of the property in the YAML file
 * Don't change any variable name
 * #####################################################################################################################
 *
 */
public class DataGenYmlProperty {

    public String Table;
    public String Schema;
    public Integer Rows;

    /**
     * Yaml build object and doesn't check the data type
     * of the map.
     * In the value of the columns properties for instance,
     * if you set string and that you have 1, it will see it as an integer
     */
    public Map<String, Map<String, Object>> Columns = new HashMap<>();

    public DataGenYmlProperty() {

    }

    public DataGenYmlProperty schema(String schema) {
        this.Schema = schema;
        return this;
    }

    public DataGenYmlProperty add(String columnName, String property, String value) {
        Map<String, Object> properties = Columns.get(columnName);
        if (properties==null){
            properties = new HashMap<>();
            Columns.put(columnName,properties);
        }
        properties.put(property, value);

        return this;
    }

    public String getTable() {
        return Table;
    }

    public String getSchema() {
        return Schema;
    }

    public Map<String, Map<String, Object>> getColumns() {
        return Columns;
    }

    public void setTable(String table) {
        this.Table = table;
    }

    public void setSchema(String schema) {
        this.Schema = schema;
    }

    public void setColumns(Map<String, Map<String, Object>> columns) {
        this.Columns = columns;
    }

    public Integer getRows() {
        return Rows;
    }

    public void setRows(Integer rows) {
        this.Rows = rows;
    }

}
