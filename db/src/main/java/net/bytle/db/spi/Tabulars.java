package net.bytle.db.spi;

import net.bytle.db.DbLoggers;
import net.bytle.db.engine.Dag;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.DataDefs;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.transfer.TransferProperties;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.transfer.TransferListener;
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
        if (Tabulars.exists(dataPath)) {
            return dataPath.getDataSystem().getSelectStream(dataPath);
        } else {
            throw new RuntimeException("The data unit (" + dataPath.toString() + ") does not exist. You can't therefore ask for a select stream.");
        }
    }


    public static void create(DataPath dataPath) {

        dataPath.getDataSystem().create(dataPath);

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


        // A dag will build the data def and we may not want want it when dropping only one table
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

        // A hack to avoid building the data def
        // Because there is for now no drop options, the getDropOrderedTables
        // will build the dependency
        if (dataPaths.size() == 1) {
            if (exists(dataPaths.get(0))) {
                drop(dataPaths.get(0));
            }
        } else {
            for (DataPath dataPath : Dag.get(dataPaths).getDropOrderedTables()) {
                if (exists(dataPath)) {
                    drop(dataPath);
                }
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
        for (DataPath sourceDataPath : Dag.get(sources).getCreateOrderedTables()) {
            DataPath targetDataPath = target.getDataSystem().getDataPath(sourceDataPath.getName());
            Tabulars.move(sourceDataPath, targetDataPath);
            targetDataPaths.add(targetDataPath);
        }

        return targetDataPaths;

    }


    public static void move(DataPath source, DataPath target) {

        move(source,target,null);

    }


    public static boolean isEmpty(DataPath queue) {
        return queue.getDataSystem().isEmpty(queue);
    }


    public static int getSize(DataPath dataPath) {
        return dataPath.getDataSystem().size(dataPath);
    }

    /**
     *
     * @param dataPath
     * @return if the data path locate a document
     *
     * The counter part is {@link #isContainer(DataPath)}
     *
     */
    public static boolean isDocument(DataPath dataPath) {
        return dataPath.getDataSystem().isDocument(dataPath);
    }

    public static void create(List<DataPath> dataPaths) {
        Dag dag = Dag.get(dataPaths);
        dataPaths = dag.getCreateOrderedTables();
        for (DataPath dataPath : dataPaths) {
            create(dataPath);
        }
    }

    public static void dropForeignKey(ForeignKeyDef foreignKeyDef) {
        throw new RuntimeException("not yet implemented");
    }

    /**
     *
     * @param dataPath
     * @return the content of a data path in a string format
     */
    public static String getString(DataPath dataPath) {
        return dataPath.getDataSystem().getString(dataPath);
    }

    /**
     * Move a source document to a target document
     * @param source
     * @param target
     * @param transferProperties
     * @return a {@link TransferListener}
     */
    public static List<TransferListener> move(DataPath source, DataPath target, TransferProperties transferProperties) {

        // check source
        if (!Tabulars.exists(source)) {
            // Is it a query definition
            if (source.getDataDef().getQuery() == null) {
                throw new RuntimeException("We cannot move the source data path (" + source.toString() + ") because it does not exist");
            }
        }

        // Check target
        final Boolean exists = Tabulars.exists(target);
        if (!exists) {
            DataDefs.copy(source.getDataDef(), target.getDataDef());
            Tabulars.create(target);
        } else {
            // If this for instance, the move of a file, the file may exist
            // but have no content and therefore no structure
            if (target.getDataDef().getColumnDefs().size()!=0) {
                for (ColumnDef columnDef : source.getDataDef().getColumnDefs()) {
                    ColumnDef targetColumnDef = target.getDataDef().getColumnDef(columnDef.getColumnName());
                    if (targetColumnDef == null) {
                        String message = "Unable to move the data unit (" + source.toString() + ") because it exists already in the target location (" + target.toString() + ") with a different structure" +
                                " (The source column (" + columnDef.getColumnName() + ") was not found in the target data unit)";
                        DbLoggers.LOGGER_DB_ENGINE.severe(message);
                        throw new RuntimeException(message);
                    }
                }
            } else {
                DataDefs.copy(source.getDataDef(), target.getDataDef());
            }
        }

        final TableSystem sourceDataSystem = source.getDataDef().getDataPath().getDataSystem();
        if (sourceDataSystem.equals(target.getDataSystem())) {
            // same provider
            sourceDataSystem.move(source, target, transferProperties);
        } else {
            try (
                    SelectStream sourceSelectStream = Tabulars.getSelectStream(source);
                    InsertStream targetInsertStream = Tabulars.getInsertStream(target)
            ) {
                while (sourceSelectStream.next()) {
                    List<Object> objects = IntStream.range(0, sourceSelectStream.getDataDef().getColumnDefs().size())
                            .mapToObj(sourceSelectStream::getObject)
                            .collect(Collectors.toList());
                    targetInsertStream.insert(objects);
                }
            }
        }
        return null;
    }
}
