package com.tabulify.niofs.http;

import com.tabulify.type.Casts;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;

public final class HttpFileSystem extends FileSystem {



    private final HttpFileSystemProvider provider;
    private final URL url;
    @SuppressWarnings({"FieldCanBeLocal", "MismatchedQueryAndUpdateOfCollection"})
    private final Map<String, Object> envs = new HashMap<>();
    private String user;

    private String password;

    public HttpFileSystem(HttpFileSystemProvider provider, URL url, final Map<String, ?> envs) {

        this.provider = provider;
        this.url = url;

        if (envs != null) {
            for (Map.Entry<String, ?> env : envs.entrySet()) {

                String attribute = env.getKey();
                Object value = env.getValue();

                HttpRequestAttribute attributeObject;
                try {
                    attributeObject = Casts.cast(attribute, HttpRequestAttribute.class);
                    switch (attributeObject) {
                        case USER:
                            this.setUser(value.toString());
                            break;
                        case PASSWORD:
                            this.setPassword(value.toString());
                            break;
                    }
                } catch (Exception e) {
                    // not known
                    this.envs.put(attribute, value);
                }

            }
        }


    }

    HttpFileSystem setUser(String user) {
        this.user = user;
        return this;
    }

    public boolean hasPassword() {
        return this.password != null;
    }




    HttpFileSystem setPassword(String password) {
        this.password = password;
        return this;
    }

    @Override
    public FileSystemProvider provider() {
        return provider;
    }


    @Override
    public void close() {

    }


    @Override
    public boolean isOpen() {
        return true;
    }


    @Override
    public boolean isReadOnly() {
        return true;
    }


    @Override
    public String getSeparator() {
        return "/";
    }

    @Override
    public Iterable<Path> getRootDirectories() {

        HttpPath rootPath = getPath(this.getSeparator());
        return Collections.singletonList(rootPath);

    }


    @Override
    public Iterable<FileStore> getFileStores() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public HttpPath getPath(final String first, final String... more) {

        String path = first;
        if (more.length > 0) {
            path += getSeparator() + String.join(getSeparator(), more);
        }
        return new HttpPath(this, path, null);

    }


    @Override
    public PathMatcher getPathMatcher(final String syntaxAndPattern) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public WatchService newWatchService() throws IOException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public String toString() {

        return String.format("%s[%s]@%s", this.getClass().getSimpleName(), provider, hashCode());

    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpFileSystem that = (HttpFileSystem) o;
        return url.equals(that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }

    /**
     * @return return if we check the access (readability, existence) with the `HEAD` method
     */
    public boolean shouldCheckAccess() {
        /*
         * No, by default because `HEAD` has a side effect in the fact that it may be
         * just not authorized (405) while a `GET` will
         *
         */
        return false;
    }

    public String getWorkingStringPath() {
        String path = this.url.getPath();
        if (path.equals("")) {
            return this.getRootCharacter();
        }
        return path;
    }

    public URL getConnectionUrl() {
        return this.url;
    }

    public String getRootCharacter() {
        return this.getSeparator();
    }

    /**
     * A http system defines the address with a path and the query property
     * (ie the uri)
     */
    public HttpPath getPath(URI uri) {
        return new HttpPath(this, uri.getPath(), uri.getQuery());
    }

  public String getPassword() {
    return this.password;
  }

  public String getUser() {
    return this.user;
  }
}
