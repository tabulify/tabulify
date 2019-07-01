package net.bytle.fs;


import java.io.*;

import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import net.bytle.os.Oss;


import javax.xml.bind.DatatypeConverter;

import static net.bytle.os.Oss.LINUX;
import static net.bytle.os.Oss.WIN;


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
     * @param path - the start path to scan
     * @return - the childs of a directory or the file if it's a file
     */
    public static List<Path> getChildFiles(Path path) {

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
     * Recursive function usd by {@link #getChildFiles(Path)}
     *
     * @param path
     * @param pathsCollector
     */
    static void addChildFiles(List<Path> pathsCollector, Path path) {

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

    }

    /**
     * @param content
     * @return a path to a txt file with the string as content
     */
    public static Path createTempFile(String content) {
        return createTempFile(content, ".txt");
    }

    public static Path createTempFile(String content, String suffix) {

        try {

            Path temp = Paths.get(System.getProperty("java.io.tmpdir"), "bytle");
            Files.createDirectories(temp);

            Path tempFile = Files.createTempFile(temp, "bytle-test", suffix, new FileAttribute[0]);
            Files.write(tempFile, content.getBytes());

            return tempFile;

        } catch (IOException e) {

            throw new RuntimeException(e);

        }

    }

    /**
     * @param path
     * @return See also: http://code.google.com/p/guava-libraries/wiki/HashingExplained
     */
    public static String getMd5(Path path) {

        try {
            byte[] bytes = Files.readAllBytes(path);
            byte[] hash = MessageDigest.getInstance("MD5").digest(bytes);
            return DatatypeConverter.printHexBinary(hash);
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
}
