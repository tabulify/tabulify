package net.bytle.db.spi;

import net.bytle.db.DbLoggers;
import net.bytle.db.engine.Dag;
import net.bytle.db.engine.Relations;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.stream.Streams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class Tabulars {


    public synchronized static Boolean exists(DataPath dataPath) {

        return dataPath.getDataSystem().exists(dataPath);

    }

    public static SelectStream getSelectStream(DataPath dataPath) {
        return dataPath.getDataSystem().getSelectStream(dataPath);
    }

    public static SelectStream getSelectStream(DataPath dataPath, String query) {
        return dataPath.getDataSystem().getSelectStream(query);
    }

    public static DataPath create(DataPath dataPath) {

        return dataPath.getDataSystem().create(dataPath);
    }

    /**
     * @param dataPaths - a list of data path
     * @return return a independent set of data path (ie independent of foreign key outside of the set)
     * <p>
     * (ie delete the foreign keys of a table if the foreign table is not part of the set)
     */
    public static List<DataPath> atomic(List<DataPath> dataPaths) {
        for (DataPath dataPath : dataPaths) {
            List<ForeignKeyDef> foreignKeys = dataPath.getDataDef().getForeignKeys();
            for (ForeignKeyDef foreignKeyDef : foreignKeys) {
                if (!(dataPaths.contains(foreignKeyDef.getForeignPrimaryKey().getDataDef().getDataPath()))) {
                    dataPath.getDataDef().deleteForeignKey(foreignKeyDef);
                }
            }
        }
        return dataPaths;
    }

    /**
     * @param dataPath - a data path container (a directory, a schema or a catalog)
     * @return the children data paths representing sql tables, schema or files
     */
    public static List<DataPath> getChildrenDataPath(DataPath dataPath) {

        return dataPath.getDataSystem().getChildrenDataPath(dataPath);

    }

    /**
     * Create all data object if they don't exist
     * taking into account the foreign key constraints
     * <p>
     *
     * @param dataPaths
     */
    public static void createIfNotExist(List<DataPath> dataPaths) {

        Dag dag = Dag.get(dataPaths);
        dataPaths = dag.getCreateOrderedTables();
        for (DataPath dataPath : dataPaths) {
            createIfNotExist(dataPath);
        }

    }

    public static boolean isContainer(DataPath dataPath) {
        return dataPath.getDataSystem().isContainer(dataPath);
    }

    /**
     * Create the table in the database if it doesn't exist
     *
     * @param dataPath
     */
    public static void createIfNotExist(DataPath dataPath) {

        if (!exists(dataPath)) {

            create(dataPath);

        } else {

            DbLoggers.LOGGER_DB_ENGINE.fine("The data object (" + dataPath.toString() + ") already exist.");

        }

    }

    public static void drop(DataPath... dataPaths) {

        Dag dag = Dag.get(Arrays.asList(dataPaths));
        for (DataPath dataPath : dag.getDropOrderedTables()) {
            dataPath.getDataSystem().drop(dataPath);
        }

    }

    /**
     * Drop one or more tables
     * <p>
     * If the table is a foreign table, the child constraint will
     * prevent the table to be dropped if the child table is not given.
     * <p>
     *
     * @param dataPaths - The tables to drop
     */
    public static void drop(List<DataPath> dataPaths) {

        drop(dataPaths.toArray(new DataPath[0]));

    }

    /**
     * Suppress all rows of the table
     *
     * @param dataPath - the tableDef where to suppress all rows
     */
    public static void delete(DataPath dataPath) {

        dataPath.getDataSystem().delete(dataPath);

    }

    public static void dropIfExists(DataPath... dataPaths) {

        dropIfExists(Arrays.asList(dataPaths));

    }


    /**
     * Drop the table from the database if exist
     * and drop the table from the cache
     *
     * @param dataPaths
     */
    public static void dropIfExists(List<DataPath> dataPaths) {

        for (DataPath dataPath : Dag.get(dataPaths).getDropOrderedTables()) {
            if (exists(dataPath)) {
                drop(dataPath);
            }
        }

    }

    public static void truncate(DataPath dataPath) {
        dataPath.getDataSystem().truncate(dataPath);
    }


    /**
     * Print the data of a table
     *
     * @param dataPath
     */
    public static void print(DataPath dataPath) {

        SelectStream tableOutputStream = getSelectStream(dataPath);
        Streams.print(tableOutputStream);
        tableOutputStream.close();

    }

    public static InsertStream getInsertStream(DataPath dataPath) {
        return dataPath.getDataSystem().getInsertStream(dataPath);
    }

    public static List<DataPath> move(List<DataPath> sources, DataPath target) {

        List<DataPath> targetDataPaths = new ArrayList<>();
        for(DataPath sourceDataPath :Dag.get(sources).getCreateOrderedTables()){
            DataPath targetDataPath = target.getDataSystem().getDataPath(sourceDataPath.getName());
            Tabulars.move(sourceDataPath,targetDataPath);
            targetDataPaths.add(targetDataPath);
        }

        return targetDataPaths;

    }

    public static DataPath move(DataPath source, DataPath target) {

        if (source.getDataSystem() == target.getDataSystem()) {
            // same provider
            source.getDataSystem().move(source, target);
        } else {
            // different providers
            final Boolean exists = Tabulars.exists(target);
            if (!exists) {
                if (target.getDataDef() == null) {
                    Relations.addColumns(source.getDataDef(), target.getDataDef());
                }
                Tabulars.create(target);
            } else {
                for (ColumnDef columnDef: source.getDataDef().getColumnDefs()){
                    ColumnDef targetColumnDef =  target.getDataDef().getColumnDef(columnDef.getColumnName());
                    if (targetColumnDef==null){
                        String message = "Unable to move the data unit ("+source.toString()+") because it exists already in the target location ("+target.toString()+") with a different structure" +
                                " (The source column ("+columnDef.getColumnName()+") was not found in the target data unit)";
                        DbLoggers.LOGGER_DB_ENGINE.severe(message);
                        throw new RuntimeException(message);
                    }
                }
            }
            try (
                    SelectStream sourceSelectStream = Tabulars.getSelectStream(source);
                    InsertStream targetInsertStream = Tabulars.getInsertStream(target)
            ) {
                while (sourceSelectStream.next()) {
                    List<Object> objects = IntStream.of(sourceSelectStream.getDataDef().getColumnDefs().size())
                            .mapToObj(sourceSelectStream::getObject)
                            .collect(Collectors.toList());
                    targetInsertStream.insert(objects);
                }
            }
        }
        return target;

    }


}
