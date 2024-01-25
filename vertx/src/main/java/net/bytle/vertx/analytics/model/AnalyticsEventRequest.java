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


  protected String agentGuid;

  protected String flowGuid;

  protected String flowHandle;

  protected String remoteIp;

  protected String sessionId;

  protected URI originUri;

  protected URI referrerUri;

  /**
  * The empty constructor is
  * needed for the construction of the pojo
  * with the Jackson library
  */
  @SuppressWarnings("unused")
  public AnalyticsEventRequest () {
  }

  /**
  * @return agentGuid The agent id The Analytics Agent is an object that has all properties
  */
  @JsonProperty("agentGuid")
  public String getAgentGuid() {
    return agentGuid;
  }

  /**
  * @param agentGuid The agent id The Analytics Agent is an object that has all properties
  */
  @SuppressWarnings("unused")
  public void setAgentGuid(String agentGuid) {
    this.agentGuid = agentGuid;
  }

  /**
  * @return flowGuid The flow id is the immutable id of the flow A flow is also known as a navigation flow or process that leads to a event creation
  */
  @JsonProperty("flowGuid")
  public String getFlowGuid() {
    return flowGuid;
  }

  /**
  * @param flowGuid The flow id is the immutable id of the flow A flow is also known as a navigation flow or process that leads to a event creation
  */
  @SuppressWarnings("unused")
  public void setFlowGuid(String flowGuid) {
    this.flowGuid = flowGuid;
  }

  /**
  * @return flowHandle The flow handle is the human identifier for the flow (rarely mutable)
  */
  @JsonProperty("flowHandle")
  public String getFlowHandle() {
    return flowHandle;
  }

  /**
  * @param flowHandle The flow handle is the human identifier for the flow (rarely mutable)
  */
  @SuppressWarnings("unused")
  public void setFlowHandle(String flowHandle) {
    this.flowHandle = flowHandle;
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

  /**
  * @return referrerUri The http `referrer` header or javascript `document.referrer` Note that Google Analytics will use \"utm_referrer\" over \"document.referrer\" if set as document.referrer is only the domain/authority part
  */
  @JsonProperty("referrerUri")
  public URI getReferrerUri() {
    return referrerUri;
  }

  /**
  * @param referrerUri The http `referrer` header or javascript `document.referrer` Note that Google Analytics will use \"utm_referrer\" over \"document.referrer\" if set as document.referrer is only the domain/authority part
  */
  @SuppressWarnings("unused")
  public void setReferrerUri(URI referrerUri) {
    this.referrerUri = referrerUri;
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

            Objects.equals(agentGuid, analyticsEventRequest.agentGuid) && Objects.equals(flowGuid, analyticsEventRequest.flowGuid) && Objects.equals(flowHandle, analyticsEventRequest.flowHandle) && Objects.equals(remoteIp, analyticsEventRequest.remoteIp) && Objects.equals(sessionId, analyticsEventRequest.sessionId) && Objects.equals(originUri, analyticsEventRequest.originUri) && Objects.equals(referrerUri, analyticsEventRequest.referrerUri);
  }

  @Override
  public int hashCode() {
    return Objects.hash(agentGuid, flowGuid, flowHandle, remoteIp, sessionId, originUri, referrerUri);
  }

  @Override
  public String toString() {
    return super.toString();
  }

}
