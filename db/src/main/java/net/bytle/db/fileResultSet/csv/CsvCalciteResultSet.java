package net.bytle.db.fileResultSet.csv;

import java.nio.file.Path;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Created by gerard on 28-06-2017.
 */
public class CsvCalciteResultSet  {

    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[1].getClassName());
    private Connection connection;
    private final Path path;
    private ResultSet resultSet;

    public CsvCalciteResultSet(Path path)  {

        this.path = path;


    }


    /**
     * Get a result set
     *
     * @return
     */
    public ResultSet getResultSet() {
        Properties info = new Properties();
        String s = path.toAbsolutePath().getParent().toString();
        LOGGER.info(s);
        info.put("model",
                "inline:"
                        + "{\n"
                        + "  version: '1.0',\n"
                        + "  defaultSchema: 'default',"
                        + "   schemas: [\n"
                        + "     {\n"
                        + "       type: 'custom',\n"
                        + "       name: 'default',\n"
                        + "       factory: 'org.apache.calcite.adapter.csv.CsvSchemaFactory',\n"
                        + "       operand: {\n"
                        + "         directory: '"+ s.replace("\\","\\\\")+"'\n"
                        + "       }\n"
                        + "     }\n"
                        + "   ]\n"
                        + "}");


        try {
            connection = DriverManager.getConnection("jdbc:calcite:", info);
            Statement statement = connection.createStatement();
            String fileName = path.getFileName().toString();
            String fileNameWithoutExtension = fileName.substring(0,fileName.length()-4);
            resultSet = statement.executeQuery("select * from \""+ fileNameWithoutExtension +"\"");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return resultSet;

    }

    /**
     * Auto-closeable
     */
    public void close() throws Exception {
        resultSet.close();
        connection.close();
    }

    /**
     * The feedbackFrequency is the number of row that must be read before
     * a feedback is given.
     * Generally, this value is the same than the fetchSize. See {@link ResultSet#setFetchSize(int)}
     *
     * @return
     */
    public Integer getFeedBackFrequency() {
        try {
            return resultSet.getFetchSize();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
