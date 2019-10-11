package net.bytle.db.fs;

import net.bytle.db.DatabasesStore;
import net.bytle.db.database.Database;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.TableSystem;
import net.bytle.db.uri.DataUri;
import net.bytle.fs.Fs;
import net.bytle.regexp.Globs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FsTableSystem extends TableSystem {


    private final Database database;

    private FsTableSystem(Database database) {
        assert database!=null;
        this.database = database;
    }

    protected static TableSystem of(Database database) {
        return new FsTableSystem(database);
    }



    /**
     *
     * @param dataUri
     * @return a list of file that matches the uri segments
     *
     * ex: the following file uri
     *   /tmp/*.md
     * will return all md file in the tmp directory
     *
     */
    public List<DataPath> getDataPaths(DataUri dataUri) {

        if (!dataUri.getDataStore().getScheme().equals(DatabasesStore.LOCAL_FILE_SYSTEM)){
            throw new RuntimeException("Only a file uri from the local file system is implemented from now");
        }

        final List<String> pathSegments = dataUri.getPathSegments();

        // Start
        Path startPath = Paths.get(".");
        List<Path> currentMatchesPaths = new ArrayList<>();
        currentMatchesPaths.add(startPath);

        for (String s: pathSegments){

            // Glob to regex Pattern
            String pattern = Globs.toRegexPattern(s);

            // The list where the actual matches path will be stored
            List<Path> matchesPath = new ArrayList<>();
            for (Path currentPath: currentMatchesPaths) {
                List<Path> paths = Fs.getChildrenFiles(currentPath);
                for (Path childrenPath : paths) {
                    if (childrenPath.getFileName().toString().matches(pattern)) {
                        matchesPath.add(childrenPath);
                    }
                }
            }

            if (matchesPath.size()==0){
                break;
            } else {
                // Recursion
                currentMatchesPaths = matchesPath;
            }

        }


        return currentMatchesPaths.stream()
                .map(s->FsDataPath.of(this,s))
                .collect(Collectors.toList());
    }

    @Override
    public DataPath getDataPath(DataUri dataUri) {
        final Path path = Fs.getPath(dataUri.getPathSegments().toArray(new String[0]));
        return FsDataPath.of(this, path);
    }

    @Override
    public Boolean exists(DataPath dataPath) {

        final FsDataPath fsDataPath = (FsDataPath) dataPath;
        return Files.exists(fsDataPath.getPath());

    }
}
