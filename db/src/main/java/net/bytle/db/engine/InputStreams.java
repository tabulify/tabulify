package net.bytle.db.engine;

import java.io.InputStream;
import java.util.Scanner;


public class InputStreams {


    // https://stackoverflow.com/questions/309424/how-to-read-convert-an-inputstream-into-a-string-in-java
    public static String toString(InputStream input) {
        try (Scanner s = new Scanner(input)) {
            return s.useDelimiter("\\A").hasNext() ? s.next() : "";
        }
    }

}
