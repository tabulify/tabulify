package net.bytle.db.csv;


import net.bytle.db.database.Databases;
import net.bytle.db.model.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

public class CsvRelation extends RelationDefAbs implements RelationDef {

    private final Path path;

    public static CsvRelation of(Path path){
        return new CsvRelation(path);
    }

    private CsvRelation(Path path) {
        super(path.toString());

        this.path = path;
        this.schema = Databases.of().getCurrentSchema();

        if (Files.exists(path)) {
            try {

                Reader in = new FileReader(path.toFile());
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


    public Path getPath() {
        return this.path;
    }

    @Override
    public String getName() {
        return path.getFileName().toString();
    }


    public CsvRelation addColumn(String name){
        super.meta.addColumn(name);
        return this;
    }

}
