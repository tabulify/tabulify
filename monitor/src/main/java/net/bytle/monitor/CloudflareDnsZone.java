package net.bytle.monitor;

public class CloudflareDnsZone {


  private final CloudflareDnsZoneConfig config;

  public CloudflareDnsZone(CloudflareDnsZoneConfig cloudflareDnsZoneConfig) {
    this.config = cloudflareDnsZoneConfig;
  }

  public static CloudflareDnsZoneConfig config(CloudflareDns cloudflareDns, String zoneName) {
    return new CloudflareDnsZoneConfig(cloudflareDns, zoneName);
  }

  public CloudflareDns getCloudflareDns() {
    return this.config.cloudflareDns;
  }

  public String getId() {
    return this.config.id;
  }

  public CloudflareDnsName getName(String name) {
    return CloudflareDnsName.create(this,name);
  }


  public static class CloudflareDnsZoneConfig {
    private final CloudflareDns cloudflareDns;
    private final String zoneName;
    private String id;

    public CloudflareDnsZoneConfig(CloudflareDns cloudflareDns, String zoneName) {
      this.cloudflareDns = cloudflareDns;
      this.zoneName = zoneName;
    }

    public CloudflareDnsZone build() {
      return new CloudflareDnsZone(this);
    }

    public CloudflareDnsZoneConfig setId(String id) {
      this.id = id;
      return this;
    }
  }

  @Override
  public String toString() {
    return config.zoneName;
  }

}
