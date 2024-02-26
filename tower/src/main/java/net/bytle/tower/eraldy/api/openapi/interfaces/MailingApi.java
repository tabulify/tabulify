package net.bytle.tower.eraldy.api.openapi.interfaces;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.eraldy.api.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.Mailing;
import net.bytle.tower.eraldy.model.openapi.MailingUpdatePost;

public interface MailingApi  {

    /**
     * The mailing
    */
    Future<ApiResponse<Mailing>> mailingIdentifierGet(RoutingContext routingContext, String mailingIdentifier);

    /**
     * Mailing Update
    */
    Future<ApiResponse<Mailing>> mailingIdentifierPost(RoutingContext routingContext, String mailingIdentifier, MailingUpdatePost mailingUpdatePost);
}
