package net.bytle.db.model;


import net.bytle.db.database.DataTypeJdbc;
import net.bytle.db.database.JdbcDataType.DataTypesJdbc;
import net.bytle.db.memory.ListInsertStream;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPaths;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.uri.DataUri;
import net.bytle.fs.Fs;
import net.bytle.type.Maps;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Retrieve a list of TableDef through a Data Definition file
 *
 */
public class DataDefs {


    /**
     * Transform a path (a data definition file or a directory containing dataDefinition file) into a bunch of data path
     *
     * @param path
     * @return
     */
    public static List<DataPath> load(Path path) {

        if (!Files.exists(path)) {
            throw new RuntimeException("The data definition file path (" + path.toAbsolutePath().toString() + " does not exist");
        }

        List<Path> fileDiscovered = new ArrayList<>();
        // if it's a file
        if (Files.isRegularFile(path)) {
            fileDiscovered.add(path);
        } else {
            fileDiscovered.addAll(Fs.getDescendantFiles(path));
        }

        Set<DataPath> dataPaths = new TreeSet<>();
        for (Path filePath : fileDiscovered) {

            List<String> names = Fs.getDirectoryNamesInBetween(filePath, path);
            DataUri dataUri = DataUri.of(String.join("/",names),Fs.getFileName(path).replace("--datadef",""));
            DataPath dataPath = DataPaths.of(dataUri);
            dataPath = readFile(dataPath, filePath);
            dataPaths.add(dataPath);

        }

        // To list
        final ArrayList<DataPath> dataPathsList = new ArrayList<>(dataPaths);
        Collections.sort(dataPathsList);
        return dataPathsList;

    }


    /**
     * Add the meta from a data def file
     *
     * @param path
     * @return the data path with its meta
     */
    private static DataPath readFile(DataPath dataPath, Path path) {

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

        switch (documents.size()) {
            case 0:
                break;
            case 1:

                Map<String, Object> document = documents.get(0);

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
                                String message = "The columns of the data def file (" + path.toString() + ") must be in a map format. ";
                                if (entry.getValue().getClass().equals(java.util.ArrayList.class)) {
                                    message += "They are in a list format. You should suppress the minus if they are present.";
                                }
                                message += "Bad Columns Values are: " + entry.getValue();
                                throw new RuntimeException(message, e);
                            }
                            for (Map.Entry<String, Object> column : columns.entrySet()) {

                                try {
                                    Map<String, Object> columnProperties = (Map<String, Object>) column.getValue();

                                    String type = "varchar";
                                    Object oType = Maps.getPropertyCaseIndependent(columnProperties, "type");
                                    if (oType != null) {
                                        type = (String) oType;
                                    }

                                    DataTypeJdbc dataTypeJdbc = DataTypesJdbc.of(type);

                                    ColumnDef columnDef = dataPath.getDataDef().getColumnOf(column.getKey(), dataTypeJdbc.getClass());
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
                                    String message = "The properties of column (" + column.getKey() + ") from the data def (" + dataPath.toString() + ") must be in a map format. ";
                                    if (column.getValue().getClass().equals(java.util.ArrayList.class)) {
                                        message += "They are in a list format. You should suppress the minus if they are present.";
                                    }
                                    message += "Bad Columns Properties Values are: " + column.getValue();
                                    throw new RuntimeException(message, e);
                                }
                            }
                            break;
                        default:
                            dataPath.getDataDef().addProperty(entry.getKey().toLowerCase(), entry.getValue());
                            break;
                    }
                }
                break;
            default:
                throw new RuntimeException("Too much metadata documents ("+documents.size()+") found in the file ("+path.toString()+") for the dataPath ("+dataPath.toString()+")");
        }
        return dataPath;

    }



    /**
     * Merge the tables property {@link TableDef#getProperty(String)}
     * and the column property {@link ColumnDef#getProperty(String)} into one.
     *
     * The first table has priority.
     * If a property does exist in the first and second table, the first will kept.
     *
     * @param firstTable
     * @param secondTable
     * @return the first table object updated
     */
    public static void mergeProperties(TableDef firstTable, TableDef secondTable) {

        Map<String, Object> firstTableProp = firstTable.getProperties();
        for (Map.Entry<String,Object> entry : secondTable.getProperties().entrySet()){
            if (!firstTableProp.containsKey(entry.getKey())){
                firstTableProp.put(entry.getKey(),entry.getValue());
            }
        }

        for (ColumnDef<?> columnDefFirstTable:firstTable.getColumnDefs()){
            ColumnDef<?> columnSecondTable = secondTable.getColumnDef(columnDefFirstTable.getColumnName());
            if (columnSecondTable!=null){
                Map<String, Object> columnPropertiesFirstTable = columnDefFirstTable.getProperties();
                final Map<String,Object> properties = columnSecondTable.getProperties();
                for (Map.Entry<String,Object> entry : properties.entrySet()){
                    if (!columnPropertiesFirstTable.containsKey(entry.getKey())){
                        columnPropertiesFirstTable.put(entry.getKey(),entry.getValue());
                    }
                }
            }
        }

    }


    public static TableDef of(DataPath dataPath) {

        return dataPath.getDataDef();

    }

    /**
     * Add the columns to the targetDef from the sourceDef
     *
     * @param targetDef
     * @param sourceDef
     */
    public static void addColumns(RelationDef targetDef, RelationDef sourceDef) {

        // Add the columns
        int columnCount = sourceDef.getColumnDefs().size();
        for (int i = 0; i < columnCount; i++) {
            ColumnDef columnDef = sourceDef.getColumnDef(i);
            targetDef.getColumnOf(columnDef.getColumnName(),columnDef.getClass())
                    .typeCode(columnDef.getDataType().getTypeCode())
                    .precision(columnDef.getPrecision())
                    .scale(columnDef.getScale());
        }

    }

    public static void printColumns(TableDef tableDef) {

        DataPath tableStructure = DataPaths.of("structure");
        tableStructure
                .getDataDef()
                .addColumn("#")
                .addColumn("Colum Name")
                .addColumn("Data Type")
                .addColumn("Key")
                .addColumn("Not Null")
                .addColumn("Default")
                .addColumn("Auto Increment")
                .addColumn("Description");

        InsertStream insertStream = Tabulars.getInsertStream(tableStructure);
        int i = 0;
        for (ColumnDef columnDef : tableDef.getColumnDefs()) {
            i++;
            insertStream.insert(
                    i,
                    columnDef.getColumnName(),
                    columnDef.getDataType().getTypeName(),
                    (tableDef.getPrimaryKey().getColumns().contains(columnDef) ? "x" : ""),
                    (columnDef.getNullable() == 0 ? "x" : ""),
                    columnDef.getDefault(),
                    columnDef.getIsAutoincrement(),
                    columnDef.getDescription()

            );
        }
        insertStream.close();


        Tabulars.print(tableStructure);
        Tabulars.drop(tableStructure);
    }
}
