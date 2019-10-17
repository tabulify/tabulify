package net.bytle.db.spi;

import com.sun.jndi.toolkit.url.Uri;
import net.bytle.db.DbLoggers;
import net.bytle.db.database.Database;
import net.bytle.db.memory.ListSelectStream;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.stream.Streams;
import net.bytle.db.uri.DataUri;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DataPaths {


    public static DataPath of(DataUri dataUri) {

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

    public static DataPath of(String... parts) {
        // FileSystems.getSqliteDefault().getDataUri(first, more);
        return TableSystems.getDefault().getDataPath(parts);

    }


    /**
     *
     * @param dataPath
     * @param names
     * @return
     */
    public static DataPath of(DataPath dataPath, String... names) {
        List<String> pathSegments = new ArrayList<>();
        pathSegments.addAll(dataPath.getPathSegments());
        pathSegments.addAll(Arrays.asList(names));
        return dataPath.getDataSystem().getDataPath(pathSegments.toArray(new String[0]));
    }

    public static DataPath of(Database database, String... names) {
        return null;
    }

    public static DataPath of(TableSystem dataSystem, List<String> pathSegments) {
        return dataSystem.getDataPath(pathSegments.toArray(new String[0]));
    }
}
