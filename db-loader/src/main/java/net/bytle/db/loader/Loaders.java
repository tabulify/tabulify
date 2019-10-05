package net.bytle.db.loader;

import net.bytle.db.model.RelationDef;
import net.bytle.db.model.TableDef;
import net.bytle.db.stream.InsertStreamListener;
import net.bytle.cli.Log;

public class Loaders {

    public static InsertStreamListener load(TableDef targetTable, RelationDef sourceDef) {

        return (new ResultSetLoader(targetTable, sourceDef).load()).get(0);

    }

    static final Log LOGGER_DB_LOADER = Log.getLog(Loaders.class);

}
