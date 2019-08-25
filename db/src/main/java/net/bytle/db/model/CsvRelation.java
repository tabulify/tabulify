package net.bytle.db.model;


import net.bytle.db.database.Databases;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

public class CsvRelation extends RelationDefAbs implements FileRelation {

    private final Path path;
    private final RelationMeta relationMeta;

    public CsvRelation(Path path) {
        this.path = path;
        this.schema = Databases.of().getCurrentSchema();
        this.name = path.getFileName().toString();

        // ResultSet csvResultSet = new CsvResultSet(inputFilePath);
        try {

            Reader in = new FileReader(path.toFile());
            Iterator<CSVRecord> recordIterator = CSVFormat.RFC4180.parse(in).iterator();
            CSVRecord headerRecord = recordIterator.next();
            relationMeta = new RelationMeta(this);
            for (int i = 0; i < headerRecord.size(); i++) {
                relationMeta.addColumn(headerRecord.get(i));
            }
            in.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public List<ColumnDef> getColumnDefs() {
        return relationMeta.getColumnDefs();
    }

    @Override
    public ColumnDef getColumnDef(String columnName) {
        return relationMeta.getColumnDef(columnName);
    }

    @Override
    public ColumnDef getColumnDef(Integer columnIndex) {
        return relationMeta.getColumnDef(columnIndex);
    }

    @Override
    public ColumnDef getColumnOf(String columnName, Class clazz) {
        return relationMeta.getColumnOf(columnName, clazz);
    }


    @Override
    public Path getPath() {
        return this.path;
    }
}
