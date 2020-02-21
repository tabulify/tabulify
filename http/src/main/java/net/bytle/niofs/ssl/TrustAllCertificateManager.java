package net.bytle.niofs.ssl;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;


/**
 * This class allow any X509 certificates to be used to authenticate the
 * remote side of a secure socket, including self-signed certificates.
 * https://en.wikibooks.org/wiki/WebObjects/Web_Services/How_to_Trust_Any_SSL_Certificate
 */
public class TrustAllCertificateManager implements X509TrustManager {

  /**
   * Empty array of certificate authority certificates.
   */
  private static final X509Certificate[] _AcceptedIssuers = new X509Certificate[] {};


  /**
   * Always trust for client SSL chain peer certificate
   * chain with any authType authentication types.
   *
   * @param chain           the peer certificate chain.
   * @param authType        the authentication type based on the client
   * certificate.
   */
  public void checkClientTrusted(X509Certificate[] chain, String authType) {
    // Nihil
  }

  /**
   * Always trust for server SSL chain peer certificate
   * chain with any authType exchange algorithm types.
   *
   * @param chain           the peer certificate chain.
   * @param authType        the key exchange algorithm used.
   */
  public void checkServerTrusted(X509Certificate[] chain, String authType) {
    // Nihil
  }

  /**
   * Return an empty array of certificate authority certificates which
   * are trusted for authenticating peers.
   *
   * @return                a empty array of issuer certificates.
   */
  public X509Certificate[] getAcceptedIssuers() {
    return(_AcceptedIssuers);
  }

}
