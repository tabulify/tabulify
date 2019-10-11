package net.bytle.db.spi;

import com.sun.jndi.toolkit.url.Uri;
import net.bytle.db.DbLoggers;
import net.bytle.db.database.Database;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.uri.DataUri;

import java.util.List;


public class Tabulars {

    public static DataPath get(DataUri dataUri) {

        List<TableSystemProvider> installedProviders = TableSystemProvider.installedProviders();
        final Database dataStore = dataUri.getDataStore();
        String scheme = dataStore.getScheme();
        for (TableSystemProvider tableSystemProvider : installedProviders) {
            if (tableSystemProvider.getSchemes().contains(scheme)) {
                final TableSystem tableSystem = tableSystemProvider.getTableSystem(dataStore);
                if (tableSystem == null) {
                    String message = "The table system is null for the provider (" + tableSystemProvider.getClass().toString() + ")";
                    DbLoggers.LOGGER_DB_ENGINE.severe(message);
                    throw new RuntimeException(message);
                }
                return tableSystem.getDataPath(dataUri);
            }
        }
        final String message = "No provider was found for the scheme (" + scheme + ") from the dataStore (" + dataStore.getDatabaseName() + ") with the Url ("+dataStore.getUrl()+")";
        DbLoggers.LOGGER_DB_ENGINE.severe(message);
        throw new RuntimeException(message);

    }

    public static DataUri of(String part, String... parts) {
        // FileSystems.getSqliteDefault().getDataUri(first, more);
        return null;

    }

    public static DataUri of(Uri uri) {

        return null;

    }

    public static Boolean exists(DataPath dataPath) {

        return dataPath.getDataSystem().exists(dataPath);

    }

    public static SelectStream getSelectStream(DataPath dataPath) {
        return dataPath.getDataSystem().getSelectStream(dataPath);
    }
}
