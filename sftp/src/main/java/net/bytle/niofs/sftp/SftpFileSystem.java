package net.bytle.niofs.sftp;

import com.jcraft.jsch.*;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by gerard on 20-11-2015.
 * A file system that you obtain with the factor {@link SftpFileSystemProvider}
 * <p/>
 * The default file system, obtained by invoking the FileSystems.getDefault method, provides access to the file system that is accessible to the Java virtual machine.
 */
public class SftpFileSystem extends FileSystem {

    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());

    public static final int DEFAULT_PORT = 22;
    private static final String DEFAULT_LOCALHOST = "localhost";

    // Parameters
    private ChannelSftp channelSftp;

    private final URI uri;


    private final SftpFileSystemBuilder sftpFileSystemBuilder;

    private Session session;

    /**
     * Return the working directory
     *
     * @return the working directory
     */
    public String getWorkingDirectory() {
        String workingDirectory;
        if (this.uri.getPath() == null || this.uri.getPath().equals("")) {
            try {
                workingDirectory = this.getChannelSftp().pwd();
            } catch (SftpException e) {
                throw new RuntimeException(e);
            }
        } else {
            workingDirectory = this.uri.getPath();
        }
        return workingDirectory;
    }


    /**
     * The channel Sftp getter
     *
     * @return ChannelSftp
     */
    protected ChannelSftp getChannelSftp() {

        if (channelSftp == null) {

            // Extract the user and the password
            String user = SftpURIUtil.getUserFromUserInfo(uri);
            String password = SftpURIUtil.getPasswordFromUserInfo(uri);

            // No need to get the path of the URI here

            // Host
            String host;
            if (uri.getHost() != null) {
                host = uri.getHost();
            } else {
                host = DEFAULT_LOCALHOST;
            }

            // Port
            int port;
            if (this.uri.getPort() == -1) {
                port = DEFAULT_PORT;
            } else {
                port = this.uri.getPort();
            }

            try {

                LOGGER.info("Trying to connect to the sftp connection (Short Uri: " + SftpURIUtil.toStringWithoutPassword(uri)+")");
                JSch jsch = new JSch();

                // SSH Session
                this.session = jsch.getSession(user, host, port);
                if (password != null) {
                    session.setPassword(password);
                }
                Properties config = new Properties();
                config.put("StrictHostKeyChecking", "no");
                session.setConfig(config);
                session.connect();

                // Channel used (sftp, exec ....
                this.channelSftp = (ChannelSftp) session.openChannel("sftp");

                this.channelSftp.connect();

            } catch (JSchException e) {
                LOGGER.severe("Unable to connect");
                throw new RuntimeException(e);
            }

            // Setting the Working Directory
            if (uri.getPath() != null && !uri.getPath().equals("")) {
                try {
                    if (this.channelSftp.stat(uri.getPath()).isDir()) {
                        this.channelSftp.cd(uri.getPath());
                    }
                } catch (SftpException e) {
                    throw new RuntimeException(e);
                }
            }


        }

        return channelSftp;
    }


    /**
     * A file system is open upon creation
     *
     * @param sftpFileSystemBuilder
     */
    private SftpFileSystem(SftpFileSystemBuilder sftpFileSystemBuilder) {

        // Uri
        this.uri = sftpFileSystemBuilder.uri;
        this.sftpFileSystemBuilder = sftpFileSystemBuilder;

    }

    @Override
    public FileSystemProvider provider() {

        return sftpFileSystemBuilder.sftpFileSystemProvider;
    }

    /**
     * A file system is open upon creation and can be closed by invoking its close method. Once closed, any further attempt to access objects in the file system cause ClosedFileSystemException to be thrown.
     * Closing a file system causes all open channels, watch services, and other closeable objects associated with the file system to be closed.
     */
    @Override
    public void close() throws IOException {
        if (this.channelSftp != null) {
            this.channelSftp.disconnect();
        }
        if (this.session != null) {
            this.session.disconnect();
        }
        //TODO: The filesystem pool must be in the sftpFileSystem class and not in the provider
        this.sftpFileSystemBuilder.sftpFileSystemProvider.removeFileSystem(this.uri);
    }

    /**
     * A file system is open upon creation and can be closed by invoking its close method. Once closed, any further attempt to access objects in the file system cause ClosedFileSystemException to be thrown.
     */
    @Override
    public boolean isOpen() {

        if (this.channelSftp == null) {
            return true;
        } else {
            return !this.channelSftp.isClosed();
        }

    }

    /**
     * Whether or not a file system provides read-only access is established when the FileSystem is created and can be tested by invoking its isReadOnly
     *
     * @return boolean if true
     */
    @Override
    public boolean isReadOnly() {

        throw new UnsupportedOperationException();

    }

    /**
     * The name separator is used to separate names in a path string.
     *
     * @return
     */
    @Override
    public String getSeparator() {

        return SftpPath.PATH_SEPARATOR;

    }

    @Override
    public Iterable<Path> getRootDirectories() {
        ArrayList<Path> rootDirectories = new ArrayList<Path>();
        Path rootPath = SftpPath.get(this, SftpPath.ROOT_PREFIX);
        rootDirectories.add(rootPath);
        return rootDirectories;
    }

    @Override
    public Iterable<FileStore> getFileStores() {

        throw new UnsupportedOperationException();

    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return Collections.unmodifiableSet(
                new HashSet<>(Arrays.asList("basic", "posix")));
    }

    @Override
    public Path getPath(String first, String... more) {

        return SftpPath.get(this, first, more);

    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchService newWatchService() throws IOException {
        throw new UnsupportedOperationException();
    }

    public static class SftpFileSystemBuilder {

        private final SftpFileSystemProvider sftpFileSystemProvider;
        private final URI uri;
        private Map<String, String> env;

        public SftpFileSystemBuilder(SftpFileSystemProvider sftpFileSystemProvider, URI uri) {
            this.sftpFileSystemProvider = sftpFileSystemProvider;
            this.uri = uri;
        }


        public SftpFileSystem build() {
            return new SftpFileSystem(this);
        }

        public SftpFileSystemBuilder environmentParameters(Map<String, String> env) {
            this.env = env;
            return this;
        }
    }

    @Override
    public String toString() {

        return "SftpFileSystem{" +
                "uri=" + SftpURIUtil.toStringWithoutPassword(uri) + '}';
    }
}
