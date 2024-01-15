package net.bytle.tower.eraldy.api.openapi.interfaces;

import io.vertx.ext.web.FileUpload;
import net.bytle.tower.eraldy.model.openapi.ListBody;
import net.bytle.tower.eraldy.model.openapi.ListImportJobRowStatus;
import net.bytle.tower.eraldy.model.openapi.ListImportJobStatus;
import net.bytle.tower.eraldy.model.openapi.ListImportPostResponse;
import net.bytle.tower.eraldy.model.openapi.ListItem;
import net.bytle.tower.eraldy.model.openapi.ListSummary;
import net.bytle.tower.eraldy.model.openapi.ListUser;
import net.bytle.tower.eraldy.model.openapi.ListUserShort;

import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;

public interface ListApi  {

  /**
     * Delete the list
    */
    Future<ApiResponse<Void>> listListDelete(RoutingContext routingContext, String listIdentifier, String realmIdentifier);

  /**
   * Get list information  Access by: * id with the listGuid * name with the listHandle and realmIdentifier
    */
    Future<ApiResponse<ListItem>> listListGet(RoutingContext routingContext, String listIdentifier, String realmIdentifier);

  /**
     * The list of emails with their import status
    */
    Future<ApiResponse<List<ListImportJobRowStatus>>> listListImportJobDetailsGet(RoutingContext routingContext, String listIdentifier, String jobIdentifier);

  /**
     * The import status for the job
    */
    Future<ApiResponse<ListImportJobStatus>> listListImportJobGet(RoutingContext routingContext, String listIdentifier, String jobIdentifier);

  /**
     * Submit an import of users for a list
    */
    Future<ApiResponse<ListImportPostResponse>> listListImportPost(RoutingContext routingContext, String listIdentifier, Integer rowCountToProcess, FileUpload fileBinary);

  /**
     * A list of imports
    */
    Future<ApiResponse<List<ListImportJobStatus>>> listListImportsGet(RoutingContext routingContext, String listIdentifier);

  /**
   * Update a list
    */
    Future<ApiResponse<ListItem>> listListPatch(RoutingContext routingContext, String listIdentifier, ListBody listBody, String realmIdentifier);

  /**
   * List the users for a list
    */
  Future<ApiResponse<List<ListUserShort>>> listListUsersGet(RoutingContext routingContext, String listIdentifier, Long pageSize, Long pageId, String searchTerm);

    /**
     * Shows a confirmation page for the registration
    */
    Future<ApiResponse<String>> listUserConfirmationUserGet(RoutingContext routingContext, String listUserIdentifier, String redirectUri);

    /**
     * Get a user in a list
    */
    Future<ApiResponse<ListUser>> listUserIdentifierGet(RoutingContext routingContext, String listUserIdentifier);

    /**
     * Deprecated - The front end is now separated from the API  Return the confirmation letter after the user has clicked on the validation link.  To get a confirmation as it's show to a user, you just need to set the subscriber name and the list guid  The other parameters will overwrite other list information.
    */
    Future<ApiResponse<String>> listUserLetterConfirmationGet(RoutingContext routingContext, String subscriberName, String listGuid, String listName, String ownerName, String ownerEmail, String ownerLogo);

    /**
     * Return the validation letter with a validation link as send by email.
    */
    Future<ApiResponse<String>> listUserLetterValidationGet(RoutingContext routingContext, String listGuid, String subscriberName, String subscriberEmail, Boolean debug);

    /**
     * Get a list of lists  The app should be given via:   - the appGuid   - or the appUri along with a realm identifier (guid or handle)
    */
    Future<ApiResponse<List<ListItem>>> listsGet(RoutingContext routingContext, String appGuid, String appUri, String realmIdentifier);

  /**
     * Get a list of list in a summary format
    */
    Future<ApiResponse<List<ListSummary>>> listsSummaryGet(RoutingContext routingContext, String realmIdentifier);
}
