package net.bytle.db.csv;

import net.bytle.db.model.ColumnDef;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.nio.file.Files;

public class CsvManager {
    public static void create(CsvDataPath csvDataPath) {

        try {
            Files.createFile(csvDataPath.getNioPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        CsvDataDef csvDataDef = csvDataPath.getDataDef();
        CSVFormat csvFormat = csvDataPath.getDataDef().getCsvFormat();
        if (csvDataDef.getHeaderRowCount()>0) {
            final String[] headers = csvDataDef.getColumnDefs().stream()
                    .map(ColumnDef::getColumnName).toArray(String[]::new);
            if (headers.length!=0) {
                csvFormat = csvFormat.withHeader(headers);
            } else {
                throw new RuntimeException("The CSV file has a format asking to print the headers but there is no columns defined for this CSV");
            }
        }

        // Creation of the file with the header or not
        try {
            CSVPrinter printer = csvFormat
                    .print(csvDataPath.getNioPath(), csvDataDef.getCharset());
            printer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
