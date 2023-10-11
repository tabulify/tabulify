package net.bytle.monitor;


import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.time.Instant;

public class MonitorApiTokenCloudflare {


  public String id;
  private String name;
  private String status;
  private Instant expirationDate;
  private JsonObject tokenJsonData;

  public static MonitorApiTokenCloudflare createFromJson(JsonObject tokenJsonData) {
    MonitorApiTokenCloudflare apiToken = new MonitorApiTokenCloudflare();
    apiToken.id = tokenJsonData.getString("id");
    apiToken.name = tokenJsonData.getString("name");
    apiToken.status = tokenJsonData.getString("status");
    apiToken.expirationDate = tokenJsonData.getInstant("expires_on");
    apiToken.tokenJsonData = tokenJsonData;

    return apiToken;
  }

  public boolean isActive() {
    return status.equals("active");
  }

  public Instant getExpirationDate() {

    return expirationDate;
  }

  @Override
  public String toString() {
    return name;
  }

  public boolean shouldBeIpRestricted() {
    JsonArray policies = tokenJsonData.getJsonArray("policies");
    if (policies == null) {
      return false;
    }
    for (int iPolicy = 0; iPolicy < policies.size(); iPolicy++) {
      JsonObject policy = policies.getJsonObject(iPolicy);

      JsonObject resources = policy.getJsonObject("resources");
      if (resources == null) {
        return false;
      }

      for (String resource : resources.getMap().keySet()) {
        if (resource.startsWith("com.cloudflare.api.account.zone")) {
          JsonArray permissionGroups = policy.getJsonArray("permission_groups");
          if (permissionGroups == null) {
            return false;
          }
          for (int i = 0; i < permissionGroups.size(); i++) {
            String permissionName = permissionGroups.getJsonObject(i).getString("name");
            if (permissionName.endsWith("Write")) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  public void checkIpRestrictionOn(String expectedIpAddress) throws MonitorException {
    JsonObject condition = tokenJsonData.getJsonObject("condition");
    if (condition == null) {
      throw new MonitorException("There is no condition on the token");
    }
    JsonObject requestIp = condition.getJsonObject("request.ip");
    if (requestIp == null) {
      throw new MonitorException("There is no request ip on the token");
    }
    JsonArray inIps = requestIp.getJsonArray("in");
    for (int i = 0; i < inIps.size(); i++) {
      String ip = inIps.getString(i);
      if (ip == null) {
        throw new MonitorException("The ip value is null on the token");
      }
      if (!ip.equals(expectedIpAddress)) {
        throw new MonitorException("The ip restricted value (" + ip + ") is not the expected one " + expectedIpAddress);
      }
    }
  }
}
