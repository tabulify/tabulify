package net.bytle.db.gen.generator;

import net.bytle.db.csv.CsvDataPath;
import net.bytle.db.gen.GenColumnDef;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.SelectStream;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class NameGenerator<T> extends CollectionGeneratorOnceAbs<T> implements CollectionGeneratorOnce<T> {


  private final GenColumnDef<String> columnDef;
  private final HistogramCollectionGenerator<String> nameStream;
  private String actualValue;

  public NameGenerator(GenColumnDef<String> columnDef) {

    this.columnDef = columnDef;
    URL uri = Class.class.getResource("/DataSet/FirstNamesScotland.csv");
    try {
      CsvDataPath csvDataPath = CsvDataPath.of(Paths.get(uri.toURI()))
        .getOrCreateDataDef()
        .setHeaderRowCount(1)
        .getDataPath();
      Map<String, Double> distProb = new HashMap<>();
      try(SelectStream selectStream = Tabulars.getSelectStream(csvDataPath)){
        while (selectStream.next()) {
          String name = selectStream.getString(1);
          double probability = selectStream.getDouble(2);
          distProb.put(name, probability);
        }
      }
      nameStream = new HistogramCollectionGenerator(columnDef, distProb);
    } catch (URISyntaxException e) {
      throw new RuntimeException("Bad Uri", e);
    }
  }

  @Override
  public String getNewValue() {
    actualValue = nameStream.getNewValue();
    return actualValue;
  }

  @Override
  public T getActualValue() {
    return (T) actualValue;
  }

  @Override
  public GenColumnDef getColumn() {
    return columnDef;
  }



  @Override
  public long getMaxGeneratedValues() {
    return nameStream.getMaxGeneratedValues();
  }

  @Override
  public void reset() {
    // nothing to do
  }


}
