package com.tabulify.tpc;


import com.tabulify.Tabular;
import com.tabulify.connection.Connection;
import com.tabulify.connection.ConnectionHowTos;

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

      //String datastoreName = DataStoresHowTos.SQLITE_DATASTORE_NAME;
      String datastoreName = ConnectionHowTos.POSTGRESQL_CONNECTION_NAME;
      Map<String,String> dialects = new HashMap<>();
      dialects.put(ConnectionHowTos.SQLITE_CONNECTION_NAME,"sqlite");
      dialects.put(ConnectionHowTos.POSTGRESQL_CONNECTION_NAME, "netezza");

      Map<String, String> projects = new HashMap<>();
      projects.put(ConnectionHowTos.POSTGRESQL_CONNECTION_NAME,"db-jdbc");

      try (Tabular tabular = Tabular.tabularWithCleanEnvironment()) {
        Connection connection = tabular.getConnection(datastoreName);
        TpcdsQgen tpcdsQgen = TpcdsQgen
          .create(connection)
          .setDialect(dialects.get(datastoreName))
          .setDsqGenDirectory(Paths.get("D:/code/tpcds-xp/build"))
          .setOutputDirectory(Paths.get(projects.get(datastoreName)+"/src/main/sql/tpcds/" + connection.getName()))
          .setDistributionFile(Paths.get("D:/code/tpcds-xp/build/tpcds.idx"))
          .setQueryTemplatesDirectory(Paths.get("D:/code/tpcds-xp/query_templates"));

        String feedback = tpcdsQgen.start();
        System.out.println(feedback);
      }

    }

}
