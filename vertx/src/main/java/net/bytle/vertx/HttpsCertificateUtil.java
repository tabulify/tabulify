package net.bytle.vertx;

import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import net.bytle.java.JavaEnvs;

/**
 * The HTTPS and certificate util class to control HTTPS
 * See the https.md documentation for more info.
 */
public class HttpsCertificateUtil {


  public static final String DEV_KEY_PEM = "./cert/key.pem";
  public static final String DEV_CERT_PEM = "./cert/cert.pem";
  private static HttpsCertificateUtil httpCertificate;


  /**
   * @return the scheme always secure
   */
  public String getHttpScheme() {
    if (isHttpsEnable()) return "https";
    return "http";
  }

  public void enableServerHttps(HttpServerOptions options) {
    /**
     * https://vertx.io/docs/apidocs/io/vertx/core/net/PemKeyCertOptions.html
     */
    if (JavaEnvs.IS_DEV) {
      options.setPemKeyCertOptions(
          new PemKeyCertOptions().addKeyPath(HttpsCertificateUtil.DEV_KEY_PEM).addCertPath(HttpsCertificateUtil.DEV_CERT_PEM)
        )
        .setSsl(true);
    }
  }

  public static HttpsCertificateUtil createOrGet() {
    if (httpCertificate == null) {
      httpCertificate = new HttpsCertificateUtil();
    }
    return httpCertificate;
  }

  /**
   * @return if https is enabled on the system
   */
  public boolean isHttpsEnable() {
    /**
     * Chrome does not allow to set a third-party cookie (ie same site: None)
     * if the connection is not secure.
     * It must be true then everywhere.
     */
    return true;
  }

  /**
   * @param httpClientOptions the http client options
   */
  public void enableClientHttps(HttpClientOptions httpClientOptions) {
    /**
     * If you want to remove the `trustAll=true`, you need
     * to add the certificate on the OS and not using the {@link HttpClientOptions#setPemTrustOptions(PemTrustOptions)}
     * otherwise it will trust only this Root certificate.
     * <p>
     * See: https://groups.google.com/g/vertx/c/NYLcHzY8EYM
     */
    boolean httpsEnable = isHttpsEnable();
    if (httpsEnable) {
      httpClientOptions
        .setSsl(true)
        .setTrustAll(true);
    }
  }
}
