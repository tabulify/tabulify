package net.bytle.db.stream;

import net.bytle.cli.Log;
import net.bytle.db.DbLoggers;
import net.bytle.db.engine.DataTypes;
import net.bytle.db.model.RelationDef;

import java.util.HashMap;
import java.util.Map;

public class Streams {

    private static final Log LOGGER = DbLoggers.LOGGER_DB_ENGINE;



    /**
     * Print the table outpustream
     * align to the left
     *
     * @param tableOutputStream
     */
    public static void print(SelectStream tableOutputStream) {


        Map<Integer, Integer> maxs = new HashMap<>();
        final RelationDef tableDef = tableOutputStream.getDataPath().getDataDef();
        while (tableOutputStream.next()) {
            for (int i = 0; i < tableDef.getColumnDefs().size(); i++) {
                String string = tableOutputStream.getString(i);
                if (string == null) {
                    string = "";
                }
                int length = string.length();
                Integer max = maxs.get(i);
                if (max == null) {
                    maxs.put(i, length);
                    continue;
                }
                if (max < length) {
                    maxs.put(i, length);
                }
            }
        }
        for (int i = 0; i < tableDef.getColumnDefs().size(); i++) {
            int length = tableDef.getColumnDef(i).getColumnName().length();
            Integer max = maxs.get(i);
            if (max == null) {
                maxs.put(i, length);
                continue;
            }
            if (max < length) {
                maxs.put(i, length);
            }
        }

        // Number of space between columns
        int spacesBetweenCols = 3;

        // Building the format string
        // https://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html
        StringBuilder formatString = new StringBuilder();
        for (int i = 0; i < tableDef.getColumnDefs().size(); i++) {
            formatString.append("%"); // Template start
            if (!DataTypes.isNumeric(tableDef.getColumnDef(i).getDataType().getTypeCode())) {
                formatString.append("-"); // result left-justified. (Default is right)
            }
            formatString
                    .append(maxs.get(i)) // the width
                    .append("s") // S is a placeholder for string
                    .append(new String(new char[spacesBetweenCols]).replace("\0", " ")); // The spaces between column
        }
        LOGGER.fine("The format string is (" + formatString + ")");

        // Print the header
        System.out.println(String.format(formatString.toString(),
                tableDef.getColumnDefs()
                        .stream()
                        .map(s -> s.getColumnName())
                        .toArray()));

        // Separation Line
        StringBuilder line = new StringBuilder();
        for (Integer max : maxs.values()) {
            line
                    .append(new String(new char[max]).replace("\0", "-"))
                    .append(new String(new char[spacesBetweenCols]).replace("\0", " "));
        }
        System.out.println(line);

        // Print the data
        tableOutputStream.beforeFirst();
        while (tableOutputStream.next()) {
            System.out.println(String.format(formatString.toString(), getObjects(tableOutputStream)));
        }

        tableOutputStream.close();

    }

    private static Object[] getObjects(SelectStream selectStream) {
        int size = selectStream.getDataPath().getDataDef().getColumnDefs().size();
        Object[] os = new Object[size];

        for (int i = 0; i < size; i++) {
            os[i] = selectStream.getObject(i);
        }
        return os;
    }


}
