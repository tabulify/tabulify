package net.bytle.db.fs;

import net.bytle.db.csv.CsvDataDef;
import net.bytle.db.model.RelationDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.TableSystem;


import java.nio.file.Path;

public class FsDataPath extends DataPath {


    private final Path path;
    private final FsTableSystem tableSystem;

    public FsDataPath(FsTableSystem fsTableSystem, Path path) {

        this.tableSystem = fsTableSystem;
        this.path = path;

    }

    public static FsDataPath of(FsTableSystem fsTableSystem, Path path) {

        return new FsDataPath(fsTableSystem,path);

    }

    @Override
    public TableSystem getDataSystem() {
        return tableSystem;
    }

    @Override
    public RelationDef getDataDef() {
        return CsvDataDef.of(this);
    }

    @Override
    public String getName() {
        return this.path.getFileName().toString();
    }

    public Path getPath() {
        return this.path;
    }
}
