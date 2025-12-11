package com.tabulify.glob;

import com.tabulify.fs.Fs;
import com.tabulify.fs.FsShortFileName;

import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Glob utility to search files by glob
 */
public class GlobFs {

    private static final Logger log = Logger.getLogger(GlobFs.class.getName());

    /**
     * @param currentPath - the current path
     * @param globPath    - a glob
     * @return a list of path that matches the glob
     * <p>
     * ex: the following glob
     * /tmp/*.md
     * will return all md file in the tmp directory
     * <p>
     * See also {@link PathMatcher}
     */
    static public List<Path> getFilesByGlob(Path currentPath, GlobPath globPath) {

        /**
         * Glob Path File System Based
         */


        /**
         * What is the start path ?
         * If the glob is an absolute path, we start from the root
         * otherwise it's relative to the current path given as argument
         */
        Path startPath;

        /**
         * Glob Path properties
         */


        // if not root in glob
        List<String> stringNames = new ArrayList<>();
        // Absolute glob path
        Path rootPath = currentPath.getRoot();
        if (globPath.isAbsolute()) {
            startPath = rootPath;
            stringNames = globPath.getNames();
        } else {
            startPath = currentPath;
            stringNames = globPath.getNames();
        }

        /**
         * Normalize
         * Delete actual and parent names (ie point and double point)
         */
        List<String> stringNamesNormalized = new ArrayList<>();
        for (String stringName : stringNames) {

            /**
             * Actual
             */
            if (stringName.equals(".")) {
                continue;
            }

            /**
             * Go to parent
             */
            if (stringName.equals("..")) {
                // delete last name
                stringNamesNormalized = stringNamesNormalized.subList(0, stringNamesNormalized.size() - 2);
                continue;
            }

            stringNamesNormalized.add(stringName);
        }


        /**
         * Recursive processing
         * We go through all glob names and retain only
         * the matched paths in the currentMatchesPaths variable
         */
        List<Path> currentMatchesPaths = new ArrayList<>();
        currentMatchesPaths.add(startPath);
        boolean recursiveWildCardFound = false;
        for (String name : stringNamesNormalized) {

            if (name.equals(Glob.DOUBLE_STAR)) {
                recursiveWildCardFound = true;
                continue;
            }

            FsShortFileName sfn = FsShortFileName.of(name);
            Pattern pattern;
            if (!sfn.isShortFileName()) {
                pattern = Pattern.compile(Glob.createOf(name).toRegexPattern());
            } else {
                pattern = Pattern.compile(Glob.createOf(sfn.getShortName() + "*").toRegexPattern(), Pattern.CASE_INSENSITIVE);
            }

            /**
             * Is it a recursive traversal, now ?
             */
            if (!recursiveWildCardFound) {
                /**
                 * This is not, we just look up this level
                 */
                List<Path> matchesPath = new ArrayList<>();
                for (Path currentRecursivePath : currentMatchesPaths) {
                    List<Path> recursiveMatchesPath = getMatchesPath(pattern, currentRecursivePath, false);
                    matchesPath.addAll(recursiveMatchesPath);
                }
                // Recursion
                currentMatchesPaths = matchesPath;
                // Break if there is no match
                if (matchesPath.isEmpty()) {
                    break;
                }
            } else {
                List<Path> matchedFiles = new ArrayList<>();
                while (!currentMatchesPaths.isEmpty()) {
                    List<Path> matchesPath = new ArrayList<>();
                    for (Path currentRecursivePath : currentMatchesPaths) {
                        List<Path> currentMatchesPath = getMatchesPath(pattern, currentRecursivePath, true);
                        matchesPath.addAll(currentMatchesPath);
                    }
                    /**
                     * Recursion only on directories
                     */
                    currentMatchesPaths = matchesPath
                            .stream()
                            .filter(Files::isDirectory)
                            .collect(Collectors.toList());
                    /**
                     * Collect the files
                     */
                    matchedFiles.addAll(
                            matchesPath
                                    .stream()
                                    .filter(Files::isRegularFile)
                                    .collect(Collectors.toList())
                    );
                }
                /// Update the returned variables
                currentMatchesPaths = matchedFiles;
                break;
            }
        }

        return currentMatchesPaths;

    }

    private static List<Path> getMatchesPath(Pattern pattern, Path currentRecursivePath, Boolean recursiveWildCardFound) {

        // The list where the actual matches path will be stored
        List<Path> matchesPath = new ArrayList<>();
        // There is also newDirectoryStream
        // https://docs.oracle.com/javase/8/docs/api/java/nio/file/Files.html#newDirectoryStream-java.nio.file.Path-java.lang.String-
        // but yeah ...
        if (Files.isDirectory(currentRecursivePath)) {
            try {
                List<Path> paths = Fs.getChildrenFiles(currentRecursivePath);
                for (Path childrenPath : paths) {
                    if (pattern.matcher(childrenPath.getFileName().toString()).find()) {
                        matchesPath.add(childrenPath);
                    } else {
                        if (recursiveWildCardFound) {
                            if (Files.isDirectory(childrenPath)) {
                                matchesPath.add(childrenPath);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                if (e.getCause().getClass().equals(AccessDeniedException.class)) {
                    log.warning(() -> "The path (" + currentRecursivePath + ") was denied");
                } else {
                    throw e;
                }
            }
        } else {
            if (pattern.matcher(currentRecursivePath.getFileName().toString()).find()) {
                matchesPath.add(currentRecursivePath);
            }
        }
        return matchesPath;
    }
}
