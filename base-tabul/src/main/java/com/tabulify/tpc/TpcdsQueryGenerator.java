package com.tabulify.tpc;


import com.tabulify.Tabular;
import com.tabulify.connection.Connection;
import com.tabulify.type.KeyNormalizer;

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
      KeyNormalizer connectionName = KeyNormalizer.createSafe("mysql");

      // Dialect by Database Scheme, connection name
      Map<String, String> dialects = new HashMap<>();
      dialects.put("sqlite", "sqlite");
      dialects.put("postgres", "netezza");
      dialects.put("sqlserver", "sqlserver");
      dialects.put("oracle", "oracle");
      // For Mysql: https://github.com/gregrahn/tpcds-kit/issues/13
      dialects.put("mysql", "netezza");

      Path tpcDsHomePath = Paths.get(System.getenv("HOME"),"code","tpcds-kit");

      try (Tabular tabular = Tabular.tabularWithCleanEnvironment()) {
        Connection connection = tabular.getConnection(connectionName);
        Path buildPath = tpcDsHomePath.resolve("tools");
        TpcdsQgen tpcdsQgen = TpcdsQgen
          .create(connection)
          .setDialect(dialects.get(connectionName))
          .setDsqGenDirectory(buildPath)
          .setOutputDirectory(Paths.get("tabulify-tabul-jdbc/src/main/sql/tpcds/" + connection.getName()))
          .setDistributionFile(buildPath.resolve("tpcds.idx"))
          .setQueryTemplatesDirectory(tpcDsHomePath.resolve("query_templates"));

        String feedback = tpcdsQgen.start();
        System.out.println(feedback);
      }

    }

}
