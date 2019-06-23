package net.bytle.db.queryExecutor;

import net.bytle.db.DbLoggers;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;

public class QueryExecutor implements IExecutable {

    private Logger LOGGER = DbLoggers.LOGGER_DB_QUERY;

    private List<String[]> parameters = new ArrayList<String[]>();
    private boolean parameterFile = false;
    private Connection connection;
    private Statistics statistics;

    private QueryExecutorProperties threadEnvironment;

    private static final String DATE_PATTERN = "yyyy-MM-dd HH.mm.ss";


    //CSV file header
    private static final Object[] CSV_FILE_HEADER = {"Counter", "Start Time", "RowNumber", "Status", "Description", "End time", "Duration Sec"};

    // TODO: Because every thread as it's own environemnt, why do we need a setup
    // Can it not be done in the run function ?
    @Override
    public IExecutable setup(String threadName, Properties properties) throws Exception {

        // Logger Name is normally hierarchic "com.foo.bar"
        // We add the thread name
        String loggerName = QueryExecutor.class.getCanonicalName();
        LOGGER = Logger.getLogger(loggerName);

        // Property
        threadEnvironment = QueryExecutorProperties.build(threadName, properties);

        // Get connection
        connection = DriverManager.getConnection(
                threadEnvironment.connectionString.trim(),
                threadEnvironment.username.trim(),
                threadEnvironment.password.trim()
        );


        if (threadEnvironment.parameters != null) {
            threadEnvironment.parameters = threadEnvironment.parameters.trim();
            parameterFile = true;

            if (threadEnvironment.parameters.startsWith("file://")) {
                List<String> list = FileUtils.readFileToArrayList(threadEnvironment.parameters.replace("file://", ""));

                outerloop:
                for (int i = 0; i <= threadEnvironment.repetitions / list.size(); i++)
                    for (String line : list) {
                        parameters.add(line.split(","));
                        if (parameters.size() == threadEnvironment.repetitions)
                            break outerloop;
                    }

                if (threadEnvironment.random)
                    Collections.shuffle(parameters);

            } else {
                parameters.add(threadEnvironment.parameters.split(","));
            }
        }

        LOGGER.info(String.valueOf(threadEnvironment));

        if (threadEnvironment.setup != null) {
            for (String query : threadEnvironment.setup.trim().split("~")) {
                if (!query.trim().equals("")) {
                    LOGGER.info("Thread " + threadEnvironment.id + " setting up using query " + query);
                    Statement s = connection.createStatement();
                    s.execute(query);
                    s.close();
                }
            }
        }

        return this;
    }

    private String fillParameters(String query, String[] parameters) {
        int i = 1;
        for (String s : parameters)
            query = query.replaceAll("%" + (i++), s.trim());
        return query;
    }

    @Override
    @SuppressWarnings("EmptyCatchBlock")
    public void run() {

        long delay = 0;
        long run = 1;
        long start;
        long duration;
        String lastError = null;

        statistics = new Statistics(threadEnvironment.repetitions, threadEnvironment.id);
        long startSecond = statistics.startUnixTimeInMillis;
        int startCount = 0;
        long end;

        // Input Data
        // Header are added to the output and must be then accessible in the outer scope
        CSVRecord inputHeaderRecord;
        // The input records that we need to process
        Iterator<CSVRecord> inputRecords;
        if (threadEnvironment.query.startsWith("file://")) {
            String queryFilepath = threadEnvironment.query.replace("file://", "");
            try {
                Reader in = new FileReader(queryFilepath);
                inputRecords = CSVFormat.RFC4180.parse(in).iterator();
                if (inputRecords.hasNext()) {
                    inputHeaderRecord = inputRecords.next();
                } else {
                    throw new RuntimeException("This file seems to have no data. Cannot read the headers.");
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Actually we accepts only files");
            // The code below is written to take also only on SQL but
            // is no more compatible with the loop over inputRecords
            // An wrapper must be created or an if then else
        }

        // CSV
        CSVPrinter csvFilePrinter;
        try {

            // Initialize FileWriter object
            FileWriter fileWriter = new FileWriter(threadEnvironment.id + "." + "csv");
            // Create the CSVFormat object with "\n" as a record delimiter
            CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator("\n");
            // Initialize CSVPrinter object
            csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);
            // Create CSV file header
            List csvOutputHeaderData = new ArrayList();

            // Test if data is present
            if (inputHeaderRecord.size() == 0) {
                throw new RuntimeException("The data seems to have no columns. Nothing to do then.");
            }

            // Create the headers
            Collections.addAll(csvOutputHeaderData, CSV_FILE_HEADER);
            // The first column is the SQL and it goes at the end as it has no analytics value
            // We start then at 1 in the loop
            if (inputHeaderRecord.size() >= 1) {
                for (int i = 1; i < inputHeaderRecord.size(); i++) {
                    csvOutputHeaderData.add(inputHeaderRecord.get(i));
                }
            }
            // And add the sql columns at the end
            csvOutputHeaderData.add(inputHeaderRecord.get(0));

            csvFilePrinter.printRecord(CSV_FILE_HEADER);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        // The load can be repeated
        for (int i = 0; i < threadEnvironment.repetitions; i++) {

            // Query Split to take all queries
            Integer numberOfQuery = 0;

            while (inputRecords.hasNext()) {

                // The record to process
                CSVRecord inputRecord = inputRecords.next();

                // The data to output
                List csvOutputData = new ArrayList();

                // The query must be in the first column
                String executableQuery = inputRecord.get(0);
                // Line without data must not be ran
                if (executableQuery != null && executableQuery.length() > 0) {
                    try {

                        // Query Id
                        numberOfQuery++;
                        csvOutputData.add(numberOfQuery);
                        LOGGER.info("Query number " + numberOfQuery);

                        // Bind variable as string: bad, bad
                        if (parameterFile)
                            executableQuery = fillParameters(executableQuery, parameters.get(i));

                        // Start time
                        start = System.currentTimeMillis();

                        csvOutputData.add(new SimpleDateFormat(DATE_PATTERN).format(start));

                        // Fetch counter is here because we need it
                        // to be written in the CSV file (before the try if something go wrong
                        int fetchRowCounter = 0;

                        // The statement operation
                        try {

                            // Statement create (for each query ???)
                            Statement statement = connection.createStatement();

                            // Time out ?
                            if (threadEnvironment.queryTimeout != -1) {
                                statement.setQueryTimeout(threadEnvironment.queryTimeout);
                                LOGGER.info("Timeout: " + threadEnvironment.queryTimeout);
                            } else {
                                LOGGER.info("No Timeout");
                            }


                            if (!executableQuery.toLowerCase().contains("select")) {

                                LOGGER.info("Execute (Not Query)");
                                statement.execute(executableQuery);

                            } else {

                                LOGGER.info("ExecuteQuery");

                                ResultSet resultSet = statement.executeQuery(executableQuery);
                                if (threadEnvironment.fetch == 0) {

                                    LOGGER.info("No Fetch");

                                } else {

                                    LOGGER.info("Fetch a maximum of (" + threadEnvironment.fetch + ") inputRecords");


                                    // Below are the variable needed to downloader the data in a stream fashion
                                    // Because we process it with if then else, we need to put them here
                                    // to have them in the scope
                                    // File
                                    FileWriter outputStream = null;
                                    // The file content
                                    StringBuilder stringBuilder = new StringBuilder();
                                    // The number of column
                                    int columnCount = resultSet.getMetaData().getColumnCount();
                                    // The file Name
                                    String fileName = null;
                                    if (threadEnvironment.download) {

                                        // Creation of the file
                                        String threadName = Thread.currentThread().getName();
                                        fileName = threadName + "." + numberOfQuery + ".csv";
                                        File dir = new File(threadName);
                                        if (!dir.exists()) {
                                            dir.mkdir();
                                        }
                                        fileName = dir.getName() + File.separator + fileName;
                                        outputStream = new FileWriter(fileName);

                                        // Put the headers in the file
                                        for (int j = 1; j <= columnCount; j++) {
                                            if (j != 1) {
                                                stringBuilder.append(";");
                                            }
                                            stringBuilder.append(resultSet.getMetaData().getColumnTypeName(j));
                                        }
                                        stringBuilder.append(System.getProperty("line.separator"));
                                        outputStream.write(stringBuilder.toString());

                                    }

                                    // Fetch
                                    int row = threadEnvironment.fetch;

                                    while (resultSet.next() && --row >= 0) {
                                        fetchRowCounter++;
                                        if (threadEnvironment.download) {
                                            stringBuilder = new StringBuilder();
                                        }
                                        for (int j = 1; j <= columnCount; j++) {
                                            if (threadEnvironment.download) {
                                                if (j != 1) {
                                                    stringBuilder.append(";");
                                                }
                                                if (resultSet.getMetaData().getColumnTypeName(j).equals("DOUBLE")) {
                                                    double aDouble = round(resultSet.getDouble(j), 2);
                                                    stringBuilder.append(aDouble);
                                                } else {
                                                    stringBuilder.append(resultSet.getString(j));
                                                }
                                            } else {
                                                // We fetch without doing something
                                                resultSet.getObject(j);
                                            }
                                        }
                                        if (threadEnvironment.download) {
                                            stringBuilder.append(System.getProperty("line.separator"));
                                            outputStream.write(stringBuilder.toString());
                                        }
                                    }
                                    resultSet.close();
                                    if (threadEnvironment.download) {
                                        outputStream.close();
                                        LOGGER.info(fetchRowCounter + " rows fetched and written in the file " + fileName);
                                    } else {
                                        LOGGER.info("No downloader of the data");
                                        LOGGER.info(fetchRowCounter + " rows were fetched");
                                        LOGGER.info(fetchRowCounter * columnCount + " cell were fetched");
                                    }

                                }
                            }

                            statement.close();

                            // Number of Row fetched
                            csvOutputData.add(fetchRowCounter);
                            csvOutputData.add("Success");
                            csvOutputData.add("");

                        } catch (SQLException ex) {
                            String message = ex.getMessage();

                            csvOutputData.add(fetchRowCounter);
                            csvOutputData.add("Failure");
                            csvOutputData.add(message);

                            LOGGER.severe("Thread " + threadEnvironment.id + ", QueryId " + numberOfQuery + ", Message:" + message + " Sql State:" + ex.getSQLState());
                            ex.printStackTrace();
                            // Log the query
                            LOGGER.severe("Query:" + executableQuery);

                            if (ex.getMessage().contains("java.io.IOException")) {
                                // java.io.IOException: prepare query failed[nQSError: 43113] Message returned from OBIS.
                                LOGGER.severe("This is java.io.IOException !");
                            } else if (message.contains("java.net.ConnectException")) {
                                LOGGER.severe("This is java net connection, the server seems to be dead. Stopping the thread.");
                                break;
                            } else {
                                LOGGER.severe("Unknown type of exception");
                                //break;
                            }

                        }

                        end = System.currentTimeMillis();
                        csvOutputData.add(new SimpleDateFormat(DATE_PATTERN).format(end));

                        statistics.storeRunTime(i, end - start);
                        final long durationSec = (end - start) / 1000;
                        LOGGER.info("Execution Finished in " + durationSec + " sec");

                        csvOutputData.add(durationSec);

                        if (end - startSecond >= 1000) {
                            statistics.storeTps(startSecond / 1000, i - startCount);
                            startSecond = end;
                            startCount = i;
                        }

                        if (threadEnvironment.maxTps != 0 && run * threadEnvironment.maxTps == i) {
                            duration = end - statistics.startUnixTimeInMillis;
                            if (duration < run * 1000)
                                delay += Math.round(((run * 1000.0) - duration) / (run * threadEnvironment.maxTps));
                            else
                                delay -= Math.round((duration - (run * 1000.0)) / (run * threadEnvironment.maxTps));
                            run++;
                        }

                        long millis = delay = delay < 0 ? 0 : delay;
                        LOGGER.info("Sleep " + millis / 1000 + " sec");
                        sleep(millis);

                        // Add the extra data from the input data
                        if (inputRecord.size() >= 1) {
                            for (int k = 1; k < inputRecord.size(); k++) {
                                csvOutputData.add(inputRecord.get(k));
                            }
                        }
                        // Add the SQL at the end
                        // Suppressing the end of line is now not needed as the input query is on one line but yeah ...
                        String executableQueryWithoutEOL = executableQuery.replaceAll("\n\r", " ");
                        csvOutputData.add(executableQueryWithoutEOL);

                        // Record in the CSV
                        csvFilePrinter.printRecord(csvOutputData);
                        csvFilePrinter.flush();

                    } catch (Exception ex) {
                        String newError = ex.getMessage();
                        if (newError == null)
                            newError = "Null pointer exception";

                        if (!newError.equals(lastError))
                            LOGGER.severe("Thread " + threadEnvironment.id + " " + (lastError = newError));
                    }
                }
            }
        }

        statistics.storeTps(startSecond / 1000, threadEnvironment.repetitions - startCount);

        statistics.calculate();

        // Close the CSV file
        try {
            csvFilePrinter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            if (connection != null)
                connection.close();
        } catch (SQLException ex) {
            LOGGER.severe("Thread " + threadEnvironment.id + " " + ex.getMessage());
        }

        LOGGER.info("Thread " + threadEnvironment.id + " has finished");
    }

    @Override
    public IStatistics getStatistics() {
        return statistics;
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
