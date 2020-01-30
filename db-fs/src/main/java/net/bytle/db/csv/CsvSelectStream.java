package net.bytle.db.csv;

import net.bytle.db.model.TableDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.SelectStreamAbs;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Clob;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CsvSelectStream extends SelectStreamAbs  {


    private final CsvDataPath csvDataPath;
    private Iterator<CSVRecord> recordIterator;
    private CSVParser csvParser;
    private CSVRecord currentRecord;

    /**
     * The record number (0=first)
     */
    private int rowNum = 0;


    /**
     * The line number in the file
     */
    private long lineNumberInTextFile = 0;

    CsvSelectStream(CsvDataPath csvDataPath) {

        super(csvDataPath);
        this.csvDataPath = csvDataPath;
        beforeFirst();

    }

    public static CsvSelectStream of(CsvDataPath csvDataPath) {

        return new CsvSelectStream(csvDataPath);

    }

  @Override
  public List<DataPath> getParents() {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
    public boolean next() {
        boolean recordWasFetched = safeIterate();
        if (recordWasFetched){
            rowNum++;
        }
        return recordWasFetched;
    }

    /**
     * The file may empty, it throws then exception,
     * this utility method encapsulates it
     *
     * @return true if there is another record, false otherwise
     */
    private boolean safeIterate() {

        currentRecord = Csvs.safeIterate(recordIterator);
        if (currentRecord == null) {
            return false;
        } else {
            lineNumberInTextFile++;
            if (currentRecord.size() == 1) {
                // Empty line
                if (currentRecord.get(0).equals("") && csvDataPath.getDataDef().isIgnoreEmptyLine()) {
                    return safeIterate();
                } else {
                    return true;
                }
            } else {
                return true;
            }
        }

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
            Path nioPath = csvDataPath.getNioPath();
            csvParser = CSVParser.parse(nioPath, csvDataDef.getCharset(), csvFormat);
            recordIterator = csvParser.iterator();
            lineNumberInTextFile=0;

            // Pass the header
            while (lineNumberInTextFile<csvDataDef.getHeaderRowCount()) {
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
    public TableDef getSelectDataDef() {
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
