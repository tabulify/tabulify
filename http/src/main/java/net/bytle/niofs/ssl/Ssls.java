package net.bytle.niofs.ssl;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

public class Ssls {

  public static SSLSocketFactory getTrustAllCertificateSocketFactory() {
    SSLContext context;

    try {
      context = SSLContext.getInstance("SSL");
      context.init(null, new TrustManager[]{new TrustAllCertificateManager()}, new SecureRandom());
    } catch (GeneralSecurityException gse) {
      throw new IllegalStateException(gse.getMessage());
    }
    return context.getSocketFactory();

  }
}
