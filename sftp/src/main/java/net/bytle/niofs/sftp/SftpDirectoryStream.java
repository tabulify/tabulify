package net.bytle.niofs.sftp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;

import java.io.IOException;
import java.nio.file.ClosedDirectoryStreamException;
import java.nio.file.DirectoryStream;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * Created by gerard on 18-05-2016.
 */
public class SftpDirectoryStream implements DirectoryStream<Path> {

    private final SftpPath path;
    private final Filter<? super Path> filter;
    private final SftpBasicFileAttributes fileAttribute;
    private boolean isClosed = false;
    private Iterator<Path> pathIterator;


    public SftpDirectoryStream(SftpPath path, Filter<? super Path> filter) throws IOException {
        this.path = path;
        this.filter = filter;

        // Sanity check: is it a directory
        this.fileAttribute = new SftpBasicFileAttributes(path);
        if (!fileAttribute.isDirectory()) throw new NotDirectoryException(this.path.toString());
    }

    @Override
    public Iterator<Path> iterator() {

        if (isClosed)
            throw new ClosedDirectoryStreamException();

        if (pathIterator != null)
            throw new IllegalStateException("Iterator has already been returned");

        List<Path> pathList = new ArrayList<>();
        try {
            Vector<ChannelSftp.LsEntry> childFiles = this.path.getChannelSftp().ls(this.path.toAbsolutePath().toString());

            for (ChannelSftp.LsEntry file: childFiles) {
                if ( !(file.getFilename().equals(".") || file.getFilename().equals(".."))) {
                    String childPathString = this.path.toAbsolutePath().toString() + this.path.getFileSystem().getSeparator() + file.getFilename();
                    Path childPath = SftpPath.get(this.path.getFileSystem(), childPathString);
                    pathList.add(childPath);
                }

            }
        } catch (SftpException e) {
            throw new RuntimeException(e);
        }
        return pathList.iterator();

    }

    @Override
    public void close() throws IOException {

        isClosed = true;

    }
}
