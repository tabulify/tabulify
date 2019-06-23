package net.bytle.db.engine;

import net.bytle.cli.CliLog;
import net.bytle.db.DbLoggers;
import net.bytle.db.model.ISqlRelation;
import net.bytle.db.model.QueryDef;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.stream.SelectStreamListener;
import net.bytle.db.stream.SqlSelectStream;
import net.bytle.db.stream.Streams;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class Queries {

    private static final Logger LOGGER = DbLoggers.LOGGER_DB_ENGINE;

    private static final String SELECT_WORD = "SELECT";
    private static final String WITH_WORD = "WITH";

    private static final List<String> queryFirstWords = Arrays.asList(new String[]{SELECT_WORD, WITH_WORD});

    /**
     * @param s
     * @return true if a the string is a query (ie start with the 'select' word
     */
    public static Boolean isQuery(String s) {

        if (s == null) {
            return false;

        }
        s = s.trim();
        List<String> seps = Arrays.asList(" ", "\r\n", "\n", "\t");
        Integer sepIndexMin = null; // Not found
        for (String sep : seps) {
            int sepIndex = s.indexOf(sep);
            if (sepIndex != -1 && (sepIndexMin == null || sepIndex < sepIndexMin)) {
                sepIndexMin = sepIndex;
            }
        }
        if (sepIndexMin == null) {
            return false;
        }

        s = s.substring(0, sepIndexMin).toUpperCase();


        return queryFirstWords.contains(s);

    }


    /**
     * Put the result of a query in a CSV file
     *
     * @param query           - the query to download
     * @param clobInApartFile - do we put the clob values in an apart file (not in the downloaded file)
     * @param outputPath      - the file path where to crete the output file
     */
    public static int download(QueryDef query, Path outputPath, Boolean clobInApartFile) {

        try {

            SqlSelectStream stream = Streams.getSqlSelectStream(query);

            // The directory
            Path outpathDir = outputPath.toAbsolutePath().getParent();

            // CSV
            CSVPrinter csvFilePrinter;
            CSVPrinter csvMetaFilePrinter;
            try {
                //initialize FileWriter object
                FileWriter csvFileWriter = new FileWriter(outputPath.toFile());
                String csvFileName = outputPath.getFileName().toString();
                String metaFileName = csvFileName.substring(0, csvFileName.lastIndexOf(".")) + "_meta.csv";
                Path csvMetaFilePath = Paths.get(outpathDir.toString(), metaFileName);
                FileWriter csvMetaFileWriter = new FileWriter(csvMetaFilePath.toFile());
                //Create the CSVFormat object with "\n" as a record delimiter
                CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(System.lineSeparator());
                //initialize CSVPrinter object
                csvFilePrinter = new CSVPrinter(csvFileWriter, csvFileFormat);
                csvMetaFilePrinter = new CSVPrinter(csvMetaFileWriter, csvFileFormat);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }


            // The number of column
            int columnCount = stream.getRelationDef().getColumnDefs().size();
            LOGGER.info(columnCount + " column(s) to download");

            // The headers Name
            List<Object> csvRow = new ArrayList<>();
            for (int j = 0; j < columnCount; j++) {
                csvRow.add(stream.getRelationDef().getColumnDef(j).getColumnName());
            }

            //Create CSV file header
            csvFilePrinter.printRecord(csvRow);
            csvMetaFilePrinter.printRecord(csvRow);

            // The type character
            csvRow = new ArrayList<>();
            for (int j = 0; j < columnCount; j++) {
                csvRow.add(stream.getRelationDef().getColumnDef(j).getDataType().getTypeName());
            }
            csvMetaFilePrinter.printRecord(csvRow);

            // The type numeric
            csvRow = new ArrayList<>();
            for (int j = 0; j < columnCount; j++) {
                csvRow.add(stream.getRelationDef().getColumnDef(j).getDataType().getTypeCode());
            }
            csvMetaFilePrinter.printRecord(csvRow);

            int counter = 0;
            while (stream.next()) {

                counter++;
                csvRow = new ArrayList<>();

                for (int j = 0; j < columnCount; j++) {

                    int typeCode = stream.getRelationDef().getColumnDef(j).getDataType().getTypeCode();
                    if (typeCode == Types.DOUBLE) {
                        // TODO: same than sqlite please, must be also used with all print stream
                        double aDouble = round(stream.getDouble(j), 2);
                        csvRow.add(aDouble);
                    } else if (typeCode == Types.CLOB) {

                        // Default would be one clob by file
                        Clob lobData = stream.getClob(j);
                        if (lobData != null) {
                            Reader reader = lobData.getCharacterStream();
                            int c;
                            Writer writer;
                            Path outputClobPath = null;
                            if (!clobInApartFile) {

                                writer = new StringWriter();

                            } else {

                                outputClobPath = Paths.get(outpathDir.toString(), String.valueOf(counter) + ".sql");
                                writer = new FileWriter(outputClobPath.toFile());

                            }
                            while ((c = reader.read()) != -1) {

                                writer.append((char) c);

                            }
                            lobData.free();
                            reader.close();

                            if (!clobInApartFile) {
                                String clobString = writer.toString();
                                if (clobString != null) {
                                    clobString = clobString.replaceAll("(\r\n|\n|\r)", " ");
                                }
                                csvRow.add(clobString);
                            } else {

                                csvRow.add(outputClobPath.getFileName());

                            }

                            writer.close();
                        }


                    } else {

                        String sWithoutEndOfLine = stream.getString(j);
                        if (sWithoutEndOfLine != null) {
                            sWithoutEndOfLine = sWithoutEndOfLine.replaceAll("(\r\n|\n|\r)", " ");
                        }
                        csvRow.add(sWithoutEndOfLine);

                    }
                }

                // Feedback
                // String column for the feedback
                if (counter % 1000 == 0) {
                    System.out.println("Rows Downloaded: " + counter);
                    // Flush
                    csvFilePrinter.flush();
                }

//                if (csvRow.size()>0) {
//                    final Object o = csvRow.get(0);
//                    String s="";
//                    if (o != null) {
//                        s=o.toString();
//                    }
//
//
//                    int maxInt = 10;
//                    String maxString = "...";
//                    if (s.length() < 10) {
//                        maxInt = s.length();
//                        maxString = "";
//                    }
//                    System.out.println("Row: " + s.substring(0, maxInt) + maxString + " with a length of " + s.length());
//
//                } else {
//                    System.out.println("No data in the row");
//                }

                // Print row must be added after the feedback otherwise csvRow is empty
                csvFilePrinter.printRecord(csvRow);

            }


            // Close the CSV file
            try {
                csvFilePrinter.flush();
                csvMetaFilePrinter.flush();
                // Total Rows downloaded
                LOGGER.info("Total Rows Downloaded: " + counter);
                csvFilePrinter.close();
                csvMetaFilePrinter.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // Close the SQL resources
            stream.close();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return 0;

    }

    /**
     * @param value - the input double value
     * @param scale - the scale (the number of decimal after the comma)
     * @return a double - half up rounded to the scale
     * TODO: The test utility of Sqlite use a printFormat for double
     */
    private static double round(double value, int scale) {
        if (scale < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(scale, RoundingMode.HALF_UP);
        return bd.doubleValue();

    }


    public static ResultSet getResultSet(ISqlRelation query) {

        try {

            Statement statement;
            Connection currentConnection = query.getDatabase().getCurrentConnection();
            if (currentConnection == null) {
                throw new RuntimeException("The database (" + query.getDatabase() + ") has no connection. This is not relational dataabse ?");
            }
            statement = currentConnection.createStatement();
            LOGGER.info("Executing the query " + query.getName());
            LOGGER.info(CliLog.onOneLine(query.getQuery()));
            return statement.executeQuery(query.getQuery());


        } catch (SQLException e) {
            LOGGER.severe("The execution went wrong, Bad query ?");
            LOGGER.severe("Query Name: " + query.getName());
            LOGGER.severe("Query: \n" + query.getQuery());
            throw new RuntimeException(e);
        }

    }


    public static void print(QueryDef queryDef) {
        SelectStream selectStream = Streams.getSelectStream(queryDef);
        Streams.print(selectStream);
    }

    /**
     * Execute a query
     *
     * @param queryDef
     */
    public static SelectStreamListener execute(QueryDef queryDef) {

        SqlSelectStream selectStream = Streams.getSqlSelectStream(queryDef);
        SelectStreamListener selectStreamListener = SelectStreamListener.get(selectStream);

        int columnCount = queryDef.getColumnDefs().size();
        while (selectStream.next()) {

            selectStreamListener.addRows(1);

            for (int i = 0; i < columnCount; i++) {

                selectStream.getObject(i);

            }

        }
        return selectStreamListener;

    }
}
