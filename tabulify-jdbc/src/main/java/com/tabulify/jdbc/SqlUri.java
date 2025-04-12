package com.tabulify.jdbc;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

/**
 * Not all JDBC URL are URI compliants, this class wraps this fact
 *
 */
public class SqlUri {

    private static final Logger LOGGER = Logger.getLogger(SqlUri.class.getPackage().getName());

    private String server;

    private URI uri;

    public SqlUri(URI uri) {
        this.uri = uri;
    }

    public SqlUri(String url) {
        try {
            this.uri = new URI(url);
        } catch (URISyntaxException e) {
            // The Sqlite URL is not URI compliant
            // jdbc:sqlite:///C:\\Users\\gerard\\AppData\\Local\\Temp\\bytle-db\\defaultDb.db
            if (url.contains("sqlite")) {
                this.server = "sqlite";
            } else {
                final String msg = "The URL (" + url + ") is not URI compliant.";
                LOGGER.severe(msg);
                System.err.println(msg);
                throw new RuntimeException(e);
            }

        }
    }

    public String getDriver() {


        String driver = null;

        String server = getServer();
        if (server == null) {
            return null;
        }

        switch (server) {
            case "sap":
                driver = "com.sap.db.jdbc.Driver";
                break;
            case "oraclebi":
                driver = "oracle.bi.jdbc.AnaJdbcDriver";
                break;
            case "timesten":
                driver = "com.timesten.jdbc.TimesTenDriver";
                break;
            default:
                LOGGER.fine("The driver for the server (" + server + ") is not known.");
        }

        return driver;

    }

    public String getServer() {
        // Get the whole uri but without the scheme (Example: sap://linuxhana:30015/?user=login&password=1234)
        if (server != null) {
            return server;
        } else {
            String schemeSpecificPart = uri.getSchemeSpecificPart();
            final int endIndex = schemeSpecificPart.indexOf(":");
            if (endIndex == -1) {
                return null;
            } else {
                server = schemeSpecificPart.substring(0, endIndex);
                return server;
            }
        }
    }
}
