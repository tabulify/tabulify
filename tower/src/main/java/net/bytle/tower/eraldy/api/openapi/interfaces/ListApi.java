package net.bytle.tower.eraldy.api.openapi.interfaces;

import io.vertx.core.Future;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.*;

import java.util.List;

public interface ListApi  {

    /**
     * Delete the list
    */
    Future<ApiResponse<Void>> listListDelete(RoutingContext routingContext, String listIdentifier, String realmIdentifier);

    /**
     * Get list information  Access by: * id with the listGuid * name with the listHandle and realmIdentifier
    */
    Future<ApiResponse<ListObject>> listListGet(RoutingContext routingContext, String listIdentifier, String realmIdentifier);

    /**
     * Create a new mailing for a list
    */
    Future<ApiResponse<Mailing>> listListIdentifierMailingPost(RoutingContext routingContext, String listIdentifier, ListMailingCreationPost listMailingCreationPost);

    /**
     * Retrieve a list of mailings for a list
    */
    Future<ApiResponse<List<Mailings>>> listListIdentifierMailingsGet(RoutingContext routingContext, String listIdentifier);

    /**
     * Register a public user to a list by sending an email for validation
    */
    Future<ApiResponse<Void>> listListIdentifierRegisterPost(RoutingContext routingContext, String listIdentifier, ListUserPostBody listUserPostBody);

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
    Future<ApiResponse<ListImportPostResponse>> listListImportPost(RoutingContext routingContext, String listIdentifier, Integer rowCountToProcess, Integer parallelCount, FileUpload fileBinary);

    /**
     * A list of imports
    */
    Future<ApiResponse<List<ListImportJobStatus>>> listListImportsGet(RoutingContext routingContext, String listIdentifier);

    /**
     * Update a list
    */
    Future<ApiResponse<ListObject>> listListPatch(RoutingContext routingContext, String listIdentifier, ListBody listBody, String realmIdentifier);

    /**
     * List the users for a list
    */
    Future<ApiResponse<List<ListUserShort>>> listListUsersGet(RoutingContext routingContext, String listIdentifier, Long pageSize, Long pageId, String searchTerm);

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
     * Get a list of list in a summary format
    */
    Future<ApiResponse<List<ListSummary>>> listsSummaryGet(RoutingContext routingContext, String realmIdentifier);
}
