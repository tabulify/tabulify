package net.bytle.db.csv;


import net.bytle.db.fs.FsDataPath;
import net.bytle.db.fs.FsTableSystemLog;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.TableDef;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;

public class CsvDataDef extends TableDef {

    private final FsDataPath fsDataPath;
    private boolean columnNameInFirstRow = true;

    public boolean isColumnNameInFirstRow() {
        return columnNameInFirstRow;
    }

    public CsvDataDef setColumnNameInFirstRow(boolean columnNameInFirstRow) {
        this.columnNameInFirstRow = columnNameInFirstRow;
        return this;
    }

    public CsvDataDef(FsDataPath dataPath) {
        super(dataPath);
        this.fsDataPath = dataPath;
    }

    @Override
    public List<ColumnDef> getColumnDefs() {
        buildColumnNamesIfNeeded();
        return super.getColumnDefs();
    }

    @Override
    public <T> ColumnDef<T> getColumnDef(String columnName) {
        buildColumnNamesIfNeeded();
        return super.getColumnDef(columnName);
    }

    @Override
    public <T> ColumnDef<T> getColumnDef(Integer columnIndex) {
        buildColumnNamesIfNeeded();
        return super.getColumnDef(columnIndex);
    }



    private void buildColumnNamesIfNeeded() {

        if (super.getColumnDefs().size() == 0 && this.columnNameInFirstRow) {


            if (Files.exists(fsDataPath.getNioPath())) {
                try {

                    Reader in = new FileReader(fsDataPath.getNioPath().toFile());
                    Iterator<CSVRecord> recordIterator = CSVFormat.RFC4180.parse(in).iterator();
                    try {
                        CSVRecord headerRecord = recordIterator.next();

                        for (int i = 0; i < headerRecord.size(); i++) {
                            this.addColumn(headerRecord.get(i));
                        }
                    } catch (java.util.NoSuchElementException e) {
                        // No more CSV records available, file is empty
                        FsTableSystemLog.LOGGER_DB_FS.info("The file (" + fsDataPath.toString() + ") seems to be empty");
                    }

                    in.close();

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

    }


}
