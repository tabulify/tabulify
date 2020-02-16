package net.bytle.db.tpc;

import com.teradata.tpcds.Table;
import net.bytle.db.spi.DataPathAbs;
import net.bytle.db.spi.DataPath;
import net.bytle.db.uri.DataUri;

import java.util.Collections;
import java.util.List;

public class TpcDataPath extends DataPathAbs {


  public static final String CURRENT_WORKING_DIRECTORY = ".";

  private final TpcDataStore dataStore;
  private final String name;

  DataUri dataUri;

  public TpcDataPath(TpcDataStore dataStore, String name) {
    this.dataStore = dataStore;
    this.name = name;
  }

  public static TpcDataPath of(TpcDataStore dataStore, String name) {
    return new TpcDataPath(dataStore, name);
  }

  @Override
  public TpcDataStore getDataStore() {
    return this.dataStore;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public List<String> getNames() {
    return Collections.singletonList(name);
  }

  @Override
  public String getPath() {
    return getName();
  }

  @Override
  public DataUri getDataUri() {
    return dataUri;
  }

  @Override
  public DataPath getSibling(String name) {
    return this.dataStore.getDataModel().getAndCreateDataPath(name);
  }

  @Override
  public DataPath getChild(String name) {
    if (this.name.equals(CURRENT_WORKING_DIRECTORY)) {
      return this.dataStore.getDataModel().getAndCreateDataPath(name);
    } else {
      throw new RuntimeException("You can't get a child from the table (" + this.name + ")");
    }
  }

  @Override
  public DataPath resolve(String... names) {
    throw new RuntimeException("Not implemented, TPC is a read only source with a fix structure. Resolving a path is not needed.");
  }

  @Override
  public DataPath getChildAsTabular(String name) {
    return getChild(name);
  }

  @Override
  public String getType() {
    return "TABLE";
  }


  @Override
  public DataPath getSelectStreamDependency() {
    DataPath dependency = null;
    if (!this.getName().toLowerCase().startsWith("s_")) {
      Table table = Table.getTable(this.getName());
      if (table.isChild()) {
        dependency = getSibling(table.getParent().getName());
      }
    }
    return dependency;
  }
}
