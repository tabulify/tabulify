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

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Types;

/**
 * Gd = Scottish Gaelic
 * Scotland
 */
public class FirstNameGd {


  public static void main(String[] args) {
    firstNamesScotlandDataSetBuild();
  }
  /**
   * Scotland only provides full name data for 2009 and 2010. Summary data is offered over the past 20 years.
   * General information about birth record data in Scotland is available here.
   * http://www.gro-scotland.gov.uk/statistics/theme/vital-events/births/bckgr-info.html
   * <p>
   * Take scottish baby names to create a csv file
   * If you want to recreate it, you should overwrite the file check exist or delete the file
   */
  public static void firstNamesScotlandDataSetBuild() {


    try (Tabular tabular = Tabular.tabular()) {

      String entity = EntitiesProperties.FIRSTNAME;
      // https://www.localeplanet.com/icu/iso639.html#gd
      // gd for Scottish Gaelic
      String locale = "gd";

      /**
       * Database is used
       */


      // National Record of Scotland - Full Lists of Babies’ First Names 2010 to 2018
      // https://www.nrscotland.gov.uk/statistics-and-data/statistics/statistics-by-theme/vital-events/names/babies-first-names/full-lists-of-babies-first-names-2010-to-2014
      //
      URI uri = Uris.of("https://www.nrscotland.gov.uk/files//statistics/babies-names/18/babies-first-names-18-full-list.csv");
      Path path = Paths.get(uri);
      CsvDataPath httpCsv = CsvDataPath.createFrom(tabular.getCurrentLocalDirectoryConnection(), path)
        .setDescription("Babies' first forenames: births registered in Scotland in 2018");

      /**
       * Download
       */
      CsvDataPath stagingDataPath = CsvDataPath.createFrom(tabular.getCurrentLocalDirectoryConnection(), Fs.getTempDirectory().resolve(entity).resolve(entity + "csv"))
        .setHeaderRowId(7)
        .createRelationDef()
        .addColumn("boy_position", Types.VARCHAR)
        .addColumn("boy_name", Types.VARCHAR)
        .addColumn("boy_number", Types.INTEGER)
        .addColumn("blank")
        .addColumn("girl_position", Types.VARCHAR)
        .addColumn("girl_name", Types.VARCHAR)
        .addColumn("girl_number", Types.INTEGER)
        .getDataPath();
      if (!Tabulars.exists(stagingDataPath)){
        Tabulars.copy(httpCsv,stagingDataPath);
      }


      /**
       * Load in the db
       */
      Connection connection = tabular.getConnection(ConnectionHowTos.POSTGRESQL_CONNECTION_NAME);
      DataPath table = connection.getDataPath("baby_uk_scotland");
      if (!Tabulars.exists(table)) {
        Tabulars.copy(stagingDataPath, table);
      }


      /**
       * Csv Creation
       */
      Path finalNamesTarget = EntitiesProperties.BASE_PATH.resolve(entity).resolve(entity + "_" + locale + ".csv");
      CsvDataPath targetNameTable = CsvDataPath.createFrom(tabular.getCurrentLocalDirectoryConnection(), finalNamesTarget)
        .setHeaderRowId(1)
        .getOrCreateRelationDef()
        .getDataPath();
      if (!Tabulars.exists(targetNameTable)) {
        Path sql = EntitiesProperties.SQL_BASE.resolve(entity).resolve(entity + "_" + locale  + ".sql");
        String query = SqlQuery.createFromPath(sql).toString();
        DataPath babyPivotQuery = connection.createScriptDataPath(query);
        Tabulars.copy(babyPivotQuery, targetNameTable);
      }

    }


  }


  /**
   * Records for the United Kingdom are broken out across England and Wales, Northern Ireland and Scotland.
   * The Office of National Statistics records births for England and Wales while Northern Ireland and Scotland are recorded seperately.
   * In all jurisdictions the minimum number of births per year for each name is 3.
   */
  public void firstNamesUk() {
    // https://catalogue.data.gov.bc.ca/dataset/most-popular-girl-names-for-the-past-100-years
    // https://catalogue.data.gov.bc.ca/dataset/most-popular-boys-names-for-the-past-100-years
  }


  /**
   * http://www.ons.gov.uk/ons/guide-method/user-guidance/health-and-life-events/births-metadata.pdf
   */
  public void firstNamesUkEnglandWales() {

    // Full name data is provided between 1996 and 2011.
    // The ONS offers historical summary data for 1904-1994 but these are restricted to the most popular names
    // per year and so not of much analytical value.
    // Information on the data itself can be found here: http://www.ons.gov.uk/ons/guide-method/user-guidance/health-and-life-events/births-metadata.pdf

  }

  /**
   * Northern Ireland provides full name data between 1997 and 2011. Like the ONS, summary data is offered but does not add much value.
   * Information on NISRA data can be found here.
   * http://www.nisra.gov.uk/demography/default.asp28.htm
   */
  public void firstNamesUkNorthernIreland() {
  }


}
