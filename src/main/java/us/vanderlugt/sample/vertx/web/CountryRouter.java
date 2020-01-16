package us.vanderlugt.sample.vertx.web;

import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;

import static us.vanderlugt.sample.vertx.web.HttpStatus.*;

@Slf4j
public class CountryRouter {
    public Router routes(Vertx vertx) {
        Router router = Router.router(vertx);
        router.route("/*").handler(context -> {
            context.response().putHeader("Content-Type", "application/json; charset=utf-8");
            context.next();
        });
        router.post("/country")
                .handler(BodyHandler.create())
                .handler(context -> create(vertx, context));
        router.get("/country/:id").handler(context -> get(vertx, context));
        router.get("/country").handler(context -> search(vertx, context));
        router.put("/country/:id")
                .handler(BodyHandler.create())
                .handler(context -> update(vertx, context));
        router.delete("/country/:id")
                .handler(context -> deleteCountry(vertx, context));
        return router;
    }

    private void create(Vertx vertx, RoutingContext context) {
        vertx.eventBus()
                .request("us.vanderlugt.country.create",
                        context.getBodyAsString(),
                        (AsyncResult<Message<String>> response) -> {
                            if (response.succeeded()) {
                                log.debug("Country successfully created: {}", response.result().body());
                                context.response()
                                        .setStatusCode(CREATED.getCode())
                                        .end(response.result().body());
                            } else if (response.cause() instanceof ReplyException) {
                                handleReplyException(context, (ReplyException) response.cause());
                            } else {
                                handleUnexpectedException(context, response.cause());
                            }
                        });
    }

    private void get(Vertx vertx, RoutingContext context) {
        String id = context.pathParam("id");
        vertx.eventBus()
                .request("us.vanderlugt.country.get", id,
                        (AsyncResult<Message<String>> response) -> {
                            if (response.succeeded()) {
                                Message<String> message = response.result();
                                if (message.body() != null) {
                                    context.response()
                                            .setStatusCode(OK.getCode())
                                            .end(message.body());
                                } else {
                                    context.response()
                                            .setStatusCode(NOT_FOUND.getCode())
                                            .end();
                                }
                            } else if (response.cause() instanceof ReplyException) {
                                handleReplyException(context, (ReplyException) response.cause());
                            } else {
                                handleUnexpectedException(context, response.cause());
                            }
                        });
    }

    private void search(Vertx vertx, RoutingContext context) {
        vertx.eventBus()
                .request("us.vanderlugt.country.search", null,
                        (AsyncResult<Message<String>> response) -> {
                            if (response.succeeded()) {
                                Message<String> message = response.result();
                                if (message.body() != null) {
                                    context.response()
                                            .setStatusCode(OK.getCode())
                                            .end(message.body());
                                } else {
                                    context.response()
                                            .setStatusCode(NO_CONTENT.getCode())
                                            .end();
                                }
                            } else {
                                handleUnexpectedException(context, response.cause());
                            }
                        });
    }

    private void update(Vertx vertx, RoutingContext context) {
        vertx.eventBus()
                .request("us.vanderlugt.country.update",
                        context.getBodyAsString(),
                        (AsyncResult<Message<String>> response) -> {
                            if (response.succeeded()) {
                                log.debug("Country successfully updated: {}", response.result().body());
                                context.response()
                                        .setStatusCode(OK.getCode())
                                        .end(response.result().body());
                            } else if (response.cause() instanceof ReplyException) {
                                handleReplyException(context, (ReplyException) response.cause());
                            } else {
                                handleUnexpectedException(context, response.cause());
                            }
                        });
    }

    private void deleteCountry(Vertx vertx, RoutingContext context) {
        vertx.eventBus()
                .request("us.vanderlugt.country.delete",
                        context.pathParam("id"),
                        (AsyncResult<Message<String>> response) -> {
                            if (response.succeeded()) {
                                log.debug("Country successfully deleted: {}", response.result().body());
                                context.response()
                                        .setStatusCode(OK.getCode())
                                        .end(response.result().body());
                            } else if (response.cause() instanceof ReplyException) {
                                handleReplyException(context, (ReplyException) response.cause());
                            } else {
                                handleUnexpectedException(context, response.cause());
                            }
                        });
    }

    private void handleReplyException(RoutingContext context, ReplyException exception) {
        log.debug("Reply exception, failure code = {}", exception.failureCode(), exception);
        if (exception.failureCode() >= 400) {
            context.response()
                    .setStatusCode(exception.failureCode())
                    .end(exception.getLocalizedMessage());
        } else {
            context.response()
                    .setStatusCode(500)
                    .end(exception.getLocalizedMessage());
        }
    }

    private void handleUnexpectedException(RoutingContext context, Throwable exception) {
        log.debug("Unexpected exception creating country", exception);
        context.response()
                .setStatusCode(500)
                .end(exception.getLocalizedMessage());
    }
}
