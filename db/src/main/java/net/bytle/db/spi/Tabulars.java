package net.bytle.db.spi;

import net.bytle.db.DatabasesStore;
import net.bytle.db.DbLoggers;
import net.bytle.db.database.Database;
import net.bytle.db.model.RelationDef;
import net.bytle.db.uri.DataUri;
import net.bytle.fs.Fs;

import java.nio.file.Path;
import java.util.List;


public class Tabulars {

    public static RelationDef get(DataUri dataUri, Path dbStorePath) {

        final String databaseName = dataUri.getDatabaseName();
        String url;
        String scheme;
        if (databaseName.equals("file")) {
            url = Fs.getPath(dataUri.getPathSegments()).toUri().toString();
            scheme = "file";
        } else {
            DatabasesStore databaseStore = DatabasesStore.of(dbStorePath);

            final Database database = databaseStore.getDatabase(databaseName);
            if (database == null) {

                final String message = "No database named (" + databaseName + ") could be found in the databaseStore (" + dbStorePath.toString() + " for the data uri (" + dataUri.toString() + ")";
                DbLoggers.LOGGER_DB_ENGINE.severe(message);
                throw new RuntimeException(message);

            } else {

                DbLoggers.LOGGER_DB_ENGINE.info("The database named (" + databaseName + ") was found");
                url = database.getUrl();
                scheme = url.substring(0, url.indexOf(":"));

            }
        }

        List<TableSystemProvider> installedProviders = TableSystemProvider.installedProviders();

        for (TableSystemProvider tableSystemProvider : installedProviders) {
            if (tableSystemProvider.getSchemes().contains(scheme)) {
                return tableSystemProvider.getTableSystem(url).getRelationDef(dataUri);
            }
        }
        final String message = "No provider was found for the scheme (" + scheme + ") from the Url (" + url + ")";
        DbLoggers.LOGGER_DB_ENGINE.severe(message);
        throw new RuntimeException(message);

    }

    public static RelationDef get(DataUri dataUri) {

        return get(dataUri, DatabasesStore.DEFAULT_STORAGE_FILE);

    }

    public static Boolean exists(RelationDef relationDef) {
        return null;
    }
}
