package net.bytle.db.model;


import net.bytle.cli.Log;
import net.bytle.db.DatabasesStore;
import net.bytle.db.DbLoggers;
import net.bytle.db.database.DataTypeJdbc;
import net.bytle.db.database.Database;
import net.bytle.db.database.JdbcDataType.DataTypesJdbc;
import net.bytle.db.engine.Tables;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPaths;
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
 *
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
    public List<DataPath> load(Path path) {

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
            DataPath dataPath = DataPaths.of(database, names.toArray(new String[0]));
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


    public static DataDefs of() {
        return new DataDefs();
    }

    public DataDefs setDatabasesStore(DatabasesStore databasesStore) {

        this.databasesStore = databasesStore;
        return this;

    }

    /**
     * *
     *
     * @param database
     * @return
     */
    public DataDefs setDatabase(Database database) {
        this.database = database;
        return this;
    }


}
