package net.bytle.fs;


import net.bytle.os.Oss;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.bytle.os.Oss.LINUX;
import static net.bytle.os.Oss.WIN;
import static net.bytle.type.Bytes.printHexBinary;


public class Fs {


    /**
     * A safe method even if the string is not a path
     *
     * @param s
     * @return
     */
    public static boolean isFile(String s) {
        try {
            return Files.isRegularFile(Paths.get(s));
        } catch (java.nio.file.InvalidPathException e) {
            return false;
        }
    }

    public static String getFileContent(Path path) {
        try {

            StringBuilder s = new StringBuilder();
            BufferedReader reader;
            reader = new BufferedReader(new FileReader(path.toFile()));
            String line;
            while ((line = reader.readLine()) != null) {
                s.append(line).append(System.getProperty("line.separator"));
            }

            return s.toString();

        } catch (FileNotFoundException e) {
            throw new RuntimeException("Unable to find the file (" + path.toAbsolutePath().normalize().toString() + ")", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Safe is directory method even if the string is not a path
     *
     * @param s
     * @return
     */
    public static boolean isDirectory(String s) {
        try {
            return Files.isDirectory(Paths.get(s));
        } catch (java.nio.file.InvalidPathException e) {
            return false;
        }
    }

    /**
     * An alias to the function {@link Files#write(Path, byte[], OpenOption...)}
     * without any option.
     *
     * @param s    - the string to write
     * @param path - the path to the file to write
     */
    public static void toFile(String s, Path path) {
        try {
            Files.write(path, s.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Same as Files.walk(Paths.get(path))
     *
     * @param path - the start path to scan
     * @return - the children of a directory or the file if it's a file
     */
    public static List<Path> getDescendantFiles(Path path) {

        // Path to return
        List<Path> pathsCollector = new ArrayList<>();
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(path)) {

                for (Path childPath : dirStream) {
                    if (Files.isRegularFile(childPath)) {
                        pathsCollector.add(childPath);
                    } else {
                        addChildFiles(pathsCollector, childPath);
                    }
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            pathsCollector.add(path);
        }
        return pathsCollector;

    }

    /**
     * Recursive function usd by {@link #getDescendantFiles(Path)}
     *
     * @param path
     * @param pathsCollector
     */
    static void addChildFiles(List<Path> pathsCollector, Path path) {

        if (Files.isDirectory(path)) {

            try (DirectoryStream<Path> childrenFiles = Files.newDirectoryStream(path)) {

                for (Path childPath : childrenFiles) {
                    if (Files.isRegularFile(childPath)) {
                        pathsCollector.add(childPath);
                    } else {
                        addChildFiles(pathsCollector, childPath);
                    }
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            pathsCollector.add(path);
        }

    }

    /**
     * @param content
     * @return a path to a txt file with the string as content
     */
    public static Path createTempFile(String content) {
        return createTempFile(content, ".txt");
    }

    /**
     * @param content -  the content of the file
     * @param suffix  - the file suffix. Example: ".txt"
     * @return a temp file in the temp directory
     */
    public static Path createTempFile(String content, String suffix) {

        try {

            Path temp = createTempDirectory(null);

            Path tempFile = Files.createTempFile(temp, null, suffix);
            Files.write(tempFile, content.getBytes());

            return tempFile;

        } catch (IOException e) {

            throw new RuntimeException(e);

        }

    }



    /**
     * @param prefix - a prefix to generate the directory name (may be null)
     * @return a temp directory
     */
    public static Path createTempDirectory(String prefix) {

        try {

            return Files.createTempDirectory(prefix);

        } catch (IOException e) {

            throw new RuntimeException(e);

        }

    }

    /**
     * @param path
     * @return See also: http://code.google.com/p/guava-libraries/wiki/HashingExplained
     */
    public static String getMd5(Path path) {

        if (Files.isDirectory(path)) {
            throw new RuntimeException("Md5 calculation for directory is not implemented. No md5 for (" + path.toAbsolutePath().toString());
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            byte[] hash = MessageDigest.getInstance("MD5").digest(bytes);
            return printHexBinary(hash);
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Return the AppData Directory
     * This directory contains data for one user.
     *
     * @param appName
     * @return
     */
    public static Path getAppData(String appName) {
        Path appData;
        switch (Oss.getType()) {
            case WIN:
                appData = Paths.get(getUserHome().toString(), "AppData", "Local", appName);
                break;
            case LINUX:
                appData = Paths.get(getUserHome().toString(), "." + appName);
                break;
            default:
                throw new RuntimeException("AppData directory for OS " + Oss.getName() + " is not implemented");
        }

        try {
            Files.createDirectories(appData);
            return appData;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    private static Path getUserHome() {
        final String home = System.getProperty("user.home");
        return Paths.get(home);
    }

    private static String getPathSeparator() {
        return System.getProperty("file.separator");
    }

    /**
     * @return the system (process) encoding
     * ie the value of the system property file.encoding
     */
    private static String getSystemEncoding() {
        return System.getProperty("file.encoding");
    }

    private static Path getTempDir() {
        return Paths.get(System.getProperty("java.io.tmpdir"));
    }


    /**
     * Create a file and all sub-directories if needed
     *
     * @param path
     */
    public static void createFile(Path path) {
        try {
            Path parent = path.getParent();
            Files.createDirectories(parent);
            Files.createFile(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Wrapper around {@link Files#write(Path, byte[], OpenOption...)}
     * to write a string to a file
     * without exception handling
     *
     * @param path
     * @param s
     */
    public static void write(Path path, String s) {
        try {
            Files.write(path, s.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A wrapper around path.relativize(base)
     *
     * @param path
     * @param base
     * @return a relative path
     */
    public static Path relativize(Path path, Path base) {
        return base.relativize(path);
    }

    public static void overwrite(Path source, Path target) {
        try {
            Files.write(target, Files.readAllBytes(source), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param path     - a regular file or a directory
     * @param basePath - a base directory
     * @return a list of name between the path and the base path
     * <p>
     * Example
     * * basePath = /tmp
     * * path = /tmp/foo/bar/blue
     * You will get { 'foo', 'bar'}
     */
    public static List<String> getDirectoryNamesInBetween(Path path, Path basePath) {

        if (basePath.getNameCount() > path.getNameCount()) {
            throw new RuntimeException("The base path should have less levels than the path");
        }
        List<String> names = new ArrayList<>();
        // -1 in the end limit because the last name if the file name, we don't return it
        for (int i = 0; i < path.getNameCount() - 1; i++) {
            String name = path.getName(i).toString();
            if (i <= basePath.getNameCount() - 1) {
                String baseName = basePath.getName(i).toString();
                if (!baseName.equals(name)) {
                    throw new RuntimeException("The path doesn't share a common branch with the base path. At the level (" + (i + 1) + ", the name is different. We got (" + name + ") for the path and (" + baseName + ") for the base path");
                }
            } else {
                names.add(name);
            }
        }
        return names;
    }


    /**
     * path.isAbsolute just tell you that the object path is absolute, not the path
     * <p>
     * This method check if there is a root
     * If this is the case, the path is absolute otherwise not.
     *
     * @param path
     * @return true if the path has a root (is absolute)
     */
    public static boolean isAbsolute(Path path) {
        if (path.getRoot() != null) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * @param path
     * @return the children path of a directory
     */
    public static List<Path> getChildrenFiles(Path path) {

        try {
            List<Path> childrenPaths = new ArrayList<>();
            for (Path childPath : Files.newDirectoryStream(path)) {
                childrenPaths.add(childPath);
            }
            return childrenPaths;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param segments
     * @return a local pah from an array string
     */
    public static Path getPath(String[] segments) {
        String[] more = Arrays.copyOfRange(segments, 1, segments.length);
        return Paths.get(segments[0], more);
    }

    /**
     * @param path
     * @return the file name without the extension
     */
    public static String getFileName(Path path) {
        final String fileName = path.getFileName().toString();
        final int endIndex = fileName.indexOf(".");
        if (endIndex == -1) {
            return fileName;
        } else {
            return fileName.substring(0, endIndex);
        }
    }

    /**
     * @param path - a file or a directory
     */
    public static List<Path> deleteIfExists(Path path) {
        if (Files.exists(path)){
            return Fs.delete(path);
        } else {
            return new ArrayList<>();
        }
    }

    private static List<Path> delete(Path path) {
        try {
            List<Path> deletedPaths = new ArrayList<>();
            if (Files.isDirectory(path)) {
                try (Stream<Path> walk = Files.walk(path)) {
                    deletedPaths = walk.sorted(Comparator.reverseOrder())
                            .filter(s->!Files.isDirectory(s))
                            .flatMap(s -> Fs.delete(s).stream())
                            .collect(Collectors.toList());
                }
            } else {

                Files.delete(path);
                deletedPaths.add(path);

            }
            return deletedPaths;
        } catch (IOException e) {
            throw  new RuntimeException(e);
        }
    }

    /**
     * Return a temporary file path.
     *
     * This function is a wrapper around the function {@link Files#createTempFile(String, String, FileAttribute[])}
     * but delete the file to return only a unique path
     *
     * If you want to create it, use a create function such as {@link #createFile(Path)}
     *
     * @param prefix the beginning of the file name
     * @param suffix the extension (example .txt) when null '.tmp'
     * @return a temporary file path
     *
     * Example:
     * Path path = Fs.getTempFilePath("test",".csv");
     */
    public static Path getTempFilePath(String prefix, String suffix) {

        try {
            Path path = Files.createTempFile(prefix,suffix);
            Files.deleteIfExists(path);
            return path;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static boolean isEmpty(Path path) {
        try {
            return Files.size(path)==0;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Path getTempDirectory() {
        try {
            return Files.createTempDirectory("tmp");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
