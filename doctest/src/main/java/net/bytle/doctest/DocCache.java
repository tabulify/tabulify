package net.bytle.doctest;

import net.bytle.fs.Fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class DocCache {

    private final Path propertiesFile = Paths.get(Fs.getAppData(DocTest.APP_NAME).toString(),"executionCache.properties");
    private final Properties appDataProperties = new Properties();

    private static DocCache docCache;
    private DocCache() {

        if (Files.exists(propertiesFile)) {
            try {
                InputStream in = Files.newInputStream(propertiesFile);
                appDataProperties.load(in);
                in.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


    }

    public static DocCache get() {
        if (docCache==null){
            docCache = new DocCache();
        }
        return docCache;
    }


    public String getMd5(Path path) {
        return appDataProperties.getProperty(path.toString());
    }

    public void store(Path path) {
        try {
            String md5 = Fs.getMd5(path);
            appDataProperties.put(path.toString(),md5);
            OutputStream ou = Files.newOutputStream(propertiesFile);
            appDataProperties.store(ou,"comments");
            ou.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
