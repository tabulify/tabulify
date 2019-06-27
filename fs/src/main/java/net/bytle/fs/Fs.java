package net.bytle.fs;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.List;

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
     *
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
}
