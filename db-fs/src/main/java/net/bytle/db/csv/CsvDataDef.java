package net.bytle.db.csv;


import net.bytle.db.database.Databases;
import net.bytle.db.fs.FsDataPath;
import net.bytle.db.model.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

public class CsvDataDef extends RelationDefAbs implements RelationDef {

    private final FsDataPath fsDataPath;

    public static CsvDataDef of(FsDataPath fsDataPath){
        return new CsvDataDef(fsDataPath);
    }

    private CsvDataDef(FsDataPath fsDataPath) {

        super(fsDataPath.toString());

        this.fsDataPath = fsDataPath;
        this.schema = Databases.of().getCurrentSchema();

        if (Files.exists(this.fsDataPath.getPath())) {
            try {

                Reader in = new FileReader(this.fsDataPath.getPath().toFile());
                Iterator<CSVRecord> recordIterator = CSVFormat.RFC4180.parse(in).iterator();
                CSVRecord headerRecord = recordIterator.next();
                this.meta = new RelationMeta(this);
                for (int i = 0; i < headerRecord.size(); i++) {
                    meta.addColumn(headerRecord.get(i));
                }
                in.close();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }


    public FsDataPath getFsDataPath() {
        return this.fsDataPath;
    }

    @Override
    public String getName() {
        return fsDataPath.getName();
    }


    public CsvDataDef addColumn(String name){
        super.meta.addColumn(name);
        return this;
    }

}
