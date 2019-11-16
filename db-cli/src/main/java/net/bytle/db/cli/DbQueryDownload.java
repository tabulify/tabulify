package net.bytle.db.cli;

import net.bytle.cli.*;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;
import net.bytle.db.engine.Queries;

import net.bytle.db.model.QueryDef;
import net.bytle.db.stream.Streams;
import net.bytle.log.Log;
import net.bytle.type.Strings;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Clob;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import static net.bytle.db.cli.Words.*;

/**
 * Created by gerard on 08-12-2016.
 * To download data
 */
public class DbQueryDownload {


    private static final Log LOGGER = Db.LOGGER_DB_CLI;

    private static final String ARG_NAME = "(Query|File.sql)";
    private static final String TARGET_DIRECTORY = "DownloadPathDir";


    public static void run(CliCommand cliCommand, String[] args) {

        cliCommand.argOf(SOURCE_DATA_URI)
                .setDescription("A data Uri that defines the connection where the query should run");
        cliCommand.argOf(ARG_NAME)
                .setDescription("One or more data uri files. Each file should contain a query");
        cliCommand.argOf(TARGET_DIRECTORY)
                .setDescription("A directory that defines where the data should be downloaded");
        String footer = "\nExample:\n" +
                cliCommand.getName() +  " @sqlite QueryToDownload.sql \n";
        cliCommand.setFooter(footer);


        CliParser cliParser = Clis.getParser(cliCommand, args);

        String sourceURL = "";
        String sourceDriver = cliParser.getString(JDBC_DRIVER_SOURCE_OPTION);

        Boolean clobInApartFile = cliParser.getBoolean(CLOB_OPTION);

        Path pathDownloadFile = null;
        String downloadPathArg = cliParser.getString(OUTPUT_FILE_PATH);
        if (downloadPathArg != null) {
            pathDownloadFile = Paths.get(downloadPathArg);
        }


        List<String> argValues = cliParser.getStrings(ARG_NAME);
        if (argValues.size() != 1) {
            System.err.println("An argument must be given");
            CliUsage.print(cliCommand);
            System.exit(1);
        }

        String arg0 = argValues.get(0);
        String sourceFileQuery;
        boolean isRegularFile = Files.isRegularFile(Paths.get(arg0));
        if (isRegularFile) {
            sourceFileQuery = cliParser.getFileContent(ARG_NAME, false);
        } else {
            sourceFileQuery = arg0;
        }

        // Problem we have no query
        if (!Queries.isQuery(sourceFileQuery)) {

            System.err.println("The first argument is expected to be an SQL file or a SQL query containing the SELECT keyword.");
            if (isRegularFile) {
                System.err.println("The first argument (" + arg0 + ") is a file but its content below does not contain the SELECT word in the first positions");
                System.err.println("Query: \n" + Strings.toStringNullSafe(sourceFileQuery));
            } else {
                System.err.println("The first argument is not a file and its value below does not contain the SELECT word in the first positions");
                System.err.println("Arg Value: \n" + Strings.toStringNullSafe(arg0));
            }
            CliUsage.print(cliParser.getCommand());
            System.exit(1);

        }


        CliTimer cliTimer = CliTimer.getTimer("download").start();

        System.out.println("Download process started");
        Database database = Databases.of(Db.CLI_DATABASE_NAME_TARGET)
                .setUrl(sourceURL)
                .setDriver(sourceDriver);

        LOGGER.info("Connection successful - Querying and Downloading the data");
        int exitStatus;
        try(
            QueryDef queryDef = database.getQuery(sourceFileQuery);
        ) {
             exitStatus = download(queryDef, pathDownloadFile, clobInApartFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Feedback
        cliTimer.stop();

        System.out.printf("Response Time to downloader the data: %s (hour:minutes:seconds:milli)%n", cliTimer.getResponseTime());
        System.out.printf("       Ie (%d) milliseconds%n", cliTimer.getResponseTimeInMilliSeconds());

        // Exit
        if (exitStatus != 0) {
            System.out.println("Error ! (" + exitStatus + ") errors were seen.");
        } else {
            System.out.println("Success ! No errors were seen.");
        }
        System.exit(exitStatus);

    }

    /**
     * Put the result of a query in a CSV file
     *
     * @param query           - the query to download
     * @param clobInApartFile - do we put the clob values in an apart file (not in the downloaded file)
     * @param outputPath      - the file path where to crete the output file
     */
    public static int download(QueryDef query, Path outputPath, Boolean clobInApartFile) {

        try (
                SqlSelectStream stream = Streams.getSqlSelectStream(query);
        ) {
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
//                    final Object o = csvRow.of(0);
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


}
