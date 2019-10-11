/*
 Copyright 2012-2013 University of Stavanger, Norway

 Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package net.bytle.niofs.sftp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.util.*;

/**
 * A Path implementation for SFTP.
 * An object that may be used to locate a file in a file system.
 * It will typically represent a system dependent file stringPath.
 * <p/>
 * Let op:
 * This sftp client has the concept of a current local directory and a current remote directory. These are not inherent to the protocol,
 * but are used implicitly for all path-based commands sent to the server (for the remote directory) or accessing the local file system
 * (for the local directory).
 * They can be queried by lpwd() and pwd(), and changed by cd(dir) and lcd(dir).
 */
class SftpPath implements Path {


    protected static final String ROOT_PREFIX = "/";
    protected static final String PATH_SEPARATOR = "/";

    private final SftpFileSystem sftpFileSystem;
    private final boolean isAbsolute;
    private String stringPath;

    private List<String> names;
    private List<String> relativeDirectoryNames; // The relative directory in a names format, get it through the function {@link #getRelativeDirectoryNames}


    /**
     * Create a path
     *
     * @param sftpFileSystem:         The File System,
     * @param stringPath:             A string path
     * @param relativeDirectoryNames; The relative directory to stringPath if any (Intern parameters)
     */
    private SftpPath(FileSystem sftpFileSystem, String stringPath, List<String> relativeDirectoryNames) {

        this.sftpFileSystem = (SftpFileSystem) sftpFileSystem;
        this.stringPath = stringPath;
        if (relativeDirectoryNames!=null) {
            setRelativeDirectoryNames(relativeDirectoryNames);
        }

        if (relativeDirectoryNames == null) {
            if (this.stringPath.startsWith(ROOT_PREFIX)) {
                isAbsolute = true;
            } else {
                isAbsolute = false;
                // This means that the path is relative to the working directory.
                // The working directory is not get here because we don't want to make a sftp connection if it's not needed
                // We have then a wrapper function. See {@link #getRelativeDirectoryNames}
            }
        } else {
            isAbsolute = false;
        }

        // Parse the stringPath parameters to a Arrays List of names
        if (stringPath != null) {

            String stringPathToParse = stringPath;
            if (isAbsolute) {
                stringPathToParse = stringPath.substring(1, stringPath.length());
            }
            this.names = new ArrayList(
                    Arrays.asList(stringPathToParse.split(PATH_SEPARATOR))
            );

        }


    }


    public FileSystem getFileSystem() {
        return this.sftpFileSystem;
    }

    public boolean isAbsolute() {

        return isAbsolute;

    }

    public Path getRoot() {
        if (isAbsolute) {
            return new SftpPath(sftpFileSystem,
                    ROOT_PREFIX,
                    null);
        } else {
            return null;
        }
    }

    /**
     * Return a relative path from a relative directory
     * This will print only the file name with the {@link #toString} method
     *
     * @return a relative path from a relative directory
     */
    public Path getFileName() {

        List relativeDirectoryNames = new ArrayList<>();
        if (!isAbsolute) {
            relativeDirectoryNames.addAll(this.getRelativeDirectoryNames());
        }
        relativeDirectoryNames.addAll(this.names.subList(0, this.names.size() - 1));

        String stringPathName = this.names.get(this.names.size() - 1);
        return new SftpPath(this.sftpFileSystem, stringPathName, relativeDirectoryNames);

    }

    public Path getParent() {

        if (isAbsolute) {

            if (this.names.size() == 0) {
                return null; // There is no parent, this is the root
            } else {
                return new SftpPath(sftpFileSystem,
                        ROOT_PREFIX + String.join(PATH_SEPARATOR, this.names.subList(0, this.names.size() - 1)),
                        null);
            }

        } else {

            return new SftpPath(sftpFileSystem,
                    String.join(PATH_SEPARATOR, this.names.subList(0, this.names.size() - 1)),
                    this.getRelativeDirectoryNames());

        }


    }

    public int getNameCount() {
        return this.names.size();
    }

    /**
     * Return the path corresponding to the index
     *
     * @param index
     * @return
     */
    public Path getName(int index) {

        List<String> localRelativeDirectoryNames = new ArrayList<>();
        if (!isAbsolute) {

            localRelativeDirectoryNames = this.getRelativeDirectoryNames();

        }
        if (index > 0) {
            localRelativeDirectoryNames.addAll(this.names.subList(0, index));
        }

        return new SftpPath(sftpFileSystem,
                this.names.get(index),
                localRelativeDirectoryNames);

    }

    public Path subpath(int beginIndex, int endIndex) {

        List<String> relativeDirectoryNames = new ArrayList<>();
        if (!isAbsolute) {

            relativeDirectoryNames = this.getRelativeDirectoryNames();

        }
        if (beginIndex > 0) {
            relativeDirectoryNames.addAll(this.names.subList(0, beginIndex));
        }

        return
                new SftpPath(sftpFileSystem,
                        String.join(PATH_SEPARATOR, this.names.subList(beginIndex, endIndex)),
                        this.relativeDirectoryNames);

    }

    public boolean startsWith(Path other) {
        throw new UnsupportedOperationException();
    }

    public boolean startsWith(String other) {
        throw new UnsupportedOperationException();
    }

    public boolean endsWith(Path other) {
        throw new UnsupportedOperationException();
    }

    public boolean endsWith(String other) {
        throw new UnsupportedOperationException();
    }

    public Path normalize() {
        throw new UnsupportedOperationException();
    }

    public Path resolve(Path other) {
        throw new UnsupportedOperationException();
    }

    public Path resolve(String other) {
        throw new UnsupportedOperationException();
    }

    public Path resolveSibling(Path other) {
        throw new UnsupportedOperationException();
    }

    public Path resolveSibling(String other) {
        throw new UnsupportedOperationException();
    }

    public Path relativize(Path other) {
        throw new UnsupportedOperationException();
    }

    public URI toUri() {
        throw new UnsupportedOperationException();
    }

    public Path toAbsolutePath() {
        if (this.isAbsolute()) {
            return this;
        } else {

            String relativePath = ROOT_PREFIX + String.join(PATH_SEPARATOR, this.getRelativeDirectoryNames());
            if (!this.stringPath.equals("")) {
                return get(sftpFileSystem, relativePath + sftpFileSystem.getSeparator() + this.stringPath);
            } else {
                return get(sftpFileSystem, relativePath);
            }

        }

    }


    public Path toRealPath(LinkOption... options) throws IOException {
        throw new UnsupportedOperationException();
    }

    public File toFile() {
        throw new UnsupportedOperationException();
    }

    public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers) throws IOException {
        throw new UnsupportedOperationException();
    }

    public WatchKey register(WatchService watcher, Kind<?>... events) throws IOException {

        throw new UnsupportedOperationException();

    }

    public Iterator<Path> iterator() {

        return new Iterator<Path>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return (i < getNameCount());
            }

            @Override
            public Path next() {
                if (i < getNameCount()) {
                    Path result = getName(i);
                    i++;
                    return result;
                } else {
                    throw new NoSuchElementException();
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

        };

    }

    public int compareTo(Path other) {
        throw new UnsupportedOperationException();
    }


    /**
     * @return the string
     */
    public String toString() {

        return this.stringPath;

    }

    /**
     * A shortcut to get the ChannelSftp saved in the file systen object
     *
     * @return ChannelSftp
     */
    protected ChannelSftp getChannelSftp() {
        return this.sftpFileSystem.getChannelSftp();
    }


    /**
     * Implementation of the createDirectory function of the FileSystemProvider
     * This function is called when this is a directory
     * She then just loop on each names and create them
     *
     * @throws SftpException
     */
    protected void createDirectory() throws SftpException {

        for (int i = 0; i < getNameCount(); i++) {
            String dirPath = getName(i).toAbsolutePath().toString();
            try {
                this.getChannelSftp().stat(dirPath);
            } catch (SftpException e) {
                if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                    this.getChannelSftp().mkdir(dirPath);
                } else {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static Path get(FileSystem fileSystem, String first, String[] more) {
        String path;
        if (more == null) {
            path = first;
        } else {
            // Build the path from the list of directory
            StringBuilder sb = new StringBuilder();
            sb.append(first);
            for (String segment : more) {
                if (segment.length() > 0) {
                    if (sb.length() > 0)
                        sb.append(PATH_SEPARATOR);
                    sb.append(segment);
                }
            }
            path = sb.toString();
        }
        return new SftpPath(fileSystem, path, null);
    }

    /**
     * A static constructor
     * You can also get a stringPath from a provider with an URI.
     *
     * @param sftpFileSystem
     * @param path
     * @return
     */
    protected static Path get(FileSystem sftpFileSystem, String path) {
        return get(sftpFileSystem, path, null);
    }

    /**
     * An helper function to populate the relativeDirectoryNames variable
     * when the path is relative to the working directory (ie when relativeDirectoryNames == null)
     * This prevent to make an SFTP connection when manipulating absolute path
     *
     * @return the relative directory in a list of names form
     */
    private List<String> getRelativeDirectoryNames() {

        // The path is relative but we don't have the relative directory
        // This means that the path is relative to the working directory
        if (!isAbsolute && relativeDirectoryNames == null) {

            String workingDirectory = ((SftpFileSystem) getFileSystem()).getWorkingDirectory();
            List<String> strings = Arrays.asList(workingDirectory.split(PATH_SEPARATOR));
            // If this is not relative to the root
            if (strings.size() != 0) {
                // The subList function suppress the root that is at the first place after the split so that
                // we don't have any empty value
                setRelativeDirectoryNames(new ArrayList(strings.subList(1, strings.size())));
            } else {
                setRelativeDirectoryNames(new ArrayList());
            }

            // To break the passing by reference, we are creating a new object
            // because the relatives directory names
            // Are a property of the path
            return new ArrayList(this.relativeDirectoryNames);

        } else {

            // To break the passing by reference, we are creating a new object
            // because the relatives directory names
            // Are a property of the path
            return new ArrayList(this.relativeDirectoryNames);

        }

    }

    public void setRelativeDirectoryNames(List<String> relativeDirectoryNames)  {
        if (this.relativeDirectoryNames !=null){
            throw new IllegalArgumentException("The relative directory names have already a value");
        }
        this.relativeDirectoryNames = relativeDirectoryNames;
    }
}