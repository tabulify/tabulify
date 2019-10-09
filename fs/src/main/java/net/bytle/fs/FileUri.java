package net.bytle.fs;

import net.bytle.db.uri.DataUri;
import net.bytle.db.uri.IDataUri;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileUri extends DataUri implements IDataUri {


    /**
     * The local file database name as also stated
     * by the {@link Path#getFileSystem()}
     */
    public static final String LOCAL_FILE_SYSTEM = "file";


    private FileUri(String uri, String... parts) {
        super(uri, parts);
    }

    /**
     *
     * @param part - if the first part does not start with the @sign it's considered to be a local file system
     * @param parts - the path in the connection
     * @return a fileUri from a data Uri string or from data Uri parts
     */
    public static FileUri of(String part, String... parts) {
        if (part == null){
            throw new RuntimeException("The first part cannot be null");
        }
        if (!part.substring(0, 1).equals(DataUri.AT_STRING)) {

            List<String> names = new ArrayList<>();
            names.addAll(Arrays.asList(part.split(PATH_SEPARATOR)));
            names.addAll(Arrays.asList(parts));
            parts = names.toArray(new String[0]);

            part = "@" + FileUri.LOCAL_FILE_SYSTEM;

        }
        return new FileUri(part,parts);

    }

    public String getFileName() {

        return super.getPathSegments()[super.getPathSegments().length-1];

    }

}
