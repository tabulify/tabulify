package net.bytle.db.csv;

import net.bytle.db.fs.FsDataPath;
import net.bytle.db.model.TableDef;
import net.bytle.db.spi.SelectStreamAbs;
import net.bytle.db.stream.SelectStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.sql.Clob;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

public class CsvSelectStream extends SelectStreamAbs implements SelectStream {


    private final FsDataPath fsDataPath;
    private Iterator<CSVRecord> recordIterator;
    private FileReader in;
    private CSVRecord currentRecord;
    private int rowNum;

    CsvSelectStream(FsDataPath fsDataPath) {

        super(fsDataPath);
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
            in = new FileReader(this.fsDataPath.getNioPath().toFile());
            recordIterator = CSVFormat.RFC4180.parse(in).iterator();
            // Pass the header
            recordIterator.next();
            rowNum = 0;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    @Override
    public int getRow() {
        return rowNum;
    }


    @Override
    public Object getObject(int columnIndex) {
        if (columnIndex > currentRecord.size() - 1) {
            final int size = fsDataPath.getDataDef().getColumnDefs().size();
            if (currentRecord.size() > size) {
                throw new RuntimeException("There is no data at the index (" + columnIndex + ") because this tabular has (" + size + ") columns (Column 1 is at index 0).");
            } else {
                return null;
            }
        }
        return currentRecord.get(columnIndex);
    }

    @Override
    public TableDef getDataDef() {
        return fsDataPath.getDataDef();
    }


    @Override
    public double getDouble(int columnIndex) {

        return Double.parseDouble(currentRecord.get(columnIndex));

    }

    @Override
    public Clob getClob(int columnIndex) {
        throw new RuntimeException("Not Yet implemented");
    }


    /**
     * Retrieves and removes the head of this data path, or returns null if this queue is empty.
     *
     * @param timeout
     * @param timeUnit
     * @return
     */
    @Override
    public boolean next(Integer timeout, TimeUnit timeUnit) {
        throw new RuntimeException("Not yet implemented");
    }

    @Override
    public Integer getInteger(int columnIndex) {
        return Integer.parseInt(currentRecord.get(columnIndex));
    }

    @Override
    public Object getObject(String columnName) {
        return currentRecord.get(columnName);
    }




}
