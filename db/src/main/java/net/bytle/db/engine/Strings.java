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
     * @param string
     * @return a compact string that is written on one line and has no double space
     */
    static public String onOneLine(String string) {
        return string.replaceAll("\r\n|\n", " ") // No new line
                .replaceAll("[ ]{2,10}", ""); // No double space;
    }

    public static String get(Path path) {
        return getFileContent(path);
    }
}
