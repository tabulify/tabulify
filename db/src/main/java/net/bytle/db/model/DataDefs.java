package net.bytle.db.model;


import net.bytle.db.database.DataTypeJdbc;
import net.bytle.db.database.JdbcDataType.DataTypesJdbc;
import net.bytle.db.engine.DataTypes;
import net.bytle.db.engine.Tables;
import net.bytle.type.Maps;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Retrieve a list of TableDef through a Data Definition file
 */
public class DataDefs {


    public static List<TableDef> load(Path path) {

        InputStream input;
        try {
            input = Files.newInputStream(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Transform the file in properties
        Yaml yaml = new Yaml();

        // Every document is one dataDef
        List<Map<String, Object>> documents = new ArrayList<>();
        for (Object data : yaml.loadAll(input)) {
            Map<String, Object> document;
            try {
                document = (Map<String, Object>) data;
            } catch (ClassCastException e) {
                String message = "A data Def must be in a map format. ";
                if (data.getClass().equals(java.util.ArrayList.class)) {
                    message += "They are in a list format. You should suppress the minus if they are present.";
                }
                message += "The Bad Data Def Values are: " + data;
                throw new RuntimeException(message, e);
            }
            documents.add(document);
        }
        List<TableDef> tableDefs = new ArrayList<>();
        switch (documents.size()) {
            case 0:
                break;
            default:
                for (int i = 0; i < documents.size(); i++) {

                    Map<String, Object> document = documents.get(i);

                    // Create the dataDef
                    Object nameAsObject = Maps.getPropertyCaseIndependent(document, "name");
                    String name;
                    if (nameAsObject == null) {
                        final String fileName = path.getFileName().toString();
                        name = fileName.substring(0, fileName.indexOf("."));
                    } else {
                        name = (String) nameAsObject;
                    }
                    TableDef dataDef = Tables.get(name);
                    tableDefs.add(dataDef);

                    // Loop through all other properties
                    for (Map.Entry<String, Object> entry : document.entrySet()) {

                        switch (entry.getKey().toLowerCase()) {
                            case "name":
                                continue;
                            case "columns":
                                Map<String, Object> columns;
                                try {
                                    columns = (Map<String, Object>) entry.getValue();
                                } catch (ClassCastException e) {
                                    String message = "The columns of the data def (" + name + ") must be in a map format. ";
                                    if (entry.getValue().getClass().equals(java.util.ArrayList.class)) {
                                        message += "They are in a list format. You should suppress the minus if they are present.";
                                    }
                                    message += "Bad Columns Values are: " + nameAsObject;
                                    throw new RuntimeException(message, e);
                                }
                                for (Map.Entry<String, Object> column : columns.entrySet()) {

                                    try {
                                        Map<String, Object> columnProperties = (Map<String, Object>) column.getValue();

                                        String type = "varchar";
                                        Object oType = Maps.getPropertyCaseIndependent(columnProperties, "type");
                                        if (oType!=null){
                                            type = (String) oType;
                                        }

                                        DataTypeJdbc dataTypeJdbc = DataTypesJdbc.of(type);

                                        ColumnDef columnDef = dataDef.getColumnOf(column.getKey(),dataTypeJdbc.getClass());
                                        for (Map.Entry<String, Object> columnProperty : columnProperties.entrySet()) {
                                            switch (columnProperty.getKey().toLowerCase()) {
                                                case "type":
                                                    columnDef.typeCode(dataTypeJdbc.getTypeCode());
                                                    break;
                                                case "precision":
                                                    columnDef.precision((Integer) columnProperty.getValue());
                                                    break;
                                                case "scale":
                                                    columnDef.scale((Integer) columnProperty.getValue());
                                                    break;
                                                case "comment":
                                                    columnDef.comment((String) columnProperty.getValue());
                                                    break;
                                                case "nullable":
                                                    columnDef.setNullable(Boolean.valueOf((String) columnProperty.getValue()));
                                                    break;
                                                default:
                                                    columnDef.addProperty(columnProperty.getKey(), columnProperty.getValue());
                                                    break;
                                            }
                                        }

                                    } catch (ClassCastException e) {
                                        String message = "The properties of column (" + column.getKey() + ") from the data def (" + name + ") must be in a map format. ";
                                        if (column.getValue().getClass().equals(java.util.ArrayList.class)) {
                                            message += "They are in a list format. You should suppress the minus if they are present.";
                                        }
                                        message += "Bad Columns Properties Values are: " + column.getValue();
                                        throw new RuntimeException(message, e);
                                    }
                                }
                                break;
                            default:
                                dataDef.addProperty(entry.getKey().toLowerCase(), entry.getValue());
                                break;
                        }
                    }
                }
                break;
        }
        return tableDefs;

    }

    private static Object getCaseInsensitiveKey(Map<String, Object> maps, String insensitiveKey) {
        String sensitiveKeyInMap = null;
        for (String key : maps.keySet()) {
            if (key.toLowerCase().equals(insensitiveKey.toLowerCase())) {
                sensitiveKeyInMap = key;
                break;
            }
        }
        return maps.get(sensitiveKeyInMap);
    }



}
