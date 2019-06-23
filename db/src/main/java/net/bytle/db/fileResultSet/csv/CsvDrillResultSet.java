package net.bytle.db.fileResultSet.csv;

import java.io.Closeable;
import java.nio.file.Path;
import java.sql.*;
import java.util.logging.Logger;

/**
 * Created by gerard on 19-06-2017.
 *
 * A JDBC connection with Drill directly
 * A drillbit must be running on the default port
 */
public class CsvDrillResultSet {

    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());

    private final Path path;
    private Connection connection;

    public CsvDrillResultSet(Path path) {
        this.path = path;
    }


    public ResultSet getResultSet() {


        try {
            Class.forName("org.apache.drill.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        Statement statement = null;
        try {


            // If you are connecting to a local embedded instance (without Zookeeper),
            // you should use drillbit host directly like:
            // Embedded mode installs Drill locally on your machine.
            // Embedded mode is a quick way to install and try Drill without having to perform any configuration tasks.
            // Installing Drill in embedded mode configures the local Drillbit service to start automatically when you launch the Drill shell.
            String url = "jdbc:drill:drillbit=localhost:31010;schema=dfs";

            // With zookeeper - Embedded mode
            //String url = "jdbc:drill:zk=local;schema=dfs";

            connection = DriverManager.getConnection(url);
            statement = connection.createStatement();
//            String query = "SELECT * from `" + path.toAbsolutePath().toString() + "`";
            String query = "select * from table(`" + path.toAbsolutePath().toString() + "` (type => 'text', fieldDelimiter => ',', extractHeader => true))";
            LOGGER.info(query);
            ResultSet resultSet = statement.executeQuery(query);
            return resultSet;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }


}
