package net.bytle.vertx;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * The geo-localization information based on Ip
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IpInfo   {

  private String ip;
  private String country;
  private String country2;
  private String country3;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public IpInfo () {
  }

  /**
  * @return ip The ip, the data is based on
  */
  @JsonProperty("ip")
  public String getIp() {
    return ip;
  }

  /**
  * @param ip The ip, the data is based on
  */
  @SuppressWarnings("unused")
  public void setIp(String ip) {
    this.ip = ip;
  }

  /**
  * @return country The country name
  */
  @JsonProperty("country")
  public String getCountry() {
    return country;
  }

  /**
  * @param country The country name
  */
  @SuppressWarnings("unused")
  public void setCountry(String country) {
    this.country = country;
  }

  /**
  * @return country2 The country ISO code on two characters
  */
  @JsonProperty("country2")
  public String getCountry2() {
    return country2;
  }

  /**
  * @param country2 The country ISO code on two characters
  */
  @SuppressWarnings("unused")
  public void setCountry2(String country2) {
    this.country2 = country2;
  }

  /**
  * @return country3 The country ISO code on three characters
  */
  @JsonProperty("country3")
  public String getCountry3() {
    return country3;
  }

  /**
  * @param country3 The country ISO code on three characters
  */
  @SuppressWarnings("unused")
  public void setCountry3(String country3) {
    this.country3 = country3;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IpInfo ipInfo = (IpInfo) o;
    return Objects.equals(ip, ipInfo.ip) &&
        Objects.equals(country, ipInfo.country) &&
        Objects.equals(country2, ipInfo.country2) &&
        Objects.equals(country3, ipInfo.country3);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ip, country, country2, country3);
  }

  @Override
  public String toString() {
    return "class IpInfo {\n" +
    "}";
  }

}
