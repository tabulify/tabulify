package net.bytle.niofs.sftp;

import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.logging.Logger;

/**
 * Created by gerard on 02-05-2016.
 *
 * An attribute view that is a read-only or updatable view of non-opaque
 * values associated with a file in a filesystem. This interface is extended or
 * implemented by specific file attribute views that define methods to read
 * and/or update the attributes of a file.
 *
 */
public class SftpFileBasicAttributeView implements BasicFileAttributeView {

    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());

    private final SftpPath path;

    public SftpFileBasicAttributeView(SftpPath path) {
        this.path = path;
    }

    public String name() {
        return "Basic File Attribute View Name";
    }

    public BasicFileAttributes readAttributes() throws IOException {
         throw new UnsupportedOperationException("The readAttributes function is not supported.");
    }

    /*
     * Create Time can not be set with Sftp
     */
    public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {

        try {

            int lastModifiedTimeInInt;
            int lastAccessTimeInInt;

            SftpATTRS attrs = path.getChannelSftp().stat(path.toAbsolutePath().toString());

            if (lastModifiedTime != null) {
                lastModifiedTimeInInt = (int) (lastModifiedTime.toMillis() / 1000 - 2*SftpBasicFileAttributes.timeOffset);
            } else {
                lastModifiedTimeInInt = attrs.getMTime();
            }

            if (lastAccessTime != null) {
                lastAccessTimeInInt = (int) (lastAccessTime.toMillis() / 1000 - 2*SftpBasicFileAttributes.timeOffset);
            } else {
                lastAccessTimeInInt = attrs.getATime();
            }
            attrs.setACMODTIME(
                    lastAccessTimeInInt,
                    lastModifiedTimeInInt
            );

            if (createTime != null) {
                LOGGER.warning("The creation time of a file doesn't exist in the SSH protocol, the creation time can then not be changed.");
                throw new UnsupportedOperationException("The creation time of a file doesn't exist in the SSH protocol, the creation time can" +
                        " then not be changed.");
            }

            path.getChannelSftp().setStat(path.toAbsolutePath().toString(),attrs);

        } catch (SftpException e) {
            throw new RuntimeException(e);
        }


    }

    /*
     * Type is the type of attribute (For now only basics)
     */
    public static <V extends FileAttributeView> V get(Path path) {

        return (V) new SftpFileBasicAttributeView((SftpPath) path);

    }

    public void setAttribute(String attr, Object value) throws IOException {

        if (attr.equals("lastAccessTime")) {
            FileTime  lastAccessTime = (FileTime) value;
            setTimes(null,lastAccessTime,null);
        } else {
            throw new UnsupportedOperationException("The file attribute ("+attr+") is unknown.");
        }

    }
}
