package net.bytle.db.csv;

import net.bytle.db.fs.FsDataPath;
import net.bytle.db.model.RelationDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.SelectStreamAbs;
import net.bytle.db.stream.SelectStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.sql.Clob;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class CsvSelectStream extends SelectStreamAbs implements SelectStream {


    private final FsDataPath fsDataPath;
    private Iterator<CSVRecord> recordIterator;
    private FileReader in;
    private CSVRecord currentRecord;
    private int rowNum;

    CsvSelectStream(FsDataPath fsDataPath) {
        this.fsDataPath = fsDataPath;
        beforeFirst();

    }

    public static CsvSelectStream of(FsDataPath fsDataPath) {

        return new CsvSelectStream(fsDataPath);

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
        try {
            in = new FileReader(this.fsDataPath.getPath().toFile());
            recordIterator = CSVFormat.RFC4180.parse(in).iterator();
            // Pass the header
            recordIterator.next();
            rowNum = 0;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean first() {
        beforeFirst();
        return next();
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
        return fsDataPath.getDataDef();
    }

    @Override
    public DataPath getDataPath() {
        return fsDataPath;
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
