package net.bytle.db.gen.entities;

import com.github.javafaker.Faker;
import net.bytle.db.Tabular;
import net.bytle.db.csv.CsvDataPath;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStream;

import java.util.*;

/**
 * A base class to extract data from Java Faker
 * Every class that wants to extract entity needs to overwrite the {@link #getRow(Faker)} and {@link #getDataPath(Tabular)}
 *
 * Faker is a low level data and will not delete the file itself
 */
public  abstract class FakerBase {


  protected final String entityName;

  public FakerBase(String entityName) {
    this.entityName = entityName.toLowerCase();
  }

  public void generate() {

    //final List<String> locale = Arrays.asList(null,"bg", "ca", "ca-CAT", "da-DK", "de", "de-AT", "de-CH", "en", "en-AU", "en-au-ocker", "en-BORK", "en-CA", "en-GB", "en-IND", "en-MS", "en-NEP", "en-NG", "en-NZ", "en-PAK", "en-SG", "en-UG", "en-US", "en-ZA", "es", "es-MX", "fa", "fi-FI", "fr", "he", "hu", "in-ID", "it", "ja", "ko", "nb-NO", "nl", "pl", "pt", "pt-BR", "ru", "sk", "sv", "sv-SE", "tr", "uk", "vi", "zh-CN", "zh-TW");
    final List<String> locale = Arrays.asList("ca","de", "en", "es","fr","hu","it","nl","pl","pt","sk","tr");

    try (Tabular tabular = Tabular.tabular()) {
      for (String local : locale) {

        String fileName = entityName;
        if (local!=null) {
          local = local.toLowerCase();
           fileName += "_" + local ;
        }
        fileName += ".csv";

        CsvDataPath targetCsv = CsvDataPath.createFrom(tabular.getCurrentLocalDirectoryConnection(), EntitiesProperties.BASE_PATH.resolve(entityName).resolve(fileName))
          .createRelationDef()
          .setHeaderRowId(1)
          .getDataPath();

        if (!Tabulars.exists(targetCsv)) {
          Faker faker;
          if (local!=null) {
            faker = new Faker(new Locale(local));
          } else {
            faker = new Faker();
          }
          Map<String, List<String>> rows = new HashMap<>();
          int i = 0;
          while (i < 10000) {
            i++;
            List<String> row = getRow(faker);
            rows.put(String.join("", row), row);
          }

          DataPath dataPath = getDataPath(tabular);

          try (InsertStream insertStream = dataPath.getInsertStream()) {
            rows.keySet().stream().sorted().forEach(k -> insertStream.insert(rows.get(k)));
          }

          Tabulars.copy(dataPath, targetCsv);
        }

      }
    }

  }

  protected abstract List<String> getRow(Faker localFaker);

  protected abstract DataPath getDataPath(Tabular tabular);


}
