package net.bytle.type;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Strings {


    public static Integer numberOfOccurrences(String s, String regexp) {

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
     *
     * @param string
     * @return a compact string that is written on one line, has no double space and is trimmed
     */
    static public String normalize(String string) {
        return string.replaceAll("\r\n|\n", " ") // No new line
                .replaceAll("[ ]{2,10}", " ")
                .trim(); // No double space;
    }

    public static String get(Path path) {
        try {

            StringBuilder s = new StringBuilder();
            BufferedReader reader;
            reader = new BufferedReader(new FileReader(path.toFile()));
            String line;
            while ((line = reader.readLine()) != null) {
                s.append(line).append(System.getProperty("line.separator"));
            }

            return s.toString();

        } catch (FileNotFoundException e) {
            throw new RuntimeException("Unable to find the file (" + path.toAbsolutePath().normalize().toString() + ")", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    /**
     * Left Trim
     * Credits: https://stackoverflow.com/questions/15567010/what-is-a-good-alternative-of-ltrim-and-rtrim-in-java
     *
     * @param s
     * @return
     */
    public static String ltrim(String s) {
        int i = 0;
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        return s.substring(i);
    }


    /**
     * Right Trim
     * Credits: https://stackoverflow.com/questions/15567010/what-is-a-good-alternative-of-ltrim-and-rtrim-in-java
     *
     * @param s
     * @return
     */
    public static String rtrim(String s) {
        int i = s.length() - 1;
        while (i >= 0 && Character.isWhitespace(s.charAt(i))) {
            i--;
        }
        return s.substring(0, i + 1);
    }

    /**
     * Right Trim only end of line
     * Credits: https://stackoverflow.com/questions/15567010/what-is-a-good-alternative-of-ltrim-and-rtrim-in-java
     *
     * @param s
     * @return
     */
    public static String rtrimEol(String s) {
        char[] eofChar = {"\n".charAt(0), "\r".charAt(0)};

        int i = s.length() - 1;
        while (i >= 0 && Arrayss.in(eofChar, s.charAt(i))) {
            i--;
        }
        return s.substring(0, i + 1);
    }

    /**
     * @param s
     * @return a camel cased string
     */
    public static String toCamelCase(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static Integer getDigitCount(String s) {

        return  Math.toIntExact(s.chars()
                .filter(i->Character.isDigit((char) i))
                .count());

    }

  public static String multiline(String... s) {
    return String.join(
      System.getProperty("line.separator"),
      s);
  }
}
