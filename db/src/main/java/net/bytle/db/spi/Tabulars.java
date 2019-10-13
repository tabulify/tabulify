package net.bytle.db.spi;

import net.bytle.db.database.Database;
import net.bytle.db.engine.Dag;
import net.bytle.db.engine.Tables;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.model.TableDef;
import net.bytle.db.stream.SelectStream;

import javax.xml.crypto.Data;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import static com.sun.xml.internal.ws.spi.db.BindingContextFactory.LOGGER;


public class Tabulars {



    public synchronized static Boolean exists(DataPath dataPath) {

        return dataPath.getDataSystem().exists(dataPath);

    }

    public static SelectStream getSelectStream(DataPath dataPath) {
        return dataPath.getDataSystem().getSelectStream(dataPath);
    }

    public static DataPath create(DataPath dataPath) {

        return dataPath.getDataSystem().create(dataPath);
    }

    /**
     *
     * @param dataPaths - a list of data path
     * @return return a independent set of data path (ie independent of foreign key outside of the set)
     *
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
     *
     * @param dataPath - a data path container (a directory, a schema or a catalog)
     * @return the children data paths representing sql tables, schema or files
     */
    public static DataPath getChildrenDataPath(DataPath dataPath) {

        return null;
    }

    public static <T> T getMax(ColumnDef<T> columnDef) {
        return columnDef.getRelationDef().getDataPath().getDataSystem().getMax(columnDef);
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

            LOGGER.fine("The data object (" + dataPath.toString() + ") already exist.");

        }

    }

    public static void drop(DataPath... dataPaths){

        Dag dag = Dag.get(Arrays.asList(dataPaths));
        for (DataPath dataPath:dag.getDropOrderedTables()) {
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





}
