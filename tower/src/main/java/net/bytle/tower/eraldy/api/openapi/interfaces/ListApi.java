package net.bytle.tower.eraldy.api.openapi.interfaces;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.*;

import java.util.List;

public interface ListApi  {

    /**
     * Get list information  Access by: * id with the listGuid * name with the listHandle and realmHandle
    */
    Future<ApiResponse<RegistrationList>> listGet(RoutingContext routingContext, String listGuid, String listHandle, String realmHandle);

    /**
     * Create a list where users can register
    */
    Future<ApiResponse<RegistrationList>> listPost(RoutingContext routingContext, ListPostBody listPostBody);

    /**
     * Shows a confirmation page for the registration
    */
    Future<ApiResponse<String>> listRegisterConfirmationRegistrationGet(RoutingContext routingContext, String registrationGuid, String redirectUri);

    /**
     * Register a user to a list by sending an email for validation
    */
    Future<ApiResponse<Void>> listRegisterPost(RoutingContext routingContext, ListRegistrationPostBody listRegistrationPostBody);

    /**
     * Get a registration object
    */
    Future<ApiResponse<Registration>> listRegistrationGet(RoutingContext routingContext, String guid, String listGuid, String subscriberEmail);

    /**
     * Return the confirmation letter after the user has clicked on the validation link.  To get a confirmation as it's show to a user, you just need to set the subscriber name and the list guid  The other parameters will overwrite other list information.
    */
    Future<ApiResponse<String>> listRegistrationLetterConfirmationGet(RoutingContext routingContext, String subscriberName, String listGuid, String listName, String ownerName, String ownerEmail, String ownerLogo);

    /**
     * Return the validation letter with a validation link as send by email.
    */
    Future<ApiResponse<String>> listRegistrationLetterValidationGet(RoutingContext routingContext, String listGuid, String subscriberName, String subscriberEmail, Boolean debug);

    /**
     * The URI of the validation link in the validation letter that the user receive. The user needs to click this link to confirm her/his registration.
    */
    Future<ApiResponse<String>> listRegistrationValidationGet(RoutingContext routingContext, String data);

    /**
     * List the registrations for a list
    */
    Future<ApiResponse<List<RegistrationShort>>> listRegistrationsGet(RoutingContext routingContext, String listGuid);

    /**
     * Get a list of lists  The app should be given via:   - the appGuid   - or the appUri along with a realm identifier (guid or handle)
    */
    Future<ApiResponse<List<RegistrationList>>> listsGet(RoutingContext routingContext, String appGuid, String appUri, String realmIdentifier);

    /**
     * Get a list of list in a summary format
    */
    Future<ApiResponse<List<ListSummary>>> listsSummaryGet(RoutingContext routingContext, String realmIdentifier);
}
