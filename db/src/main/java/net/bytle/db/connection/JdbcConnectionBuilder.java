/*
 * Copyright (c) 2014. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package net.bytle.db.connection;


import net.bytle.db.DbLoggers;
import net.bytle.db.database.Database;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * User: gerard
 * Date: 2/25/14
 * Time: 11:47 AM
 * A class to represent all connection:
 * * Sftp
 * * Local
 * * JDBC
 *
 * See {@link Database#getCurrentConnection()}
 */

@Deprecated
public class JdbcConnectionBuilder {

    private static final Logger LOGGER = DbLoggers.LOGGER_DB_ENGINE;

    private String driver;
    private final String url;
    private String user = null;
    private String password;
    private String postStatement;

    public JdbcConnectionBuilder(String user, String password, String urlString, String driver) {
        if (urlString == null) {
            throw new IllegalArgumentException("The url cannot be null");
        } else {
            url = urlString;
        }
        this.user = user;
        this.password = password;
        this.driver = driver;
    }

    public JdbcConnectionBuilder postStatement(String postStatement) {
        this.postStatement = postStatement;
        return this;

    }

    public Connection build() {

        // Extract the server part of the JDBC url to be able
        // to give automatically the driver if unknown
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        URIExtended uriExtended = new URIExtended(uri);
        String driver = uriExtended.getDriver();


        if (driver != null) {
            LOGGER.fine("Driver was null, trying to load the default driver " + driver);
            loadDriver(driver);
        } else {
            LOGGER.fine("No default driver specified in the command line and in the code for the server " + uriExtended.getServer());
        }


        LOGGER.info("Trying to connect to the " + uriExtended.getServer() + " server  (The login and password are not shown for security reason)");
        Connection connection;

        try {
            if (user != null) {
                connection = DriverManager.getConnection(url, user, password);
            } else {

                connection = DriverManager.getConnection(url);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Connection error occurs: " + e.getMessage()+", url:"+url, e);
        }
        LOGGER.info("Connected");


        // Post Connection statement (such as alter session set current_schema)

        if (postStatement != null) {
            CallableStatement callableStatement;
            try {
                callableStatement = connection.prepareCall(postStatement);
                LOGGER.info("Post Statement was executed " + postStatement);
                callableStatement.execute();
            } catch (SQLException e) {
                throw new RuntimeException("Post Connection error occurs: " + e.getMessage(), e);
            }
        }

        return connection;

    }



    private void loadDriver(String driver) {
        try {
            Class.forName(driver);
            LOGGER.info("Driver " + driver + " loaded");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("The driver Class (" + driver + ") could not be loaded. An error occurs: " + e.getMessage(), e);
        }
    }

}


