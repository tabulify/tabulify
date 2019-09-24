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
    public static final String DATABASE_FILE_NAME = "file";


    public FileUri(String uri, String... parts) {
        super(uri, parts);
    }

    /**
     *
     * @param part - a local path or a connection
     * @param parts - the path in the connection
     * @return a fileUri from a data Uri string or from data Uri parts
     */
    public static FileUri of(String part, String... parts) {
        if (part == null){
            throw new RuntimeException("The first part cannot be null");
        }
        if (parts.length==0){
            // This is an URI
            String uri = part;
            if (!part.substring(0, 1).equals(DataUri.AT_STRING)) {
                uri = "@" + FileUri.DATABASE_FILE_NAME + DataUri.PATH_SEPARATOR + part;
            }
            return new FileUri(uri);

        } else {

            List<String> partsToPass = Arrays.asList(parts);
            String databaseName = part;
            // Not sure what to do here
            if (part.equals(".") || part.equals("..")){
                databaseName = FileUri.DATABASE_FILE_NAME;
                partsToPass = new ArrayList<>();
                partsToPass.add(part);
                partsToPass.addAll(Arrays.asList(parts));
            }
            return new FileUri(databaseName, partsToPass.toArray(new String[0]));
        }
    }




    public String getFileName() {
        return super.getPathSegments()[super.getPathSegments().length-1];
    }
}
