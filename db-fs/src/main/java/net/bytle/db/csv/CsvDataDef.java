package net.bytle.db.csv;


import net.bytle.db.fs.FsTableSystemLog;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.TableDef;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;

/**
 * Implementation of the CSV format
 *
 * @see <a href="https://commons.apache.org/proper/commons-csv/apidocs/org/apache/commons/csv/CSVFormat.html">CSVFormat for common format</a>
 *
 * Format:
 *   * <a href="https://www.w3.org/TR/2015/REC-tabular-data-model-20151217/#parsing">W3c Parsing</a>
 *   * <a href="https://cloud.google.com/bigquery/docs/loading-data-cloud-storage-csv">BigQuery</a>
 *
 *
 */
public class CsvDataDef extends TableDef {

    /**
     * See {@link #setCharset(Charset)}
     */
    private Charset charset = StandardCharsets.UTF_8;

    /**
     * The path of the Csv
     */
    private final CsvDataPath fsDataPath;

    /**
     * The number of header rows in the file
     */
    private int headerRowCount = 0;

    /**
     * Indicates whether to trim whitespace around cells
     * In case of fixed format
     */
    private boolean trimWhitespace = false;

    /**
     * Newline
     * The strings that is used at the end of a row
     */
    private String newline = "\r\n";

    /**
     * See {@link #setDelimiter(String) | Delimiter}
     */
    private String delimiter = ",";

    /**
     * The string that is used to escape the {@link #quoteCharacter | quote character } within escaped cells
     */
    private char escapeCharacter = '"';

    /**
     * See {@link #setQuoteCharacter(char)}
     */
    private char quoteCharacter = '"';

    public boolean isTrimWhitespace() {
        return trimWhitespace;
    }

    public void setTrimWhitespace(boolean trimWhitespace) {
        this.trimWhitespace = trimWhitespace;
    }

    public String getNewline() {
        return newline;
    }

    public void setNewline(String newline) {
        this.newline = newline;
    }

    public char getEscapeCharacter() {
        return escapeCharacter;
    }

    public void setEscapeCharacter(char escapeCharacter) {
        this.escapeCharacter = escapeCharacter;
    }

    public char getQuoteCharacter() {
        return quoteCharacter;
    }

    /**
     *
     * @param quoteCharacter The string that is used around escaped cells
     */
    public void setQuoteCharacter(char quoteCharacter) {
        this.quoteCharacter = quoteCharacter;
    }

    /**
     *
     * @return the {@link #delimiter}
     */
    public String getDelimiter() {
        return delimiter;
    }

    /**
     *
     * @param delimiter The separator between cells known as cell delimiter. Default value is a comma ','
     * @return The {@link CsvDataDef CsvDataDef} instance for chaining initialization
     */
    public CsvDataDef setDelimiter(String delimiter) {
        this.delimiter = delimiter;
        return this;
    }

    /**
     * Set the character set of the file
     * @param charset The character encoding for the file - Default: UTf-8
     * @return The {@link CsvDataDef CsvDataDef} instance for chaining initialization
     */
    public CsvDataDef setCharset(Charset charset) {
        this.charset = charset;
        return this;
    }

    public int getHeaderRowCount() {
        return headerRowCount;
    }

    public CsvDataDef setHeaderRowCount(int headerRowCount) {
        this.headerRowCount = headerRowCount;
        return this;
    }

    /**
     *
     * @param dataPath The CsvDataPath
     */
    public CsvDataDef(CsvDataPath dataPath) {
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

    @Override
    public CsvDataPath getDataPath() {
        return fsDataPath;
    }

    /**
     * Build the column metadata from the first row if needed
     */
    private void buildColumnNamesIfNeeded() {

        if (super.getColumnDefs().size() == 0 && this.headerRowCount > 0) {


            if (Files.exists(fsDataPath.getNioPath())) {
                try (
                    Reader in = new FileReader(fsDataPath.getNioPath().toFile());
                ){
                    Iterator<CSVRecord> recordIterator = CSVFormat.RFC4180.parse(in).iterator();
                    try {

                        CSVRecord headerRecord = null;
                        for (int i=0;i<=this.headerRowCount;i++) {
                            headerRecord = recordIterator.next();
                        }

                        for (int i = 0; i < headerRecord.size(); i++) {
                            this.addColumn(headerRecord.get(i));
                        }

                    } catch (java.util.NoSuchElementException e) {
                        // No more CSV records available, file is empty
                        FsTableSystemLog.LOGGER_DB_FS.info("The file (" + fsDataPath.toString() + ") seems to be empty");
                    }

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

    }


    /**
     *
     * @return the {@link #charset}
     */
    public Charset getCharset() {
        return charset;
    }

}
