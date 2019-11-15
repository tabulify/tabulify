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
     * @param recordIterator
     * @return a csvRecord or null if this is the end
     */
    public static CSVRecord safeIterate(Iterator<CSVRecord> recordIterator) {

        if (recordIterator.hasNext()) {
            return recordIterator.next();
        } else {
            return null;
        }

    }
}
