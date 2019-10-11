package net.bytle.db.fs;

import net.bytle.db.csv.CsvTable;
import net.bytle.db.model.RelationDef;
import net.bytle.db.spi.TableSystem;
import net.bytle.db.uri.DataUri;
import net.bytle.fs.Fs;
import net.bytle.regexp.Globs;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FsTableSystem extends TableSystem {


    private final String uri;
    private final Map<String, ?> env;

    private FsTableSystem(String uri, Map<String, ?> env) {
        assert uri!=null;
        if (env==null){
            env = new HashMap<>();
        }
        this.uri = uri;
        this.env = env;
    }

    protected static TableSystem of(String uri) {
        Map<String,?> env = new HashMap<>();
        return new FsTableSystem(uri, env);
    }

    public static TableSystem of(String uri, Map<String, ?> env) {
        return new FsTableSystem(uri,env);
    }

    @Override
    public RelationDef getRelationDef(DataUri dataUri) {
        Path path = Fs.getPath(dataUri.getPathSegments());
        return CsvTable.of(path);
    }

    /**
     *
     * @param fileUri
     * @return a list of file that matches the uri segments
     *
     * ex: the following file uri
     *   /tmp/*.md
     * will return all md file in the tmp directory
     *
     */
    public static List<Path> get(FileUri fileUri) {

        if (!fileUri.getDataStore().equals(FileUri.LOCAL_FILE_SYSTEM)){
            throw new RuntimeException("Only a file uri from the local file system is implemented from now");
        }

        final String[] pathSegments = fileUri.getPathSegments();

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
                return matchesPath;
            } else {
                // Recursion
                currentMatchesPaths = matchesPath;
            }

        }

        return currentMatchesPaths;
    }

}
