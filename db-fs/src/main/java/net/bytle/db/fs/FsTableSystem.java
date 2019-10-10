package net.bytle.db.fs;

import net.bytle.db.csv.CsvTable;
import net.bytle.db.model.RelationDef;
import net.bytle.db.spi.TableSystem;
import net.bytle.db.uri.DataUri;
import net.bytle.fs.Fs;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class FsTableSystem extends TableSystem {


    private final String uri;
    private final Map<String, ?> env;

    private FsTableSystem(String uri, Map<String, ?> env) {
        assert uri!=null;
        if (env==null){
            env = new HashMap<>();
        }
        this.uri = uri;
        this.env = env;
    }

    protected static TableSystem of(String uri) {
        Map<String,?> env = new HashMap<>();
        return new FsTableSystem(uri, env);
    }

    public static TableSystem of(String uri, Map<String, ?> env) {
        return new FsTableSystem(uri,env);
    }

    @Override
    public RelationDef getRelationDef(DataUri dataUri) {
        Path path = Fs.getPath(dataUri.getPathSegments());
        return CsvTable.of(path);
    }
}
