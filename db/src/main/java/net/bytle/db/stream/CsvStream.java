package net.bytle.db.stream;

import net.bytle.db.model.FileRelation;
import net.bytle.db.model.RelationDef;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.sql.Clob;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class CsvStream implements SelectStream {


    private final RelationDef sourceDef;
    private final Iterator<CSVRecord> recordIterator;
    private final FileReader in;
    private CSVRecord currentRecord;
    private int rowNum;

    CsvStream(FileRelation sourceDef) {
        this.sourceDef = sourceDef;

        try {
            in = new FileReader(sourceDef.getPath().toFile());

            recordIterator = CSVFormat.RFC4180.parse(in).iterator();
            // Pass the header
            recordIterator.next();
            rowNum = 0;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static CsvStream get(FileRelation sourceDef) {
        return new CsvStream(sourceDef);
    }

    @Override
    public boolean next() {
        try {
            currentRecord = recordIterator.next();
            rowNum++;
        } catch (NoSuchElementException e) {
            return false;
        }
        return true;
    }

    @Override
    public void close() {
        try {
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getString(int columnIndex) {
        return currentRecord.get(columnIndex);
    }

    @Override
    public void beforeFirst() {
        throw new RuntimeException("Not Yet implemented");
    }

    @Override
    public boolean first() {
        throw new RuntimeException("Not Yet implemented");
    }

    @Override
    public boolean last() {
        throw new RuntimeException("Not Yet implemented");
    }

    @Override
    public int getRow() {
        return rowNum;
    }

    @Override
    public boolean previous() {
        throw new RuntimeException("Not Yet implemented");
    }

    @Override
    public Object getObject(int columnIndex) {
        return currentRecord.get(columnIndex);
    }

    @Override
    public RelationDef getRelationDef() {
        return sourceDef;
    }

    @Override
    public double getDouble(int columnIndex) {
        return Double.parseDouble(currentRecord.get(columnIndex));
    }

    @Override
    public Clob getClob(int columnIndex) {
        throw new RuntimeException("Not Yet implemented");
    }
}
