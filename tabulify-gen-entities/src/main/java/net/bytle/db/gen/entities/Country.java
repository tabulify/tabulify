package net.bytle.db.gen.entities;

import net.bytle.db.Tabular;
import net.bytle.db.csv.CsvDataPath;
import net.bytle.db.connection.Connection;
import net.bytle.db.connection.ConnectionHowTos;
import net.bytle.db.fs.sql.SqlQuery;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.type.Uris;
import net.bytle.fs.Fs;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;

import static net.bytle.db.gen.entities.EntitiesProperties.*;

public class Country {


  public static void main(String[] args) throws IOException {
    Country.generate();
  }

  public static void generate() throws IOException {

    try (Tabular tabular = Tabular.tabularWithCleanEnvironment()) {

      /**
       * A db is used as SQL engine
       */

      // Fichier des prenoms
      // https://www.insee.fr/fr/statistiques/2540004
      //
      URI uri = Uris.of("https://raw.githubusercontent.com/lukes/ISO-3166-Countries-with-Regional-Codes/master/all/all.csv");
      Path httpPath = Paths.get(uri);
      CsvDataPath httpCsv = ((CsvDataPath) tabular.getDataPath(httpPath))
        .setDelimiterCharacter(',')
        .setHeaderRowId(1)
        .setQuoteCharacter('"')
        .createRelationDef()
        .addColumn("name", Types.VARCHAR)
        .addColumn("alpha-2", Types.CHAR, 2)
        .addColumn("alpha-3", Types.CHAR, 3)
        .addColumn("country-code", Types.INTEGER)
        .addColumn("iso_3166-2-code", Types.VARCHAR)
        .addColumn("region", Types.VARCHAR)
        .addColumn("sub-region", Types.VARCHAR)
        .addColumn("intermediate-region", Types.VARCHAR)
        .addColumn("region-code", Types.INTEGER)
        .addColumn("sub-region-code", Types.INTEGER)
        .addColumn("intermediate-region-code", Types.INTEGER)
        .getDataPath();

      String mainEntity = COUNTRY;
      DataPath localCsv = tabular.getDataPath(Fs.getTempDirectory().resolve(mainEntity).resolve(mainEntity + ".csv"));
      if (!Tabulars.exists(localCsv)) {
        Tabulars.copy(httpCsv, localCsv);
      }

      /**
       * Db Table
       */
      Connection connection = tabular.getConnection(ConnectionHowTos.POSTGRESQL_CONNECTION_NAME);
      DataPath countryTable = connection.getDataPath(mainEntity);
      if (!Tabulars.exists(countryTable)) {
        Tabulars.dropIfExists(countryTable);
        Tabulars.copy(httpCsv, countryTable);
      }

      /**
       * CSV creation
       */
      List<String> entities = Arrays.asList(COUNTRY, REGION, SUBREGION);
      entities.forEach(entity -> {

        Path targetPath = EntitiesProperties.BASE_PATH.resolve(entity).resolve(entity + ".csv");
        CsvDataPath targetDataPath = CsvDataPath.createFrom(tabular.getCurrentLocalDirectoryConnection(), targetPath)
          .setHeaderRowId(1);

        if (!Tabulars.exists(targetDataPath)) {
          Path sql = EntitiesProperties.SQL_BASE.resolve(entity).resolve(entity + ".sql");
          String query = SqlQuery.createFromPath(sql).toString();
          DataPath queryDataPath = connection.createScriptDataPath(query);
          Tabulars.dropIfExists(targetDataPath);
          Tabulars.copy(queryDataPath, targetDataPath);
        }

      });
    }

  }
}

