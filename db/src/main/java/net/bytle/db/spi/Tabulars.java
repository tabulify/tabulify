package net.bytle.db.spi;

import com.sun.jndi.toolkit.url.Uri;
import net.bytle.db.DatabasesStore;
import net.bytle.db.DbLoggers;
import net.bytle.db.database.Database;
import net.bytle.db.model.RelationDef;
import net.bytle.db.uri.DataUri;
import net.bytle.fs.Fs;


import java.nio.file.Path;
import java.util.List;


public class Tabulars {

    public static RelationDef get(String uri, Path dbStorePath) {

        DataUri dataUri = DataUri.of(dbStorePath,uri);
        final String databaseName = dataUri.getDataStore().getDatabaseName();
        String url;
        String scheme;
        if (databaseName.equals("file")) {
            url = Fs.getPath(dataUri.getPathSegments().toArray(new String[0])).toUri().toString();
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
                final TableSystem tableSystem = tableSystemProvider.getTableSystem(url);
                if (tableSystem==null){
                    String message = "The table system is null for the provider ("+tableSystemProvider.getClass().toString()+")";
                    DbLoggers.LOGGER_DB_ENGINE.severe(message);
                    throw new RuntimeException(message);
                }
                return tableSystem.getRelationDef(dataUri);
            }
        }
        final String message = "No provider was found for the scheme (" + scheme + ") from the Url (" + url + ")";
        DbLoggers.LOGGER_DB_ENGINE.severe(message);
        throw new RuntimeException(message);

    }

    public static DataUri of(String part, String... parts ) {
        // FileSystems.getDefault().getDataUri(first, more);
        return null;

    }

    public static DataUri of(Uri uri) {

        return null;

    }

    public static Boolean exists(DataUri dataUri) {
        //Files.exists()
        return null;

    }
}
