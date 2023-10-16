package net.bytle.monitor;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bytle.dns.DnsException;
import org.xbill.DNS.Address;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class CloudflareDnsName {


  private final CloudflareDnsZone zone;
  private final String dnsName;

  public CloudflareDnsName(CloudflareDnsZone zone, String dnsName) {
    this.zone = zone;
    this.dnsName = dnsName;
  }

  public static CloudflareDnsName create(CloudflareDnsZone zone, String dnsName) {
    return new CloudflareDnsName(zone, dnsName);
  }

  public Future<InetAddress> getFirstARecordOrNull() {
    return this.zone
      .getCloudflareDns()
      .getCloudflareClient()
      .getRequest("https://api.cloudflare.com/client/v4/zones/"+this.zone.getId()+"/dns_records")
      .addQueryParam("name", dnsName)
      .addQueryParam("type", "A")
      .send()
      .compose(response -> {

        String body = response.bodyAsString();
        JsonObject jsonBody;
        try {
          jsonBody = new JsonObject(body);
        } catch (Exception e) {
          return Future.failedFuture(new IllegalStateException("Content is not Json\n" + body, e));
        }

        JsonArray jsonArray = jsonBody.getJsonArray("result");
        if (jsonArray.size() == 0) {
          return Future.succeededFuture(null);
        }
        /**
         * See doc:
         * https://developers.cloudflare.com/api/operations/dns-records-for-a-zone-list-dns-records
         */
        JsonObject dnsRecordJsonData = jsonArray.getJsonObject(0);
        String content = dnsRecordJsonData.getString("content");
        try {
          return Future.succeededFuture(Address.getByAddress(content, Address.IPv4));
        } catch (UnknownHostException e) {
          return Future.failedFuture(new DnsException(content + " is not an ipv4 address", e));
        }

      });
  }

}
