package net.bytle.api.http;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;

public class Https {

  /**
   *
   * @param request
   * @return the IP address of the real client (browser), not the proxy
   */
  public static String getRealRemoteClient(HttpServerRequest request) {

    // remoteClient is the ip of the direct connection (ie a proxy or the real client)
    String remoteClient = getClientAddress(request.remoteAddress());

    // X-Real-IP headers is checked
    final MultiMap headers = request.headers();
    return headers.contains("X-Real-IP") ? headers.get("X-Real-IP") : remoteClient;

  }

  private static String getClientAddress(SocketAddress inetSocketAddress) {
    if (inetSocketAddress == null) {
      return null;
    }
    return inetSocketAddress.host();
  }

}
