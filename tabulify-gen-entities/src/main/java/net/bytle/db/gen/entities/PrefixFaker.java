package net.bytle.db.gen.entities;

import com.github.javafaker.Faker;
import com.github.javafaker.Name;
import net.bytle.db.Tabular;
import net.bytle.db.spi.DataPath;

import java.util.ArrayList;
import java.util.List;

/**
 * Extract the Java Faker information
 * for each local
 */
public class PrefixFaker extends FakerBase {

  public static void main(String[] args) {

    new PrefixFaker(EntitiesProperties.PREFIX).generate();

  }

  public PrefixFaker(String entity) {
    super(entity);
  }

  @Override
  protected List<String> getRow(Faker localFaker) {
    Name name = localFaker.name();
    List<String> row = new ArrayList<>();
    row.add(name.prefix());
    row.add("");
    row.add("");
    return row;
  }

  @Override
  protected DataPath getDataPath(Tabular tabular) {

    return tabular.getAndCreateRandomMemoryDataPath()
      .getOrCreateRelationDef()
      .addColumn(this.entityName)
      .addColumn(EntitiesProperties.GENDER)
      .addColumn(EntitiesProperties.PROBABILITY)
      .getDataPath();
  }




}
