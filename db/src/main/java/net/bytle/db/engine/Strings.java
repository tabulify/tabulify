package net.bytle.db.engine;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.bytle.db.engine.Fs.getFileContent;

public class Strings {


    public static Integer numberOfOccurences(String s, String regexp) {

        Pattern pattern = Pattern.compile(regexp, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(s);
        Integer counter = 0;
        while (matcher.find()) {
            counter++;
        }
        return counter;

    }

    /**
     * Function used before a text comparison to normalize the text
     * @param string
     * @return a compact string that is written on one line, has no double space and is trimmed
     */
    static public String normalize(String string) {
        return string.replaceAll("\r\n|\n", " ") // No new line
                .replaceAll("[ ]{2,10}", "")
                .trim(); // No double space;
    }

    public static String get(Path path) {
        return getFileContent(path);
    }


    /**
     * A function to help written null safe console message
     *
     * @param o the input object
     * @return "null" if the object is null or the string representation of the object
     */
    static public String toStringNullSafe(Object o) {

        if (o == null) {
            return "null";
        } else {
            return o.toString();
        }

    }
}
