package net.bytle.db.gen.entities;

import net.bytle.db.Tabular;
import net.bytle.db.csv.CsvDataPath;
import net.bytle.db.connection.Connection;
import net.bytle.db.connection.ConnectionHowTos;
import net.bytle.db.fs.sql.SqlQuery;
import net.bytle.db.fs.FsDataPath;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.type.Uris;
import net.bytle.fs.Fs;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.sql.Types;

import static net.bytle.db.gen.entities.EntitiesProperties.FIRSTNAME;

public class FirstNameFr {

  public static void main(String[] args) throws IOException {
    try (Tabular tabular = Tabular.tabularWithCleanEnvironment()) {

      /**
       * A db is used as SQL engine
       */

      Path finalNamesTarget = EntitiesProperties.BASE_PATH.resolve(FIRSTNAME).resolve(FIRSTNAME + "_fr.csv");
      CsvDataPath targetNameTable = CsvDataPath.createFrom(tabular.getCurrentLocalDirectoryConnection(), finalNamesTarget)
        .setHeaderRowId(1)
        .getOrCreateRelationDef()
        .getDataPath();


      // Fichier des prenoms
      // https://www.insee.fr/fr/statistiques/2540004
      //
      String name = "nat2019";
      URI uri = Uris.of("https://www.insee.fr/fr/statistiques/fichier/2540004/" + name + "_csv.zip");
      Path path = Paths.get(uri);
      DataPath zipSource = tabular.getDataPath(path);
      Path tempDirectory = Fs.getTempDirectory();
      Path zipPathLocal = tempDirectory.resolve("FirstNameFr").resolve(zipSource.getName());
      FsDataPath zipLocal = tabular.getDataPath(zipPathLocal);
      if (!Tabulars.exists(zipLocal)) {
        Tabulars.copy(zipSource, zipLocal);
      }

      // Extract the Csv file
      FileSystem zipFs = FileSystems.newFileSystem(zipLocal.getAbsoluteNioPath(), null);
      String csvFileName = name + ".csv";
      Path zipPath = zipFs.getPath(csvFileName);
      CsvDataPath csvInZip = CsvDataPath.createFrom(tabular.getCurrentLocalDirectoryConnection(), zipPath)
        .setDelimiterCharacter(';')
        .setHeaderRowId(1)
        .createRelationDef()
        .addColumn("sexe", Types.INTEGER)
        .addColumn("premier_prenom", Types.VARCHAR, 25)
        .addColumn("annee_naissance", Types.VARCHAR, 4)
        .addColumn("frequence", Types.INTEGER)
        .getDataPath();
      Connection connection = tabular.getConnection(ConnectionHowTos.POSTGRESQL_CONNECTION_NAME);
      DataPath target = connection.getDataPath("baby_fr");
      Tabulars.dropIfExists(target);
      Tabulars.copy(csvInZip, target);
      Path sql = Paths.get("db-gen-entities", "src", "main", "resources", "sql", FIRSTNAME, FIRSTNAME + "_fr.sql");
      SqlQuery query = SqlQuery.createFromPath(sql);
      DataPath babyPivotQuery = connection.createScriptDataPath(query.toString());

      Tabulars.dropIfExists(targetNameTable);
      Tabulars.copy(babyPivotQuery, targetNameTable);

    }

  }
}

