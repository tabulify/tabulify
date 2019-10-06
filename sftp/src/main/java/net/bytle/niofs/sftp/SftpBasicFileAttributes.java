package net.bytle.niofs.sftp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.logging.Logger;

/**
 * Created by gerard on 21-11-2015.
 */
public class SftpBasicFileAttributes implements BasicFileAttributes {

    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());

    public SftpATTRS attrs;

    // The time that gets back miss one/two hours and I can't find a time zone cause
    // In Seconds, 2 hour
    static final int timeOffset = 60*60;


    protected SftpBasicFileAttributes(SftpPath path) throws IOException {
        try {
            this.attrs = path.getChannelSftp().stat(path.toAbsolutePath().toString());
        } catch (SftpException e) {
            throwExc(path, e);
        }
    }

    private void throwExc(SftpPath path, SftpException e) throws IOException {
        if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE){
            throw new NoSuchFileException("The file doesn't exist");
        } else {
            throw new IOException("Unable to get the file attributes of (" + path.toAbsolutePath().toString() + ")", e);
        }
    }

    public FileTime lastModifiedTime() {
        // 2 hour
        return FileTime.fromMillis( (long) (this.attrs.getMTime() + 2*timeOffset)*1000);
    }

    public FileTime lastAccessTime() {
        return FileTime.fromMillis( (long) (this.attrs.getATime() + 2*timeOffset)*1000);
    }

    /**
     * Creation Time is not in the specification
     * http://tools.ietf.org/html/draft-ietf-secsh-filexfer-02#section-5
     * @return {@link SftpBasicFileAttributes#lastModifiedTime() the last modified time }
     */
    public FileTime creationTime() {
        LOGGER.warning("The creation time of a file doesn't exist in the SSH protocol, returning the last modified time");
        return lastModifiedTime();
    }

    public boolean isRegularFile() {
        return !this.attrs.isDir();
    }

    public boolean isDirectory() {
        return this.attrs.isDir();
    }

    public boolean isSymbolicLink() {
        return this.attrs.isLink();
    }

    /**
     *
     * @return false
     */
    public boolean isOther() {
        return false;
    }

    public long size() {
        return this.attrs.getSize();
    }

    /**
     *
     * @return null
     */
    public Object fileKey() {
        return null;
    }
}
