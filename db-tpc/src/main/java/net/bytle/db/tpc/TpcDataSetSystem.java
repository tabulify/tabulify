package net.bytle.db.tpc;

import net.bytle.db.database.Database;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.DataType;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataSetSystem;
import net.bytle.db.spi.ProcessingEngine;
import net.bytle.db.spi.TableSystemProvider;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.uri.DataUri;

import java.util.List;

public class TpcDataSetSystem extends DataSetSystem {

    public static TpcDataSetSystem of() {
        return new TpcDataSetSystem();
    }

    @Override
    public DataPath getDataPath(DataUri dataUri) {
        return null;
    }

    @Override
    public DataPath getDataPath(String... name) {
        return null;
    }

    @Override
    public Boolean exists(DataPath dataPath) {
        return null;
    }

    @Override
    public SelectStream getSelectStream(DataPath dataPath) {
        return null;
    }

    @Override
    public Database getDatabase() {
        return null;
    }

    @Override
    public <T> T getMax(ColumnDef<T> columnDef) {
        return null;
    }

    @Override
    public boolean isContainer(DataPath dataPath) {
        return false;
    }

    @Override
    public String getProductName() {
        return null;
    }

    @Override
    public DataType getDataType(Integer typeCode) {
        return null;
    }

    @Override
    public TableSystemProvider getProvider() {
        return null;
    }

    @Override
    public List<DataPath> getChildrenDataPath(DataPath dataPath) {
        return null;
    }

    @Override
    public Integer getMaxWriterConnection() {
        return null;
    }

    @Override
    public Boolean isEmpty(DataPath queue) {
        return null;
    }

    @Override
    public Integer size(DataPath dataPath) {
        return null;
    }

    @Override
    public boolean isDocument(DataPath dataPath) {
        return false;
    }

    @Override
    public String getString(DataPath dataPath) {
        return null;
    }

    @Override
    public List<DataPath> getDescendants(DataPath dataPath) {
        return null;
    }

    @Override
    public List<DataPath> getDescendants(DataPath dataPath, String glob) {
        return null;
    }

    @Override
    public List<DataPath> getReferences(DataPath dataPath) {
        return null;
    }



    @Override
    public void close() throws Exception {

    }
}
