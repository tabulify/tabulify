package net.bytle.vertx.analytics;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.uuid.Generators;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.type.KeyCase;
import net.bytle.type.KeyNormalizer;
import net.bytle.vertx.*;
import net.bytle.vertx.analytics.event.AnalyticsServerEvent;
import net.bytle.vertx.analytics.model.*;
import net.bytle.vertx.auth.AuthUser;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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


  private final AnalyticsDelivery analyticsDelivery;
  private final JsonMapper jacksonMapperThatAllowEmptyBean;

  public AnalyticsTracker(Server server) throws ConfigIllegalException {


    this.analyticsDelivery = new AnalyticsDelivery(server);

    /**
     * Server event may have only the name of the event
     * that is not a property. The bean may then be empty
     * By default, Jackson failed, we disable it
     */
    this.jacksonMapperThatAllowEmptyBean = server.getJacksonMapperManager()
      .jsonMapperBuilder()
      .disableFailOnEmptyBeans()
      .build();

  }

  public static AnalyticsTracker createFromServer(Server server) throws ConfigIllegalException {

    return new AnalyticsTracker(server);

  }


  /**
   * @param analyticsServerEvent - an internal server event
   */
  public EventBuilder eventBuilder(AnalyticsServerEvent analyticsServerEvent) {


    return new EventBuilder()
      .setAnalyticsServerEvent(analyticsServerEvent);

  }


  /**
   * @param externalAnalyticsEvent - an external analytics received from the API
   */
  public EventBuilder eventBuilder(AnalyticsEvent externalAnalyticsEvent) {

    return new EventBuilder()
      .setClientEvent(externalAnalyticsEvent);

  }

  public class EventBuilder {

    private AuthUser authUser;


    /**
     * The http routing context
     * Event may be inserted outside an HTTP call
     * The context may be therefore null
     */
    private RoutingContext routingContext;


    private String organizationId;
    private String realmId;
    /**
     * We keep the server event to check if this a SignUp, SignIn and ProfileUpdate
     * event to deliver the user profile
     */
    private AnalyticsServerEvent analyticsServerEvent;
    private AnalyticsEvent analyticsClientEvent;

    private EventBuilder() {
    }


    public EventBuilder setUser(AuthUser authUser) {
      this.authUser = authUser;
      return this;
    }

    public EventBuilder setRoutingContext(RoutingContext routingContext) {
      this.routingContext = routingContext;
      return this;
    }

    /**
     * Process the event
     * - create the event and add it to the queue
     * - if the event is a SignIn, SignUp or profile update, create a user and add it to the queue
     */
    public void processEvent() {

       /**
       * Built and send the event to the queue
       * (the event is processed async)
       */
      analyticsDelivery.addEventToDelivery(buildEvent());

      if(this.analyticsServerEvent!=null){

      }


    }

    private AnalyticsEvent buildEventFromServerEvent() {

      AnalyticsEvent analyticsEvent = new AnalyticsEvent();

      String eventName = analyticsServerEvent.getName();
      if (eventName == null) {
        throw new InternalException("The event name is null but is mandatory");
      }
      analyticsEvent.setName(eventName);

      /**
       * App and request
       */
      analyticsEvent.setApp(analyticsServerEvent.getApp());
      analyticsEvent.setRequest(analyticsServerEvent.getRequest());

      /**
       * The primitive server name (ie event name, appId, ...)
       * processed above are ignored
       * when creating the Json object
       * No null or blank value
       */
      Map<String, Object> jsonObjectMapTarget = jacksonMapperThatAllowEmptyBean.convertValue(
          analyticsServerEvent,
          new TypeReference<Map<String, Object>>() {
          })
        .entrySet()
        .stream()
        .filter(e -> e.getValue() != null && !e.getValue().toString().isBlank())
        .collect(Collectors.toMap(
          Map.Entry::getKey,
          Map.Entry::getValue
        ));
      analyticsEvent.setAttr(jsonObjectMapTarget);
      return analyticsEvent;
    }

    private AnalyticsEvent buildEvent() {

      AnalyticsEvent analyticsEvent;
      if (analyticsServerEvent != null) {
        analyticsEvent = this.buildEventFromServerEvent();
      } else {
        if (analyticsClientEvent == null) {
          throw new InternalException("To build an event a server event or a client event should be given");
        }
        analyticsEvent = analyticsClientEvent;
      }

      /**
       * Normalize event name
       */
      String name = analyticsEvent.getName();
      if (name == null) {
        throw new InternalException("An event should have a name. The event (" + analyticsEvent.getClass().getSimpleName() + ") has no name");
      }
      KeyCase eventHandleCase = KeyCase.HANDLE;
      KeyNormalizer eventName = KeyNormalizer.createFromString(name);
      analyticsEvent.setName(eventName.toCase(eventHandleCase));

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
       * Next to the event name, normalize the other handle
       */
      String appHandle = analyticsEventApp.getAppHandle();
      if (appHandle != null) {
        analyticsEventApp.setAppHandle(KeyNormalizer.createFromString(appHandle).toCase(eventHandleCase));
      }
      String appRealmHandle = analyticsEventApp.getAppRealmHandle();
      if (appRealmHandle != null) {
        analyticsEventApp.setAppRealmHandle(KeyNormalizer.createFromString(appRealmHandle).toCase(eventHandleCase));
      }
      String appOrganisationHandle = analyticsEventApp.getAppOrganisationHandle();
      if (appOrganisationHandle != null) {
        analyticsEventApp.setAppOrganisationHandle(KeyNormalizer.createFromString(appOrganisationHandle).toCase(eventHandleCase));
      }
      String flowHandle = analyticsEventRequest.getFlowHandle();
      if (flowHandle != null) {
        analyticsEventRequest.setFlowHandle(KeyNormalizer.createFromString(flowHandle).toCase(eventHandleCase));
      }


      /**
       * Add user data
       */
      if (this.authUser != null) {

        analyticsEventUser.setUserGuid(authUser.getSubject());
        analyticsEventUser.setUserEmail(authUser.getSubjectEmail());

        /**
         * App data if any
         */
        analyticsEventApp.setAppRealmGuid(authUser.getRealmGuid());
        analyticsEventApp.setAppRealmHandle(authUser.getRealmHandle());
        String organizationGuid = authUser.getOrganizationGuid();
        if (organizationGuid != null) {
          // a user may have no organization
          // an app may not
          analyticsEventApp.setAppOrganisationGuid(organizationGuid);
          analyticsEventApp.setAppOrganisationHandle(authUser.getOrganizationHandle());
        }
      }

      /**
       * App data
       */
      if (this.realmId != null) {
        analyticsEventApp.setAppRealmGuid(this.realmId);
      }
      if (this.organizationId != null) {
        analyticsEventApp.setAppOrganisationGuid(this.realmId);
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
      LocalDateTime creationTime = analyticsEventState.getEventCreationTime();
      if (creationTime == null) {
        creationTime = DateTimeUtil.getNowInUtc();
        analyticsEventState.setEventCreationTime(creationTime);
      }
      if (analyticsEvent.getGuid() == null) {

        long timestamp = creationTime.toEpochSecond(ZoneOffset.UTC);
        UUID uuid = Generators.timeBasedEpochGenerator().construct(timestamp);
        analyticsEvent.setGuid(uuid.toString());

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

    public EventBuilder setAnalyticsServerEvent(AnalyticsServerEvent analyticsServerEvent) {
      this.analyticsServerEvent = analyticsServerEvent;
      return this;
    }

    public EventBuilder setClientEvent(AnalyticsEvent clientEvent) {
      this.analyticsClientEvent = clientEvent;
      return this;
    }
  }

}
