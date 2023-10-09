package net.bytle.niofs.sftp;

import com.jcraft.jsch.SftpException;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;

/**
 * A factory for a Sftp file system
 */
public class SftpFileSystemProvider extends FileSystemProvider {

  /**
   * Even if it's SFTP, the ssh scheme permits to use the same URI
   * to execute code
   */
  static final String SFTP_SCHEME = "ssh";

  // The pool of Sftp Connection
  private static final Map<URI, SftpFileSystem> fileSystemPool = new HashMap<>();

  @Override
  public String getScheme() {
    return SFTP_SCHEME;
  }

  /**
   * The newFileSystem method is used to create a file system
   */
  @Override
  public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {

    String key = toFileSystemId(uri, env);
    if (fileSystemPool.containsKey(key)) {
      return fileSystemPool.get(key);
    } else {
      // Build a Sftp File System object
      SftpFileSystem sftpFileSystem = new SftpFileSystem.SftpFileSystemBuilder(this, uri)
        .environmentParameters((Map<String, String>) env)
        .build();

      // Add the file system in the pool
      fileSystemPool.put(uri, sftpFileSystem);

      // Return it
      return sftpFileSystem;

    }

  }


  /**
   * @param path
   * @param options
   * @param attrs
   * @return A FileChannel object that allows a file to be read or written in the file system.
   * @throws IOException
   */
  @Override
  public FileChannel newFileChannel(Path path,
                                    Set<? extends OpenOption> options,
                                    FileAttribute<?>... attrs) {
    throw new UnsupportedOperationException();
  }

  /**
   * The getFileSystem method is used to retrieve a reference to an existing file system
   *
   * @param uri
   * @return a FileSystem
   */
  @Override
  public FileSystem getFileSystem(URI uri) {

    FileSystem sftpFileSystem = fileSystemPool.get(uri);
    if (sftpFileSystem == null) {
      try {
        sftpFileSystem = newFileSystem(uri, null);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    assert sftpFileSystem != null;
    return sftpFileSystem;

  }

  /**
   * To construct an ID of the file System from the URI
   * to support the function {@link #getFileSystem(URI)}
   *
   * @param uri
   * @return
   */
  private String toFileSystemId(URI uri) {
    return toFileSystemId(uri, null);
  }

  private String toFileSystemId(URI uri, Map<String, ?> env) {

    String id = null;
    if (uri.getUserInfo() != null) {
      id = uri.getUserInfo();
    }
    if (uri.getHost() != null) {
      id += uri.getHost();
    } else {
      id += "localhost";
    }
    if (uri.getPort() != -1) {
      id += uri.getPort();
    } else {
      id += "22";
    }

    return id;

  }

  /**
   * When the {@link SftpFileSystem#close() file System close}, the file system must be deleted from the pool
   *
   * @param uri
   * @return
   */
  protected FileSystem removeFileSystem(URI uri) {

    return fileSystemPool.remove(toFileSystemId(uri));

  }

  @Override
  public Path getPath(URI uri) {

    String path = uri.getPath();
    return getFileSystem(uri).getPath(path);

  }

  /**
   * Seekable means read
   *
   * @param path
   * @param options
   * @param attrs
   * @return
   * @throws IOException
   */
  @Override
  public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {

    if (options.size() == 0) {
      Set<StandardOpenOption> optionsSet = new HashSet<>();
      optionsSet.add(StandardOpenOption.READ);
      return new SftpByteChannel(toSftpPath(path), optionsSet);
    } else {
      return new SftpByteChannel(toSftpPath(path), options);
    }


  }

  @Override
  public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
    return new SftpDirectoryStream(toSftpPath(dir), filter);

  }

  /**
   * Attempts to write to file stores by means of an object associated with a read-only file system throws ReadOnlyFileSystemException.
   *
   * @param dir
   * @param attrs
   * @throws IOException
   */
  @Override
  public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {

    try {
      toSftpPath(dir).createDirectory();
    } catch (SftpException e) {
      throw new RuntimeException(e);
    }

  }

  @Override
  public void delete(Path path) throws IOException {

    SftpPath sftpPath = toSftpPath(path);
    if (Files.exists(sftpPath)) {
      try {

        if (Files.isDirectory(sftpPath)) {

          sftpPath.getChannelSftp().rmdir(sftpPath.toAbsolutePath().toString());

        } else {

          sftpPath.getChannelSftp().rm(sftpPath.toAbsolutePath().toString());

        }

      } catch (SftpException e) {
        throw new IOException(e);
      }
    }


  }

  @Override
  public void copy(Path source, Path target, CopyOption... options) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void move(Path source, Path target, CopyOption... options) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isSameFile(Path path, Path path2) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isHidden(Path path) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public FileStore getFileStore(Path path) throws IOException {
    throw new UnsupportedOperationException();
  }

  /**
   * Checks the existence, and optionally the accessibility, of a file.
   *
   * @param path
   * @param modes
   * @throws UnsupportedOperationException - an implementation is required to support checking for READ, WRITE, and EXECUTE access. This exception is specified to allow for the Access enum to be extended in future releases.
   * @throws NoSuchFileException           - if a file does not exist (optional specific exception)
   * @throws AccessDeniedException         - the requested access would be denied or the access cannot be determined because the Java virtual machine has insufficient privileges or other reasons. (optional specific exception)
   * @throws IOException                   - if an I/O error occurs
   * @throws SecurityException             - In the case of the default provider, and a security manager is installed, the checkRead is invoked when checking read access to the file or only the existence of the file, the checkWrite is invoked when checking write access to the file, and checkExec is invoked when checking execute access.
   */
  @Override
  public void checkAccess(Path path, AccessMode... modes) throws IOException {

    // check the existence, the read attributes will throw a NoSuchFileException
    Files.readAttributes(path, SftpPosixFileAttributes.class);

    // Check a little bit the accessibility
    // TODO: check that the connected user has the right to do the operation
    // Not easy as we get only the uid...
    for (AccessMode mode : modes) {
      switch (mode) {
        case READ:
          break;
        case WRITE:
          if (path.getFileSystem().isReadOnly())
            throw new AccessDeniedException(toString());
          break;
        case EXECUTE:
          break;
        default:
          throw new UnsupportedOperationException();
      }
    }


  }

  @Override
  public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
    // The type permits to build and return the good view
    // For now, there is only
    //    - a basic implementations
    if (type == BasicFileAttributeView.class) {
      return SftpFileBasicAttributeView.get(path);
    } else if (type == PosixFileAttributeView.class) {
      return SftpPosixFileAttributeView.get(path);
    } else {
      throw new UnsupportedOperationException("The class (" + type + ") is not supported.");
    }
  }

  /**
   * This function is used to test if a file exist
   * See {@link Files#notExists(Path, LinkOption...)}
   * If the file doesn't exist, it must throws a IOException, otherwise it exist
   *
   * @param path
   * @param type<A>
   * @param options
   * @return
   * @throws IOException
   */
  @Override
  public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {

    if (type == BasicFileAttributes.class || type == SftpBasicFileAttributes.class) {
      return (A) new SftpBasicFileAttributes((SftpPath) path);
    } else if (type == PosixFileAttributes.class || type == SftpPosixFileAttributes.class) {
      return (A) new SftpPosixFileAttributes((SftpPath) path);
    } else {
      throw new UnsupportedOperationException("The class (" + type + ") is not supported.");
    }

  }

  /**
   * This function is used to test if a file exist
   * If the file doesn't exist, it must throws a IOException, otherwise it exist
   *
   * @param path
   * @param attributes
   * @param options
   * @return
   * @throws IOException
   */
  @Override
  public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {

    String type;
    String attr;

    int colonPos = attribute.indexOf(':');
    if (colonPos == -1) {
      type = "basic";
      attr = attribute;
    } else {
      type = attribute.substring(0, colonPos++);
      attr = attribute.substring(colonPos);
    }
    if (type.equals("basic")) {
      SftpFileBasicAttributeView view = SftpFileBasicAttributeView.get(path);
      view.setAttribute(attr, value);
    } else {
      throw new UnsupportedOperationException("view <" + type + "> is not supported");
    }


  }

  // Checks that the given file is a SftpPath
  static final SftpPath toSftpPath(Path path) {
    if (path == null)
      throw new NullPointerException();
    if (!(path instanceof SftpPath))
      throw new ProviderMismatchException();
    return (SftpPath) path;
  }


}
