package net.bytle.db.csv;

import net.bytle.db.fs.FsDataPath;
import net.bytle.db.fs.FsTableSystem;

import java.nio.file.Path;


public class CsvDataPath extends FsDataPath {

    public CsvDataPath(FsTableSystem fsTableSystem, Path path) {
        super(fsTableSystem, path);
    }

    public CsvDataPath(Path path) {
        super(FsTableSystem.getDefault(), path);
    }



    @Override
    public CsvDataDef getDataDef() {
        if (this.dataDef==null) {
            this.dataDef = new CsvDataDef(this);
        }
        return (CsvDataDef) this.dataDef;
    }

}
