package net.bytle.db.spi;

import com.sun.jndi.toolkit.url.Uri;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.model.TableDef;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.uri.DataUri;

import java.util.List;


public class Tabulars {



    public static Boolean exists(DataPath dataPath) {

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
                if (!(dataPaths.contains(foreignKeyDef.getForeignPrimaryKey().getRelationDef().getDataPath()))) {
                    dataPath.getDataDef().deleteForeignKey(foreignKeyDef);
                }
            }
        }
        return dataPaths;
    }
}
