package net.bytle.db.queryExecutor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {
    public static String readFileToString(String file, String separator) throws IOException {
        String line;
        StringBuilder stringBuilder = new StringBuilder("");
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

        while ((line = bufferedReader.readLine()) != null)
            stringBuilder.append(line).append(separator);
        bufferedReader.close();

        return stringBuilder.toString();
    }

    public static List<String> readFileToArrayList(String file) throws IOException {
        ArrayList<String> lines = new ArrayList<String>();
        String line;
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

        while ((line = bufferedReader.readLine()) != null)
            lines.add(line);
        bufferedReader.close();

        return lines;
    }
}
