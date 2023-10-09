package net.bytle.db.tpc;

import net.bytle.db.Tabular;
import net.bytle.db.fs.FsConnectionResourcePath;
import net.bytle.db.noop.NoOpConnection;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataSystem;
import net.bytle.db.spi.ProcessingEngine;
import net.bytle.db.spi.ResourcePath;
import net.bytle.type.MediaType;
import net.bytle.type.Variable;

import static net.bytle.db.tpc.TpcDataPath.CURRENT_WORKING_DIRECTORY_NAME;
import static net.bytle.db.tpc.TpcDataPath.SEPARATOR;

public class TpcConnection extends NoOpConnection {


  private final TpcDataSetSystem tpcDataSystem;

  public TpcConnection(Tabular tabular, Variable name, Variable url) {
    super(tabular, name, url);
    tpcDataSystem = new TpcDataSetSystem(this);
  }

  @Override
  public DataSystem getDataSystem() {
    return tpcDataSystem;
  }


  @Override
  public DataPath getDataPath(String pathOrName, MediaType mediaType) {

    if (pathOrName.equals(CURRENT_WORKING_DIRECTORY_NAME)) {
      return getCurrentDataPath();
    } else {
      return getDataModel().getAndCreateDataPath(pathOrName);
    }
  }

  @Override
  public String getCurrentPathCharacters() {
    return CURRENT_WORKING_DIRECTORY_NAME;
  }

  @Override
  public String getParentPathCharacters() {
    // No parent, return empty string
    return "";
  }

  @Override
  public String getSeparator() {
    return SEPARATOR;
  }


  @Override
  public DataPath getCurrentDataPath() {
    return new TpcDataPath(this, CURRENT_WORKING_DIRECTORY_NAME);
  }


  @Override
  public DataPath createScriptDataPath(DataPath dataPath) {
    throw new UnsupportedOperationException("The tpc data source does not support scripting");
  }


  @Override
  public ProcessingEngine getProcessingEngine() {
    throw new UnsupportedOperationException("The tpc data source does not have a processing engine");
  }

  @Override
  public Boolean ping() {
    return true;
  }


  private TpcdsModel tpcModel;

  public TpcdsModel getDataModel() {
    if (tpcModel == null) {
      this.tpcModel = TpcdsModel.of(this);
    }
    return tpcModel;
  }

  @Override
  public ResourcePath createStringPath(String pathOrName, String... names) {
    /**
     * Because there is no schema, the fs string path works
     */
    return FsConnectionResourcePath.createOf(pathOrName, names);
  }
}
