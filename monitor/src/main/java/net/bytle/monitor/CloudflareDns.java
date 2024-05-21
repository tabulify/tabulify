package net.bytle.monitor;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bytle.dns.DnsException;
import net.bytle.dns.DnsNotFoundException;

import java.util.HashMap;
import java.util.Map;


public class CloudflareDns {


  private final CloudflareApi cloudflareApi;

  Map<String, CloudflareDnsZone> cloudflareDnsZoneMap = new HashMap<>();

  public CloudflareDns(CloudflareApi cloudflareApi) {
    this.cloudflareApi = cloudflareApi;

  }

  public static CloudflareDns create(CloudflareApi cloudflareApi) {
    return new CloudflareDns(cloudflareApi);
  }


  /**
   * Zone is the unit of storage
   */
  Future<CloudflareDnsZone> getZone(String zoneName) {
    // https://developers.cloudflare.com/api/operations/zones-get
    CloudflareDnsZone zone = cloudflareDnsZoneMap.get(zoneName);
    if (zone != null) {
      return Future.succeededFuture(zone);
    }
    return this.cloudflareApi
      .getRequest("https://api.cloudflare.com/client/v4/zones")
      .addQueryParam("name", zoneName)
      .send()
      .compose(response -> {

        String body = response.bodyAsString();
        JsonObject jsonBody;
        try {
          jsonBody = new JsonObject(body);
        } catch (Exception e) {
          return Future.failedFuture(new IllegalStateException("Content is not Json\n" + body, e));
        }

        CloudflareDnsZone.CloudflareDnsZoneConfig result = CloudflareDnsZone.config(this, zoneName);

        Boolean success = jsonBody.getBoolean("success");
        if (!success) {
          JsonArray errors = jsonBody.getJsonArray("errors");
          if (!errors.isEmpty()) {
            JsonObject error = errors.getJsonObject(0);
            Integer code = error.getInteger("code");
            String message = error.getString("message");
            return Future.failedFuture(new InternalError("CloudFlare Request Error: " + code + " - " + message));
          }

        }
        JsonArray jsonArray = jsonBody.getJsonArray("result");
        switch (jsonArray.size()) {
          case 0:
            return Future.failedFuture(new DnsNotFoundException("No zone with the name (" + zoneName + ")"));
          case 1:
            JsonObject tokenJsonData = jsonArray.getJsonObject(0);
            result.setId(tokenJsonData.getString("id"));
            CloudflareDnsZone futureZoneResult = result.build();
            this.cloudflareDnsZoneMap.put(zoneName, futureZoneResult);
            return Future.succeededFuture(futureZoneResult);
          default:
            return Future.failedFuture(new DnsException("Too much zones for the name (" + zoneName + ")"));
        }

      }, err -> Future.failedFuture(new DnsException("Error while querying the zone", err)));

  }

  public CloudflareApi getCloudflareClient() {
    return this.cloudflareApi;
  }


}
