package net.bytle.db.gen.entities;

import java.nio.file.Path;
import java.nio.file.Paths;

public class EntitiesProperties {

  /**
   * To simplify the fact that an entity may be plural (table) or singular (column)
   * We just use singular everywhere
   *
   * Name of tables and or columns
   */
  public static final String FIRSTNAME = "firstname";
  public static final String PREFIX = "prefix";
  public static final String LASTNAME = "lastname";
  public static final String GENDER = "gender";
  public static final String PROBABILITY = "probability";
  public static final String ENTITY = "entity";
  public static final String COUNTRY = "country";
  public static final String REGION = "region";
  public static final String SUBREGION = "subregion";


  public static final Path SQL_BASE = Paths.get("db-gen-entities", "src", "main", "resources", "sql");
  public static Path BASE_PATH = Paths.get("db-gen-entities", "src", "main", "resources", ENTITY);

}
