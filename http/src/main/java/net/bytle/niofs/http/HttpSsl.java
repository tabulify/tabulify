package net.bytle.niofs.http;

import net.bytle.niofs.ssl.Ssls;
import net.bytle.niofs.ssl.TrustAllHostnameVerifier;

import javax.net.ssl.HttpsURLConnection;

/**
 * This class provide various static methods that relax X509 certificate and
 * hostname verification while using the SSL over the HTTP protocol.
 *
 * Doc
 * https://docs.oracle.com/javase/7/docs/api/javax/net/ssl/HttpsURLConnection.html
 *
 * Inspired by:
 * https://en.wikibooks.org/wiki/WebObjects/Web_Services/How_to_Trust_Any_SSL_Certificate
 */
final class HttpSsl {


  /**
   * Set the default Hostname Verifier to an instance of a fake class that
   * trust all hostnames.
   */
  private static void setDefaultToTrustAllHostNames() {

    HttpsURLConnection.setDefaultHostnameVerifier(new TrustAllHostnameVerifier());

  }

  /**
   * Set the default X509 Trust Manager to an instance of a fake class that
   * trust all certificates, even the self-signed ones.
   */
  private static void setDefaultToTrustAllCertificates() {

    HttpsURLConnection.setDefaultSSLSocketFactory(Ssls.getTrustAllCertificateSocketFactory());

  }




}
