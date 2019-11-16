package net.bytle.db.cli;

import net.bytle.cli.*;
import net.bytle.db.DatabasesStore;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;
import net.bytle.db.engine.Queries;

import net.bytle.db.model.QueryDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPaths;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.Streams;
import net.bytle.db.transfer.TransferListener;
import net.bytle.db.uri.DataUri;
import net.bytle.log.Log;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Clob;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.bytle.db.cli.Words.*;

/**
 * Created by gerard on 08-12-2016.
 * To download data
 */
public class DbQueryDownload {


    private static final Log LOGGER = Db.LOGGER_DB_CLI;

    private static final String ARG_NAME = "sqlFileDataUri...|sourceDataUri targetDataUri";
    private static final String TARGET_DIRECTORY = "DownloadPathDir";
    private static final String TARGET_SOURCE_MODE = "--source-target-mode";


    public static void run(CliCommand cliCommand, String[] args) {

        cliCommand.optionOf(SOURCE_DATA_URI)
                .setDescription("Only in default mode, A data Uri that defines the connection where all queries should run. It works with the default mode.");
        cliCommand.argOf(ARG_NAME)
                .setDescription("In default mode, a succession of sql files defined by one or more data Uri" +System.lineSeparator()
                        "In target/source mode, a succession of query data uri followed by its target uri");
        cliCommand.optionOf(TARGET_DIRECTORY)
                .setDescription("Only in default mode, a directory that defines where the data should be downloaded");
        cliCommand.flagOf(TARGET_SOURCE_MODE)
                .setDescription("if this flag is present, the source target mode will be used (ie a succession of source/target data uri as argument)");
        String example = cliCommand.getName() + CliParser.PREFIX_LONG_OPTION +SOURCE_DATA_URI+ " @sqlite QueryToDownload.sql \n";
        cliCommand.addExample(example);
        cliCommand.optionOf(DATABASE_STORE);

        // Parse
        CliParser cliParser = Clis.getParser(cliCommand, args);

        // Database Store
        final Path storagePathValue = cliParser.getPath(DATABASE_STORE);
        DatabasesStore databasesStore = DatabasesStore.of(storagePathValue);

        // The data with the transfers
        Map<DataPath,DataPath> transfers = new HashMap<>();

        // Mode
        Boolean targetSourceMode = cliParser.getBoolean(TARGET_SOURCE_MODE);
        if (!targetSourceMode){
            LOGGER.info("Mode: Default, download of one or several query file");

            String stringSourceDataUri = cliParser.getString(SOURCE_DATA_URI);
            if (stringSourceDataUri == null) {
                throw new RuntimeException("In the default mode, the option ("+SOURCE_DATA_URI+") is mandatory");
            }
            DataUri sourceDataUri = DataUri.of(stringSourceDataUri);
            DataPath sourceDataPath = DataPaths.of(databasesStore,sourceDataUri);

            DataPath dataPathDownloadLocation;
            String downloadPathArg = cliParser.getString(OUTPUT_DATA_URI);
            if (downloadPathArg != null) {
                dataPathDownloadLocation = DataPaths.of(DataUri.of(downloadPathArg));
            } else {
                dataPathDownloadLocation = DataPaths.of(Paths.get("."));
            }

            List<String> sqlFileUris = cliParser.getStrings(ARG_NAME);
            if (sqlFileUris.size() != 1) {
                System.err.println("In default mode, at minimum a file data uri must be given");
                CliUsage.print(cliCommand);
                System.exit(1);
            }

            // Creating the transfer map (ie the source query data path/target data path)
            transfers = new HashMap<>();
            for (String stringSqlFileUri:sqlFileUris) {

                // Source
                DataUri sqlFileUri = DataUri.of(stringSqlFileUri);
                DataPath sqlDataPath = DataPaths.of(databasesStore,sqlFileUri);
                String sourceFileQuery = Tabulars.getString(sqlDataPath);
                if (!Queries.isQuery(sourceFileQuery)) {
                    System.err.println("The data path (" + sqlDataPath + ") does not contains a query.");
                    CliUsage.print(cliParser.getCommand());
                    System.exit(1);
                }
                DataPath sourceQueryDataPath = DataPaths.ofQuery(sourceDataPath,sourceFileQuery);

                // Target
                DataPath targetDataPath = DataPaths.childOf(dataPathDownloadLocation,sqlDataPath.getName());

                // Transfer
                transfers.put(sourceQueryDataPath,targetDataPath);

            }


        } else {
            LOGGER.info("Mode: Target/Source, source uri followed by the target Uri.");
            throw new RuntimeException("Not yet implemented, sorry");
        }


        System.out.println("Download process started");

        CliTimer totalTimer = CliTimer.getTimer("total").start();

        for (Map.Entry<DataPath, DataPath> transfer:transfers.entrySet()) {

            DataPath transferSource = transfer.getKey();
            DataPath transferTarget = transfer.getValue();

            String transferName = transferSource.getName()+transferTarget.getName();
            CliTimer cliTimer = CliTimer.getTimer(transferName).start();

            TransferListener transferListener = Tabulars.transfer(transferSource,transferTarget);

            cliTimer.stop();
        }
        totalTimer.stop();

        System.out.printf("Response Time to download the data: %s (hour:minutes:seconds:milli)%n", totalTimer.getResponseTime());
        System.out.printf("       Ie (%d) milliseconds%n", totalTimer.getResponseTimeInMilliSeconds());

        // Exit
        if (exitStatus != 0) {
            System.out.println("Error ! (" + exitStatus + ") errors were seen.");
            System.exit(exitStatus);
        } else {
            System.out.println("Success ! No errors were seen.");
        }


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
