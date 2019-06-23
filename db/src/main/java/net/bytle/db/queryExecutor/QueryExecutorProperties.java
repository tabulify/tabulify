package net.bytle.db.queryExecutor;

import java.util.Properties;

public class QueryExecutorProperties {

    private final static String SEPARATOR = System.getProperty("line.separator");

    private String outputResultSetDir;

    public String connectionString;
    public String username;
    public String password;
    public String query;
    public Integer repetitions;
    public String id;
    public String parameters;
    public Integer maxTps;
    public Boolean random;
    public String setup;
    public Integer fetch;
    public boolean download;
    public Integer queryTimeout;

    public static QueryExecutorProperties build(String threadName, Properties properties) throws Exception {
        QueryExecutorProperties queryExecutorProperties = new QueryExecutorProperties();

        String query = properties.getProperty("query", null);
        String globalQuery;
        if (query != null && query.startsWith("file://"))
            globalQuery = FileUtils.readFileToString(
                    query.replace("file://", ""),
                    SEPARATOR
            );
        else
            globalQuery = query;

        query = properties.getProperty(threadName + ".query", null);

        if (query == null && globalQuery == null)
            throw new Exception("No query could be found for thread " + threadName);


        String connectionString = properties.getProperty(
                threadName + ".connectionString",
                properties.getProperty(
                        "connectionString",
                        null
                )
        );
        if (connectionString == null)
            throw new Exception("No connection string could be found for thread " + threadName);

        String username = properties.getProperty(
                threadName + ".username",
                properties.getProperty(
                        "username",
                        null
                )
        );
        if (username == null)
            throw new Exception("No username could be found for thread " + threadName);

        String password = properties.getProperty(
                threadName + ".password",
                properties.getProperty(
                        "password",
                        null
                )
        );
        if (password == null)
            throw new Exception("No password could be found for thread " + threadName);

        if (password.startsWith("encrypted://")) {
            String masterPassword = properties.getProperty(
                    threadName + ".masterPassword",
                    properties.getProperty(
                            "masterPassword",
                            null
                    )
            );

            if (masterPassword == null)
                throw new Exception("No master password could be found for thread " + threadName);

            password = new Protector(masterPassword).decrypt(password.replaceAll("^encrypted://", ""));
        } else {
            password = password.replaceAll("^plain://", "");
        }

        String propertyName = "fetch";
        String fetch = properties.getProperty(threadName + "." + propertyName, properties.getProperty(propertyName, null));
        if (fetch != null) {
            Boolean fetchBool = NormalizeBoolean(fetch);
            if (fetchBool != null) {
                if (fetchBool) {
                    queryExecutorProperties.fetch = Integer.MAX_VALUE; // fetch all
                } else {
                    queryExecutorProperties.fetch = 0; // Do not fetch
                }
            } else {
                // Fetch may be a number
                try {
                    queryExecutorProperties.fetch = Integer.valueOf(fetch.trim());
                } catch (Exception ex) {
                    System.err.println("The property fetch is not a numeric (" + fetch + ")");
                }
            }
        }

        propertyName = "downloader";
        String propertyValue = properties.getProperty(threadName + "." + propertyName, properties.getProperty(propertyName, null));
        if (propertyValue != null) {
            Boolean downloadBool = NormalizeBoolean(propertyValue);
            if (downloadBool != null) {
                queryExecutorProperties.download = downloadBool;
            } else {
                // Fetch may be a number
                System.err.println("The property " + propertyName + " is not a boolean (" + propertyValue + ")");
            }
        } else {
            queryExecutorProperties.download = false;
        }


        queryExecutorProperties.connectionString = connectionString;
        queryExecutorProperties.username = username;
        queryExecutorProperties.password = password;
        queryExecutorProperties.id = threadName;
        queryExecutorProperties.setup = properties.getProperty(threadName + ".setup", properties.getProperty("setup", null));
        queryExecutorProperties.query = query == null ? globalQuery : query;
        queryExecutorProperties.repetitions = Integer.valueOf(properties.getProperty(threadName + ".repetitions", properties.getProperty("repetitions", "1")));
        queryExecutorProperties.parameters = properties.getProperty(threadName + ".parameters", null);
        queryExecutorProperties.maxTps = Integer.valueOf(properties.getProperty(threadName + ".maxTps", properties.getProperty("maxTps", "0")));
        queryExecutorProperties.random = !properties.getProperty(threadName + ".random", properties.getProperty("random", "n")).toLowerCase().equals("n");
        queryExecutorProperties.queryTimeout = Integer.valueOf(properties.getProperty(threadName + ".queryTimeout", properties.getProperty("queryTimeout", "-1")));
        queryExecutorProperties.outputResultSetDir = properties.getProperty("outputResultSetDir", null);

        return queryExecutorProperties;
    }

    /**
     * @param s
     * @return false or true if it's a boolean otherwise return null
     */
    private static Boolean NormalizeBoolean(String s) {

        if (s == null || s.trim().length() == 0 || s.trim().toLowerCase().startsWith("n") || s.trim().toLowerCase().equals("false") || s.trim().equals("0"))
            return false;
        else if (s.trim().toLowerCase().startsWith("y") || s.trim().toLowerCase().equals("true") || s.trim().equals("all"))
            return true;
        else return null
                    ;

    }

    @Override
    public String toString() {
        return String.format(
                "%s %s @ %s (%d, %s, %d, %s, %s)",
                id,
                username,
                connectionString,
                repetitions,
                parameters == null ? "none" : parameters,
                maxTps,
                random ? "y" : "n",
                fetch == -1 ? "all" : (fetch == 0 ? "none" : fetch)
        );
    }
}
