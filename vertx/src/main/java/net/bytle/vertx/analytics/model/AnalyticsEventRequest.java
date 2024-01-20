package net.bytle.vertx.analytics.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;
import java.util.Objects;

/**
 * The request/remote identifiers of an event
 **/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsEventRequest   {


  protected String agentId;

  protected String remoteIp;

  protected String sessionId;

  protected URI originUri;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public AnalyticsEventRequest () {
  }

  /**
  * @return agentId The agent id
  */
  @JsonProperty("agentId")
  public String getAgentId() {
    return agentId;
  }

  /**
  * @param agentId The agent id
  */
  @SuppressWarnings("unused")
  public void setAgentId(String agentId) {
    this.agentId = agentId;
  }

  /**
  * @return remoteIp The ip (the remote client ip)
  */
  @JsonProperty("remoteIp")
  public String getRemoteIp() {
    return remoteIp;
  }

  /**
  * @param remoteIp The ip (the remote client ip)
  */
  @SuppressWarnings("unused")
  public void setRemoteIp(String remoteIp) {
    this.remoteIp = remoteIp;
  }

  /**
  * @return sessionId The session id (a nonce)
  */
  @JsonProperty("sessionId")
  public String getSessionId() {
    return sessionId;
  }

  /**
  * @param sessionId The session id (a nonce)
  */
  @SuppressWarnings("unused")
  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  /**
  * @return originUri The origin uri (from where the request originate inside the app) For a browser, the uri in the address bar or a iframe uri For a gui app, an uri that represents a page
  */
  @JsonProperty("originUri")
  public URI getOriginUri() {
    return originUri;
  }

  /**
  * @param originUri The origin uri (from where the request originate inside the app) For a browser, the uri in the address bar or a iframe uri For a gui app, an uri that represents a page
  */
  @SuppressWarnings("unused")
  public void setOriginUri(URI originUri) {
    this.originUri = originUri;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AnalyticsEventRequest analyticsEventRequest = (AnalyticsEventRequest) o;
    return

            Objects.equals(agentId, analyticsEventRequest.agentId) && Objects.equals(remoteIp, analyticsEventRequest.remoteIp) && Objects.equals(sessionId, analyticsEventRequest.sessionId) && Objects.equals(originUri, analyticsEventRequest.originUri);
  }

  @Override
  public int hashCode() {
    return Objects.hash(agentId, remoteIp, sessionId, originUri);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
