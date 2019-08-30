package net.bytle.db.model;


import net.bytle.cli.Log;
import net.bytle.db.DatabasesStore;
import net.bytle.db.DbLoggers;
import net.bytle.db.database.DataTypeJdbc;
import net.bytle.db.database.Database;
import net.bytle.db.database.JdbcDataType.DataTypesJdbc;
import net.bytle.db.engine.Tables;
import net.bytle.fs.Fs;
import net.bytle.type.Maps;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Retrieve a list of TableDef through a Data Definition file
 * TODO: The package is surely not the good one here
 */
public class DataDefs {

    static Log LOGGER = DbLoggers.LOGGER_DB_ENGINE;

    private DatabasesStore databasesStore;
    private Database database;

    /**
     * Transform a path (a data definition file or a directory containing dataDefinition file) into a bunch of TableDef
     * <p>
     * The schema and database are set accordingly to this rule:
     * * one child directory: the directory is the schema name
     * * two child directories: the first directory is the database name, the second the schema name
     *
     * @param path
     * @return
     */
    public List<TableDef> load(Path path) {

        if (!Files.exists(path)) {
            throw new RuntimeException("The data definition file path (" + path.toAbsolutePath().toString() + " does not exist");
        }

        List<Path> fileDiscovered = new ArrayList<>();
        // if it's a file
        if (Files.isRegularFile(path)) {
            fileDiscovered.add(path);
        } else {
            fileDiscovered.addAll(Fs.getChildFiles(path));
        }

        Set<TableDef> tableDefsToReturn = new TreeSet<>();
        for (Path filePath : fileDiscovered) {

            List<TableDef> tableDefsFromDataFile = readFile(filePath);
            List<String> names = Fs.getDirectoryNamesInBetween(filePath, path);

            switch (names.size()) {
                case 0:
                    tableDefsToReturn.addAll(tableDefsFromDataFile);
                    break;
                case 1:
                    if (Files.isDirectory(path)) {
                        // Only schema, the database should not be null
                        final String schemaName = filePath.getParent().getFileName().toString();
                        if (database == null) {
                            LOGGER.severe("The data definition to load (" + filePath + ") has a file path that is one level deep compared to the base directory (" + path + "). It defines therefore only schema (" + schemaName + "), no database name.");
                            LOGGER.severe("The base directory should be define one level deep or the database should be given programmatically.");
                            throw new RuntimeException("The database should not be null for the path (" + path + ") in order to define the schema (" + schemaName + ") of the child file (" + filePath + ").");
                        }
                        SchemaDef schemaDef = database.getSchema(schemaName);
                        tableDefsToReturn.addAll(tableDefsFromDataFile.stream()
                                .map(s -> s.setSchema(schemaDef))
                                .collect(Collectors.toList()
                                ));
                    } else {
                        tableDefsToReturn.addAll(tableDefsFromDataFile);
                    }
                    break;
                case 2:
                    final String schemaNameCase2 = names.get(1);
                    final String databaseName = names.get(0);
                    if (databasesStore == null) {
                        throw new RuntimeException("The database store should not be null in order to get the database (" + databaseName + "). The data definition to load (" + filePath + ") has a file path that is one level deep compared to the base directory (" + path + "). It defines therefore a schema (" + schemaNameCase2 + ") and a database name (" + databaseName + ")");
                    }
                    Database databaseCase2 = databasesStore.getDatabase(databaseName);
                    SchemaDef schemaDefCas2 = databaseCase2.getSchema(schemaNameCase2);
                    tableDefsToReturn.addAll(tableDefsFromDataFile.stream()
                            .map(s -> s.setSchema(schemaDefCas2))
                            .collect(Collectors.toList()
                            ));
                    break;
                default:
                    throw new RuntimeException("The data definition to load (" + filePath + ") has a file path that is more than two levels deep compared to the base directory (" + path + ") and it should not be more than 2 to define schema and database");
            }


        }

        // To list
        return new ArrayList<>(tableDefsToReturn);
    }


    /**
     * Transform a data definition file into one or more data definition file
     *
     * @param path
     * @return a list of tableDef
     */
    private static List<TableDef> readFile(Path path) {

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
                                        if (oType != null) {
                                            type = (String) oType;
                                        }

                                        DataTypeJdbc dataTypeJdbc = DataTypesJdbc.of(type);

                                        ColumnDef columnDef = dataDef.getColumnOf(column.getKey(), dataTypeJdbc.getClass());
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


    public static DataDefs of() {
        return new DataDefs();
    }

    public DataDefs setDatabasesStore(DatabasesStore databasesStore) {

        this.databasesStore = databasesStore;
        return this;

    }

    /**
     *      *
     * @param database
     * @return
     */
    public DataDefs setDatabase(Database database) {
        this.database = database;
        return this;
    }


}
