package net.bytle.db.csv;

import net.bytle.fs.Fs;
import org.apache.commons.csv.CSVRecord;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Static function
 */
public class Csvs {

    /**
     *
     * @param recordIterator
     * @param csvDataPath
     * @return a csvRecord or null if this is the end
     */
    public static CSVRecord safeIterate(Iterator<CSVRecord> recordIterator, CsvDataPath csvDataPath) {
        try {
            return recordIterator.next();
        } catch (NoSuchElementException e) {
            return null;
        } catch (Exception e) {
            if (e instanceof IllegalStateException){
                // We got that when the file is empty
                if (Fs.isEmpty(csvDataPath.getNioPath())) {
                    return null;
                } else {
                    throw new RuntimeException("IllegalStateException: Error when iterating on the next csv record", e);
                }
            } else {
                throw new RuntimeException("Error when iterating on the next csv record", e);
            }
        }
    }
}
