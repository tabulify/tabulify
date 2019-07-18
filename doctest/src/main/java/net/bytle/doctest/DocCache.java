package net.bytle.doctest;

import net.bytle.fs.Fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class DocCache {


    private final Path cacheDirectory;

    private DocCache(String name) {

        cacheDirectory = Paths.get(Fs.getAppData(DocTestExecutor.APP_NAME).toString(),name);
        if (!Files.exists(cacheDirectory)){
            try {
                Files.createDirectory(cacheDirectory);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


    }

    /**
     * The name is a namespace to be able to cache two different set of doc
     * @param name
     * @return
     */
    public static DocCache get(String name) {

        return new DocCache(name);
    }


    public String getMd5(Path path) {
        Path cacheFilePath = getPathCacheFile(path);
        if (Files.exists(cacheFilePath)) {
            return Fs.getMd5(cacheFilePath);
        } else {
            return null;
        }
    }

    protected Path getPathCacheFile(Path path) {
        Path relativeCachePath = path;
        if (relativeCachePath.isAbsolute()){
            relativeCachePath = Fs.relativize(path,path.getRoot());
        }
        return Paths.get(cacheDirectory.toString(),relativeCachePath.toString()).normalize();
    }

    public void store(Path path) {
        try {
            Path cachePath = getPathCacheFile(path);
            Path parent = cachePath.getParent();
            if (!(Files.exists(parent))){
                Files.createDirectories(parent);
            }
            Fs.overwrite(path,cachePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public List<DocTestUnit> getDocTestUnits(Path path) {
        final Path pathCacheFile = getPathCacheFile(path);
        if (Files.exists(pathCacheFile)) {
            return DocTestParser.getDocTests(pathCacheFile);
        } else {
            return null;
        }
    }
}
