package net.bytle.vertx.analytics;

import com.fasterxml.uuid.Generators;
import com.mixpanel.mixpanelapi.ClientDelivery;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.java.JavaEnvs;
import net.bytle.type.KeyNameNormalizer;
import net.bytle.vertx.*;
import net.bytle.vertx.analytics.model.*;
import net.bytle.vertx.auth.AuthUser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Managed the tracking with:
 * * {@link AnalyticsEvent Events}. The things that happen in your product.
 * * {@link AnalyticsUser} The people who use your product.
 * * Company: Organizations that use your product.
 * * Traits are the properties of your users and companies (query filter and agg)
 */
public class AnalyticsTracker {

  static Logger LOGGER = LogManager.getLogger(AnalyticsTracker.class);

  private final HTreeMap<String, AnalyticsEvent> eventsQueue;

  private final MapDb mapDb;
  private boolean logEventDelivery = false;
  private final AnalyticsMixPanel analyticsMixPanel;

  public AnalyticsTracker(Server server) throws ConfigIllegalException {


    this.analyticsMixPanel = new AnalyticsMixPanel(server);

    this.mapDb = server.getMapDb();
    this.eventsQueue = mapDb
      .hashMapWithJsonValueObject("event_queue", Serializer.STRING, AnalyticsEvent.class)
      .createOrOpen();

    int sec10 = 10000;
    server.getVertx().setPeriodic(sec10, sec10, jobId -> sendEventsInQueue());


    if (JavaEnvs.IS_DEV) {
      this.logEventDelivery = true;
    }

  }

  public static AnalyticsTracker createFromServer(Server server) throws ConfigIllegalException {

    return new AnalyticsTracker(server);

  }

  public AnalyticsTracker addEventToQueue(AnalyticsEvent analyticsEvent) {
    this.eventsQueue.put(analyticsEvent.getId(), analyticsEvent);
    this.mapDb.commit();
    return this;
  }

  /**
   * @param user - the user
   * @param ip   - the ip when the user was created for geo-localization
   * @return the analytics tracker
   * Segment recommends that you make an Identify call:
   * <p>
   * * After a user first registers
   * * After a user logs in
   * * When a user updates their info (for example, they change or add a new address)
   */
  @SuppressWarnings("unused")
  public AnalyticsTracker deliverUser(AnalyticsUser user, String ip) {
    //noinspection unused
    JsonObject props = this.analyticsMixPanel.toMixPanelUser(user, ip);
    return this;
  }

  /**
   * GROUP_ID = Organisation
   * <a href="https://www.june.so/docs/quickstart/identify#companies">Company</a>
   */
  @SuppressWarnings("unused")
  public AnalyticsTracker deliverOrganization() {
    return this;
  }


  public AnalyticsTracker sendEventsInQueue() {

    if (this.eventsQueue.isEmpty()) {
      return this;
    }

    // Gather together a bunch of messages into a single
    // ClientDelivery. This can happen in a separate thread
    // or process from the call to MessageBuilder.event()
    ClientDelivery delivery = new ClientDelivery();

    /**
     * The error of the API returns the id of the inserted
     * message.
     * https://developer.mixpanel.com/reference/import-events#example-of-a-validation-error
     * Therefore, we create a list
     */
    List<AnalyticsEvent> eventInBatch = new ArrayList<>();
    List<AnalyticsEvent> badEvents = new ArrayList<>();
    for (AnalyticsEvent event : this.eventsQueue.values()) {

      try {

        event.getState().setSendingTime(DateTimeUtil.getNowInUtc());

        /**
         * It should never happen but this
         * is enough to get a difficult error
         * to debug from MixPanel
         */
        if (event.getName() == null) {
          badEvents.add(event);
          continue;
        }

        // Create an event
        // https://docs.mixpanel.com/docs/tracking/how-tos/identifying-users#what-is-distinct-id
        // https://docs.mixpanel.com/docs/tracking/reference/distinct-id-limits
        // The purpose of distinct_id is to provide a single, unified identifier for a user across devices and sessions.
        // This helps Mixpanel compute metrics like Daily Active Users accurately: when two events have the same value of distinct_id, they are considered as being performed by 1 unique user.
        JSONObject mixpanelEvent = this.analyticsMixPanel.buildEvent(event);
        if (!delivery.isValidMessage(mixpanelEvent)) {
          continue;
        }
        delivery.addMessage(mixpanelEvent);
        if (this.logEventDelivery) {
          LOGGER.info("The event " + event + " was added for delivery");
        }
        eventInBatch.add(event);
      } catch (Exception e) {
        this.handleFatalError(e, event);
      }
    }

    /**
     * Process bad event
     * (Should be stored elsewhere)
     */
    for (AnalyticsEvent event : badEvents) {
      LOGGER.error("The event was deleted (" + event.getName() + "," + event.getId() + ")");
      this.eventsQueue.remove(event.getId());
    }

    try {
      // https://developer.mixpanel.com/reference/import-events
      this.analyticsMixPanel.deliver(delivery);
    } catch (IOException e) {
      // Example: Here it's `Can't communicate with Mixpanel`
      // https://github.com/mixpanel/mixpanel-java/blob/master/src/demo/java/com/mixpanel/mixpanelapi/demo/MixpanelAPIDemo.java#L57C45-L57C77
      // Do we get the error?
      // https://developer.mixpanel.com/reference/import-events#example-of-a-validation-error
      // See impl: https://github.com/mixpanel/mixpanel-java/blob/master/src/main/java/com/mixpanel/mixpanelapi/MixpanelAPI.java#L195
      return this;
    }
    for (AnalyticsEvent event : eventInBatch) {
      try {
        AnalyticsLogger.log(event);
      } catch (Exception e) {
        this.handleFatalError(e, event);
      }
      this.eventsQueue.remove(event.getId());
    }
    this.mapDb.commit();
    return this;
  }

  /**
   * Handle fatal error
   * <p>
   * A fatal error may be solved when retried (ie timeout for instance)
   * but only a maximum
   *
   */
  private void handleFatalError(Exception e, AnalyticsEvent event) {
    LOGGER.error("The event (" + event.getName() + ") could not be sent", e);
    // we remove the event otherwise it will repeat
    this.eventsQueue.remove(event.getId());
  }

  /**
   * @param analyticsServerEvent - an internal server event
   */
  public EventBuilder eventBuilder(AnalyticsServerEvent analyticsServerEvent) {
    AnalyticsEvent analyticsEvent = new AnalyticsEvent();

    Map<String, Object> jsonObjectMap = JsonObject.mapFrom(analyticsServerEvent).getMap();
    String eventNameKey = "name";
    String name = (String) jsonObjectMap.get(eventNameKey);
    if (name == null) {
      throw new InternalException("The event name is null but is mandatory");
    }
    analyticsEvent.setName(name);
    jsonObjectMap.remove(eventNameKey);

    /**
     * No null or blank value
     */
    Map<String, Object> jsonObjectMapTarget = jsonObjectMap
      .entrySet()
      .stream()
      .filter(e -> e.getValue() != null && !e.getValue().toString().isBlank())
      .collect(Collectors.toMap(
        Map.Entry::getKey,
        Map.Entry::getValue
      ));

    analyticsEvent.setAttr(jsonObjectMapTarget);
    return new EventBuilder(analyticsEvent);

  }


  /**
   * @param externalAnalyticsEvent - an external analytics received from the API
   */
  public EventBuilder eventBuilder(AnalyticsEvent externalAnalyticsEvent) {

    return new EventBuilder(externalAnalyticsEvent);

  }

  public class EventBuilder {

    private AuthUser authUser;

    AnalyticsEvent analyticsEvent;

    /**
     * The http routing context
     * Event may be inserted outside an HTTP call
     * The context may be therefore null
     */
    private RoutingContext routingContext;


    private String organizationId;
    private String realmId;

    private EventBuilder(AnalyticsEvent analyticsEvent) {
      this.analyticsEvent = analyticsEvent;
    }


    /**
     * Send the event to the queue
     * (the event is processed async)
     */
    public void addEventToQueue() {

      AnalyticsEvent analyticsEvent = buildEvent();
      AnalyticsTracker.this.addEventToQueue(analyticsEvent);

    }

    public EventBuilder setUser(AuthUser authUser) {
      this.authUser = authUser;
      return this;
    }

    public EventBuilder setRoutingContext(RoutingContext routingContext) {
      this.routingContext = routingContext;
      return this;
    }

    public AnalyticsEvent buildEvent() {

      /**
       * Normalize event name
       */
      String name = this.analyticsEvent.getName();
      if (name == null) {
        throw new InternalException("An event should have a name. The event (" + analyticsEvent.getClass().getSimpleName() + ") has no name");
      }
      KeyNameNormalizer eventName = KeyNameNormalizer.createFromString(name);
      analyticsEvent.setName(eventName.toEventCase());

      /**
       * Create the event sub-objects if absent
       */
      AnalyticsEventUser analyticsEventUser = analyticsEvent.getUser();
      if (analyticsEventUser == null) {
        analyticsEventUser = new AnalyticsEventUser();
        analyticsEvent.setUser(analyticsEventUser);
      }
      AnalyticsEventApp analyticsEventApp = analyticsEvent.getApp();
      if (analyticsEventApp == null) {
        analyticsEventApp = new AnalyticsEventApp();
        analyticsEvent.setApp(analyticsEventApp);
      }
      AnalyticsEventState analyticsEventState = analyticsEvent.getState();
      if (analyticsEventState == null) {
        analyticsEventState = new AnalyticsEventState();
        analyticsEvent.setState(analyticsEventState);
      }
      AnalyticsEventRequest analyticsEventRequest = analyticsEvent.getRequest();
      if (analyticsEventRequest == null) {
        analyticsEventRequest = new AnalyticsEventRequest();
        analyticsEvent.setRequest(analyticsEventRequest);
      }
      AnalyticsEventUtm analyticsEventUtm = analyticsEvent.getUtm();
      if (analyticsEventUtm == null) {
        analyticsEventUtm = new AnalyticsEventUtm();
        analyticsEvent.setUtm(analyticsEventUtm);
      }


      /**
       * Add user data
       */
      if (this.authUser != null) {

        analyticsEventUser.setUserId(authUser.getSubject());
        analyticsEventUser.setUserEmail(authUser.getSubjectEmail());

        /**
         * App data if any
         */
        analyticsEventApp.setAppRealmId(authUser.getAudience());
        analyticsEventApp.setAppOrganisationId(authUser.getGroup());
      }

      /**
       * App data
       */
      if (this.realmId != null) {
        analyticsEventApp.setAppRealmId(this.realmId);
      }
      if (this.organizationId != null) {
        analyticsEventApp.setAppOrganisationId(this.realmId);
      }

      /**
       * Request
       */
      if (routingContext != null) {


        HttpServerRequest request = routingContext.request();
        RoutingContextWrapper routingContextWrapper = new RoutingContextWrapper(routingContext);
        try {
          analyticsEventRequest.setRemoteIp(HttpRequestUtil.getRealRemoteClientIp(request));
        } catch (NotFoundException e) {
          //
        }
        Session session = routingContext.session();
        if (session != null) {
          analyticsEventRequest.setSessionId(session.id());
        }
        try {
          URI requestReferrer = routingContextWrapper.getReferer();
          analyticsEventRequest.setReferrerUri(requestReferrer);
        } catch (NotFoundException | IllegalStructure e) {
          // bad data
        }

        /**
         * UTM
         */
        // &utm_campaign=SavedSearches#distanceMeters:9000|searchInTitleAndDescription:true|Language:nl-NL|offeredSince:1705597578000|asSavedSearch:true
        String utmCampaign = request.getParam("utm_campaign");
        if (utmCampaign != null) {
          analyticsEventUtm.setUtmCampaign(utmCampaign);
        }
        String utmCampaignId = request.getParam("utm_campaign_id");
        if (utmCampaignId != null) {
          analyticsEventUtm.setUtmCampaignId(utmCampaign);
        }
        // &utm_source=systemmail
        String utmSource = request.getParam("utm_source");
        if (utmSource != null) {
          analyticsEventUtm.setUtmSource(utmSource);
        }
        // &utm_medium=email
        String utmMedium = request.getParam("utm_medium");
        if (utmMedium != null) {
          analyticsEventUtm.setUtmMedium(utmMedium);
        }
        // example: utm_content=button
        String utmContent = request.getParam("utm_content");
        if (utmContent != null) {
          analyticsEventUtm.setUtmContent(utmContent);
        }
        String utmTerm = request.getParam("utm_term");
        if (utmTerm != null) {
          analyticsEventUtm.setUtmTerm(utmTerm);
        }
        String utmReferrer = request.getParam("utm_referrer");
        if (utmReferrer != null) {
          try {
            analyticsEventUtm.setUtmReferrer(URI.create(utmReferrer));
          } catch (Exception e) {
            // bad data
          }
        }

      }


      /**
       * Creation time and uuid time part should be the same
       * to allow retrieval on event id
       * with data partition
       * (ie extract time from uuid, select on creation time
       * to partition)
       */
      LocalDateTime creationTime = analyticsEventState.getCreationTime();
      if (creationTime == null) {
        creationTime = DateTimeUtil.getNowInUtc();
        analyticsEventState.setCreationTime(creationTime);
      }
      if (analyticsEvent.getId() == null) {

        long timestamp = creationTime.toEpochSecond(ZoneOffset.UTC);
        UUID uuid = Generators.timeBasedEpochGenerator().construct(timestamp);
        analyticsEvent.setId(uuid.toString());

      }

      return analyticsEvent;
    }


    public EventBuilder setOrganizationId(String organizationId) {
      this.organizationId = organizationId;
      return this;
    }

    public EventBuilder setRealmId(String realmId) {
      this.realmId = realmId;
      return this;
    }
  }

}
