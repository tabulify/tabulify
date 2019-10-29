package net.bytle.db.csv;


import net.bytle.db.fs.FsDataPath;
import net.bytle.db.model.TableDef;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.Iterator;

public class CsvDataDef  {




    public static void build(TableDef dataDef) {
        FsDataPath fsDataPath = (FsDataPath) dataDef.getDataPath();

        if (Files.exists(fsDataPath.getNioPath())) {
            try {

                Reader in = new FileReader(fsDataPath.getNioPath().toFile());
                Iterator<CSVRecord> recordIterator = CSVFormat.RFC4180.parse(in).iterator();
                CSVRecord headerRecord = recordIterator.next();

                for (int i = 0; i < headerRecord.size(); i++) {
                    dataDef.addColumn(headerRecord.get(i));
                }
                in.close();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


    }



}
