package net.bytle.db.spi;

import net.bytle.db.DbLoggers;
import net.bytle.db.database.Database;
import net.bytle.db.uri.DataUri;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This is a class with static utility to get data path
 * If you want any other operations on a data path, go to the {@link Tabulars} class
 */
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
        final String message = "No provider was found for the scheme (" + scheme + ") from the dataStore (" + dataStore.getDatabaseName() + ") with the Url (" + dataStore.getUrl() + ")";
        DbLoggers.LOGGER_DB_ENGINE.severe(message);
        throw new RuntimeException(message);

    }

    public static DataPath of(String... parts) {

        return TableSystems.getDefault().getDataPath(parts);

    }


    /**
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


    public static DataPath of(TableSystem dataSystem, List<String> pathSegments) {
        return dataSystem.getDataPath(pathSegments.toArray(new String[0]));
    }


    public static DataPath of(TableSystem tableSystem, String... names) {
        return tableSystem.getDataPath(names);
    }
}
