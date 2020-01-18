package net.bytle.api.http;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import net.bytle.api.DropWizard;

public class Https {

  public static final String CACHE_CONTROL = "Cache-Control";
  public static final String CACHE_CONTROL_NO_STORE = "no-store";
  public final static String X_FORWARDED_FOR = "X-Forwarded-For";
  public static final String X_REAL_IP = "X-Real-IP";

  /**
   * @param request
   * @return the IP address of the real client (browser), not the proxy - may be null
   */
  public static String getRealRemoteClient(HttpServerRequest request) {

    final MultiMap headers = request.headers();

    String xForwardedFor = headers.get(X_FORWARDED_FOR);
    if (xForwardedFor != null) {
      return Https.getRemoteIpFromXForwardedFor(xForwardedFor);
    }
    String xRealIP = headers.get(X_REAL_IP);
    if (xRealIP != null) {
      return xRealIP;
    }

    // remoteClient is the ip of the direct connection (ie a proxy or the real client)
    SocketAddress inetSocketAddress = request.remoteAddress();
    if (inetSocketAddress == null) {
      return null;
    } else {
      return inetSocketAddress.host();
    }

  }


  /**
   * @param xForwardedFor - The content of a X-Forwarded-For header
   * @return the remote ip (not the proxy ip)
   */
  public static String getRemoteIpFromXForwardedFor(String xForwardedFor) {
    assert xForwardedFor != null;
    String[] xForwardedForParts = xForwardedFor.split(",");
    return xForwardedForParts[0];
  }

  public static Vertx getVertx() {
    VertxOptions vertxOptions = new VertxOptions();
    vertxOptions.setMetricsOptions(DropWizard.getMetricsOptions());
    return Vertx.vertx(vertxOptions);
  }

}
