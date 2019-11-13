package net.bytle.db.csv;

import net.bytle.db.fs.FsDataPath;
import net.bytle.db.model.TableDef;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.InsertStreamAbs;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

public class CsvInsertStream extends InsertStreamAbs implements InsertStream {

    private final CsvDataDef csvDataDef;
    final CSVPrinter printer;
    private final CsvDataPath csvDataPath;

    public CsvInsertStream(CsvDataPath fsDataPath) {

        super(fsDataPath);

        this.csvDataDef = fsDataPath.getDataDef();
        this.csvDataPath = fsDataPath;
        final String headers = csvDataDef.getColumnDefs().stream()
                .map(s->s.getColumnName())
                .collect(Collectors.joining(","));
        try {
            BufferedWriter writer = Files.newBufferedWriter(csvDataPath.getNioPath(),csvDataPath.getDataDef().getCharset());
            printer = CSVFormat
                    .DEFAULT
                    .withHeader(headers)
                    .print(writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static CsvInsertStream of(CsvDataPath csvDataPath) {

        return new CsvInsertStream(csvDataPath);

    }


    /**
     * @param values - The values to insert
     * @return the {@link InsertStream} for insert chaining
     */
    @Override
    public CsvInsertStream insert(List<Object> values) {
        try {
            printer.printRecord(values);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    /**
     * Close the stream
     * ie
     * * commit the last rows
     * * close the connection
     * * ...
     */
    @Override
    public void close() {
        try {
            printer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * In case of parent child hierarchy
     * we can check if we need to send the data with the function nextInsertSendBatch()
     * and send it with this function
     */
    @Override
    public void flush() {
        try {
            printer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
