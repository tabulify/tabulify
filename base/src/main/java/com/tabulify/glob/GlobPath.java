package com.tabulify.glob;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * A glob path
 * Multiple glob separated by path separator
 */
public class GlobPath {

    private final String globPath;
    private final List<String> globNames;
    private final String globParent;
    private final String globName;

    public GlobPath(String globPattern) {

        /**
         * Wiki normalization
         */
        globPattern = globPattern.replace(":", "/");

        globPath = globPattern;
        globNames = Arrays.asList(globPattern.split("[\\\\/]"));

        String globParent1 = null;
        if (globNames.size() >= 2) {
            globParent1 = globNames.get(globNames.size() - 2);
        }
        globParent = globParent1;

        globName = globNames.get(globNames.size() - 1);
    }

    public boolean isAbsolute() {
        File[] roots = File.listRoots();
        for (File root : roots) {
            if (globPath.startsWith(root.toString())) {
                return true;
            }
        }
        return false;
    }

    public List<String> getNames() {
        return globNames;
    }

    public String getParent() {
        return globParent;
    }

    public String getName() {
        return globName;
    }

}
