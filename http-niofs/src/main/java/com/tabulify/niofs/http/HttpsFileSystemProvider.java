package com.tabulify.niofs.http;

/**
 *
 */
public final class HttpsFileSystemProvider extends HttpFileSystemProvider {

    /** Scheme for HTTPS files. */
    public static final String SCHEME = "https";

    /**
     * {@inheritDoc}
     *
     * @return {@link #SCHEME}.
     */
    @Override
    public final String getScheme() {
        return SCHEME;
    }

}
