package net.bytle.niofs.sftp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created by gerard on 22-11-2015.
 * A channel to (write|overwrite) a new File
 * <p/>
 * SFTP knows only three transfer mode RESUME, APPEND, OVERWRITE
 */
public class SftpByteChannel implements SeekableByteChannel {

    private final SftpFileProgressMonitor monitor;
    WritableByteChannel writableByteChannel;
    ReadableByteChannel readableByteChannel;

    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());

    protected SftpByteChannel(SftpPath path, Set<? extends OpenOption> options) throws IOException {

        monitor = new SftpFileProgressMonitor();

        // Write Channel
        if (options.containsAll(EnumSet.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE))) {

            // Create a new file (overwrite: create a new file or overwrite it)
            try {
                OutputStream put = path.getChannelSftp().put(path.toAbsolutePath().toString(), monitor, ChannelSftp.OVERWRITE);
                writableByteChannel = Channels.newChannel(put);
            } catch (SftpException e) {
                // The error message seems platform dependent ...
                if (
                        (e.id == ChannelSftp.SSH_FX_FAILURE && e.getMessage().equals("The system cannot find the path specified")) ||
                                (e.id == ChannelSftp.SSH_FX_FAILURE && e.getMessage().equals("No such file or directory")) ||
                                (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE)) {
                    // This exception can be thrown with the use of the function java.file.Files.createFile
                    throw new NoSuchFileException("The parent directory or the file doesn't exist (" + path.toAbsolutePath().toString() + ")");

                } else {

                    // The logger is to debug Travis-ci
                    LOGGER.warning("Trying to create a new file, we get the following Error Id (" + e.id + "), Error Message: (" + e.getLocalizedMessage() + ")");
                    // The error message seems platform dependent therefore we send a NoSuchFileException
                    // Required by the API.
                    throw new NoSuchFileException(e.id + e.getLocalizedMessage());

                }
            }

        } else if (options.containsAll(EnumSet.of(StandardOpenOption.READ))) {
            // read channel
            try {
                InputStream inputStream = path.getChannelSftp().get(path.toAbsolutePath().toString(), monitor);
                readableByteChannel = Channels.newChannel(inputStream);
            } catch (SftpException e) {
                throw new RuntimeException(e);
            }

        } else {
            throw new UnsupportedOperationException("Operations are not supported (" + options + ")");
        }


    }

    public int read(ByteBuffer dst) throws IOException {
        return readableByteChannel.read(dst);
    }

    public int write(ByteBuffer src) throws IOException {
        return writableByteChannel.write(src);
    }

    public long position() throws IOException {
        // Need to get a monitor from the channelsftp.put method
        // To implement it
        return monitor.getCount();
    }

    public SeekableByteChannel position(long newPosition) throws IOException {
        throw new UnsupportedOperationException("With SFTP you cannot change the position");
    }

    public long size() throws IOException {
        throw new UnsupportedOperationException();
    }

    public SeekableByteChannel truncate(long size) throws IOException {
        throw new UnsupportedOperationException();
    }

    public boolean isOpen() {
        boolean isOpen = false;
        if (writableByteChannel != null) {
            isOpen = writableByteChannel.isOpen();
        }
        if (readableByteChannel != null) {
            isOpen = readableByteChannel.isOpen();
        }
        return isOpen;
    }

    public void close() throws IOException {
        if (writableByteChannel != null) {
            writableByteChannel.close();
        }
        if (readableByteChannel != null) {
            readableByteChannel.close();
        }
    }
}
