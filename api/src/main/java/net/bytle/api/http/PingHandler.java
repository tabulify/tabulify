package net.bytle.api.http;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class PingHandler implements Handler<RoutingContext> {

    private String message = "pong";

    public void handle(RoutingContext context) {
        context.response().setStatusCode(200).end(message);
    }

    public PingHandler setMessage(String message) {
        this.message = message;
        return this;
    }
}
