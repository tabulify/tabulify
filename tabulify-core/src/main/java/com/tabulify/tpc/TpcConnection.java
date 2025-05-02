package com.tabulify.tpc;

import com.tabulify.Tabular;
import com.tabulify.conf.Origin;
import com.tabulify.connection.Connection;
import com.tabulify.fs.FsConnectionResourcePath;
import com.tabulify.noop.NoOpConnection;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataSystem;
import com.tabulify.spi.ProcessingEngine;
import com.tabulify.spi.ResourcePath;
import net.bytle.exception.CastException;
import net.bytle.type.Casts;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.MediaType;

import static com.tabulify.tpc.TpcDataPath.CURRENT_WORKING_DIRECTORY_NAME;
import static com.tabulify.tpc.TpcDataPath.SEPARATOR;

public class TpcConnection extends NoOpConnection {


  private final TpcDataSetSystem tpcDataSystem;

  public TpcConnection(Tabular tabular, com.tabulify.conf.Attribute name, com.tabulify.conf.Attribute url) {
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

  @SuppressWarnings("RedundantMethodOverride")
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

  @Override
  public Connection addAttribute(KeyNormalizer name, Object value, Origin origin) {
    try {
      TpcConnectionAttributeEnum connectionAttribute = Casts.cast(name, TpcConnectionAttributeEnum.class);
      return addAttribute(
        this
          .getTabular()
          .getVault()
          .createAttribute(connectionAttribute, value, origin)
      );
    } catch (CastException e) {
      return super.addAttribute(name, value, origin);
    }
  }
}
