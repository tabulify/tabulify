package net.bytle.db.csv;

import net.bytle.db.model.TableDef;
import net.bytle.db.spi.SelectStreamAbs;
import net.bytle.db.stream.SelectStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.sql.Clob;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

public class CsvSelectStream extends SelectStreamAbs implements SelectStream {


    private final CsvDataPath csvDataPath;
    private Iterator<CSVRecord> recordIterator;
    private CSVRecord currentRecord;
    private int rowNum;
    private CSVParser csvParser;

    CsvSelectStream(CsvDataPath csvDataPath) {

        super(csvDataPath);
        this.csvDataPath = csvDataPath;
        beforeFirst();

    }

    public static CsvSelectStream of(CsvDataPath csvDataPath) {

        return new CsvSelectStream(csvDataPath);

    }

    @Override
    public boolean next() {
        rowNum++;
        return safeIterate();
    }

    /**
     * The file may empty, it throws then exception,
     * this utility method encapsulates it
     * @return true if there is another record, false otherwise
     */
    private boolean safeIterate() {
        try {
            currentRecord = recordIterator.next();
        } catch (NoSuchElementException e) {
            return false;
        } catch (Exception e) {
            if (e instanceof IllegalStateException){
                // We got that when the file is empty
                return false;
            } else {
                throw new RuntimeException("Error when iterating on the next csv record", e);
            }
        }
        return true;
    }

    @Override
    public void close() {
        try {
            csvParser.close();
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
            CsvDataDef csvDataDef = this.csvDataPath.getDataDef();
            CSVFormat csvFormat = csvDataDef.getCsvFormat();
            csvParser = CSVParser.parse(csvDataPath.getNioPath(), csvDataDef.getCharset(), csvFormat);
            recordIterator = csvParser.iterator();


            // Pass the header
            for (int i = 0; i <= csvDataDef.getHeaderRowCount(); i++) {
                safeIterate();
            }

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
            final int size = csvDataPath.getDataDef().getColumnDefs().size();
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
        return csvDataPath.getDataDef();
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
     * Retrieves and removes the head of this data path, or returns false if this queue is empty.
     *
     * @param timeout  - the time out before returning null
     * @param timeUnit - the time unit of the time out
     * @return true if there is a new element, otherwise false
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
