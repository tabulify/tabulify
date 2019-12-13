package net.bytle.api.http;

import com.codahale.metrics.Counter;
import com.codahale.metrics.SharedMetricRegistries;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.validation.ValidationException;

/**
 * DropWizard
 */
public class HandlerFailure implements Handler<RoutingContext> {

    private final Counter validationErrorsCounter;
    private static final Logger logger = LoggerFactory.getLogger(HandlerFailure.class);

    public HandlerFailure() {
        validationErrorsCounter = SharedMetricRegistries.getDefault().counter("validationErrors");
    }

    public void handle(RoutingContext context) {

        Throwable thrown = context.failure();
        String userId = context.request().getHeader("Authorization");
        recordError(userId, thrown);

        if(thrown instanceof ValidationException) {
            context.response().setStatusCode(400).end(thrown.getMessage());
        } else {
            context.response().setStatusCode(500).end(thrown.getMessage());
        }
    }

    private void recordError(String userId, Throwable thrown) {
        String dynamicMetadata = "";
        if(userId != null) {
            dynamicMetadata = String.format("userId=%s ", userId);
        }

        validationErrorsCounter.inc();
        logger.error(dynamicMetadata + thrown.getMessage());
    }
}
