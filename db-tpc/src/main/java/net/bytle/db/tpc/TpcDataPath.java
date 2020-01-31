package net.bytle.db.tpc;

import com.teradata.tpcds.Table;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.TableSystem;
import net.bytle.db.uri.DataUri;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TpcDataPath extends DataPath {


  public static final String CURRENT_WORKING_DIRECTORY = ".";

  private final TpcDataSetSystem dataStore;
  private final String name;

  DataUri dataUri;

  public TpcDataPath(TpcDataSetSystem dataStore, String name) {
    this.dataStore = dataStore;
    this.name = name;
  }

  public static TpcDataPath of(TpcDataSetSystem dataStore, String name) {
    return new TpcDataPath(dataStore, name);
  }

  @Override
  public TableSystem getDataSystem() {
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
    if (this.name == null) {
      return this.dataStore.getDataModel().getAndCreateDataPath(name);
    } else {
      throw new RuntimeException("You can get a child from the table (" + name + ")");
    }
  }

  @Override
  public DataPath resolve(String... names) {
    throw new RuntimeException("Not implemented, TPC is a read only source with a fix structure. Resolving a path is not needed.");
  }


  @Override
  public List<DataPath> getSelectStreamDependencies() {
    List<DataPath> dependencies = new ArrayList<>();
    if (!this.getName().toLowerCase().startsWith("s_")) {
      Table table = Table.getTable(this.getName());
      if (table.isChild()) {
        dependencies = Arrays.asList(getSibling(table.getParent().getName()));
      }
    }
    return dependencies;
  }
}
