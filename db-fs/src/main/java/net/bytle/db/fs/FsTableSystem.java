package net.bytle.db.fs;

import net.bytle.db.csv.CsvTable;
import net.bytle.db.model.RelationDef;
import net.bytle.db.spi.TableSystem;
import net.bytle.db.uri.DataUri;
import net.bytle.fs.Fs;

import java.nio.file.Path;

public class FsTableSystem extends TableSystem {


    @Override
    public RelationDef getRelationDef(DataUri dataUri) {
        Path path = Fs.getPath(dataUri.getPathSegments());
        return CsvTable.of(path);
    }
}
