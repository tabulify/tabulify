package net.bytle.db.tpc;

import net.bytle.db.database.Database;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataSetSystem;
import net.bytle.db.spi.TableSystemProvider;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.uri.DataUri;
import net.bytle.regexp.Globs;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TpcDataSetSystem extends DataSetSystem {

    private static final String PRODUCT_NAME = "tpcds";
    private static TpcDataSetSystem tpcDataSetSystem;
    private TpcdsModel tpcModel;
    private final Database database;

    private TpcDataSetSystem() {
        this.database = Database.of(PRODUCT_NAME);
    }

    public static TpcDataSetSystem of() {
        if (tpcDataSetSystem==null){
            tpcDataSetSystem = new TpcDataSetSystem();
        }
        return tpcDataSetSystem ;
    }

    public TpcdsModel getDataModel() {
        if (tpcModel==null) {
            this.tpcModel = TpcdsModel.of(this);
        }
        return tpcModel;
    }

    @Override
    public DataPath getDataPath(DataUri dataUri) {

        return getDataPath(dataUri.getPath());

    }

    @Override
    public DataPath getDataPath(String... names) {
        DataPath dataPath = this.getDataModel().getDataPath(names[0]);
        // Case when it's the working directory
        if (dataPath==null) {
            dataPath = TpcDataPath.of(this, names[0]);
        }
        return dataPath;
    }

    @Override
    public Boolean exists(DataPath dataPath) {
        assert dataPath!=null:"A data path should not be null";
        return tpcModel.getDataPath(dataPath.getName())!=null;
    }

    @Override
    public SelectStream getSelectStream(DataPath dataPath) {
        return TpcdsSelectStream.of(dataPath);
    }

    @Override
    public Database getDatabase() {
        return this.database;
    }



    @Override
    public boolean isContainer(DataPath dataPath) {
        return false;
    }

    @Override
    public String getProductName() {
        return PRODUCT_NAME;
    }


    @Override
    public TableSystemProvider getProvider() {

        throw new RuntimeException("Not yet implemented");
    }

    @Override
    public List<DataPath> getChildrenDataPath(DataPath dataPath) {
        if (dataPath.getPath().equals(TpcDataPath.CURRENT_WORKING_DIRECTORY)) {
            return this.tpcModel.getDataPaths();
        } else {
            return new ArrayList<>();
        }
    }



    @Override
    public Boolean isEmpty(DataPath dataPath) {
        return false;
    }

    @Override
    public Integer size(DataPath dataPath) {
        throw new RuntimeException("Not yet implemented");
    }

    @Override
    public boolean isDocument(DataPath dataPath) {
        if (dataPath.getPath().equals(TpcDataPath.CURRENT_WORKING_DIRECTORY)) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public List<DataPath> getDescendants(DataPath dataPath) {
        return getChildrenDataPath(dataPath);
    }

    @Override
    public List<DataPath> getDescendants(DataPath dataPath, String glob) {
        String pattern = Globs.toRegexPattern(glob);
        return getChildrenDataPath(dataPath).stream()
                .filter(d->d.getPath().matches(pattern))
                .collect(Collectors.toList());
    }

    @Override
    public List<DataPath> getReferences(DataPath dataPath) {
        throw new RuntimeException("Not yet implemented");
    }

    @Override
    public void close() {

    }
}
