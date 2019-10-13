package net.bytle.db.spi;

import net.bytle.db.engine.Dag;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.model.TableDef;
import net.bytle.db.stream.SelectStream;

import java.util.List;


public class Tabulars {



    public synchronized static Boolean exists(DataPath dataPath) {

        return dataPath.getDataSystem().exists(dataPath);

    }

    public static SelectStream getSelectStream(DataPath dataPath) {
        return dataPath.getDataSystem().getSelectStream(dataPath);
    }

    public static DataPath create(DataPath dataPath) {

        return null;
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
}
