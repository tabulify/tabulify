package net.bytle.db.spi;

import net.bytle.db.stream.SelectStream;
import net.bytle.db.uri.DataUri;

public abstract class TableSystem {

    public abstract DataPath getDataPath(DataUri dataUri);

    public abstract Boolean exists(DataPath dataPath);

    public abstract SelectStream getSelectStream(DataPath dataPath);
}
