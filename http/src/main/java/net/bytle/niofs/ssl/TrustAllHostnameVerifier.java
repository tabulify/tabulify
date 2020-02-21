package net.bytle.niofs.ssl;

import javax.net.ssl.HostnameVerifier;


/**
 *
 *  This class implements a hostname verifier that will accept all of them
 *
 */
public class TrustAllHostnameVerifier implements HostnameVerifier {

  /**
   * Always return true, indicating that the host name is
   * an acceptable match with the server's authentication scheme.
   *
   * @param hostname        the host name.
   * @param session         the SSL session used on the connection to
   * host.
   * @return                the true boolean value
   * indicating the host name is trusted.
   */
  public boolean verify(String hostname, javax.net.ssl.SSLSession session) {
    return(true);
  }

}
