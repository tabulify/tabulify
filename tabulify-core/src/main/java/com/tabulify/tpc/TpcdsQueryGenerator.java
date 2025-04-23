package com.tabulify.tpc;


import com.tabulify.Tabular;
import com.tabulify.connection.Connection;
import com.tabulify.connection.ConnectionHowTos;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;


public class TpcdsQueryGenerator {

    /**
     * A function helper to create the tpcds queries
     * <p>
     * As the queries are changed, this must run only at will
     * Not during the test run. This is why it's an ignored test
     */
    public static void main(String[] args) {

      // Args
      String datastoreName = ConnectionHowTos.MYSQL_CONNECTION_NAME;

      // Dialect by Database
      Map<String,String> dialects = new HashMap<>();
      dialects.put(ConnectionHowTos.SQLITE_CONNECTION_NAME,"sqlite");
      dialects.put(ConnectionHowTos.POSTGRESQL_CONNECTION_NAME, "netezza");
      dialects.put(ConnectionHowTos.SQLSERVER_CONNECTION_NAME, "sqlserver");
      dialects.put(ConnectionHowTos.ORACLE_CONNECTION_NAME, "oracle");
      // For Mysql: https://github.com/gregrahn/tpcds-kit/issues/13
      dialects.put(ConnectionHowTos.MYSQL_CONNECTION_NAME, "netezza");

      Path tpcDsHomePath = Paths.get(System.getenv("HOME"),"code","tpcds-kit");

      try (Tabular tabular = Tabular.tabularWithCleanEnvironment()) {
        Connection connection = tabular.loadHowtoConnections().getConnection(datastoreName);
        Path buildPath = tpcDsHomePath.resolve("tools");
        TpcdsQgen tpcdsQgen = TpcdsQgen
          .create(connection)
          .setDialect(dialects.get(datastoreName))
          .setDsqGenDirectory(buildPath)
          .setOutputDirectory(Paths.get("tabulify-jdbc/src/main/sql/tpcds/" + connection.getName()))
          .setDistributionFile(buildPath.resolve("tpcds.idx"))
          .setQueryTemplatesDirectory(tpcDsHomePath.resolve("query_templates"));

        String feedback = tpcdsQgen.start();
        System.out.println(feedback);
      }

    }

}
