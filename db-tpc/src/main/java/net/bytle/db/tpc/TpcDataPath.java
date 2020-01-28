package net.bytle.db.tpc;

import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.TableSystem;
import net.bytle.db.uri.DataUri;

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
        return new TpcDataPath(dataStore,name);
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
    public List<String> getPathParts() {
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


}
