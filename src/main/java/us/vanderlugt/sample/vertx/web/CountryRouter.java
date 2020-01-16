package us.vanderlugt.sample.vertx.web;

import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.validation.HTTPRequestValidationHandler;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;
import us.vanderlugt.sample.vertx.model.country.Country;

import static io.vertx.ext.web.api.validation.ValidationException.ErrorType.JSON_INVALID;
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
                .handler(HTTPRequestValidationHandler.create()
                        .addCustomValidatorFunction(context -> validateCountry(context, true)))
                .handler(context -> create(vertx, context))
                .failureHandler(this::validationErrorHandler);
        router.get("/country/:id")
                .handler(HTTPRequestValidationHandler.create()
                        .addPathParamWithPattern("id", "[A-Z]{2}"))
                .handler(context -> get(vertx, context))
                .failureHandler(this::validationErrorHandler);
        router.get("/country")
                .handler(context -> search(vertx, context));
        router.put("/country/:id")
                .handler(BodyHandler.create())
                .handler(HTTPRequestValidationHandler.create()
                        .addPathParamWithPattern("id", "[A-Z]{2}")
                        .addCustomValidatorFunction(context -> validateCountry(context, true)))
                .handler(context -> update(vertx, context))
                .failureHandler(this::validationErrorHandler);
        router.delete("/country/:id")
                .handler(HTTPRequestValidationHandler.create()
                        .addPathParamWithPattern("id", "[A-Z]{2}"))
                .handler(context -> deleteCountry(vertx, context))
                .failureHandler(this::validationErrorHandler);
        return router;
    }

    private void validationErrorHandler(RoutingContext context) {
        Throwable failure = context.failure();
        if (failure instanceof ValidationException) {
            context.response().setStatusCode(400)
                    .end(new JsonObject()
                            .put("message", failure.getLocalizedMessage())
                            .encode());
        }
    }

    private void validateCountry(RoutingContext context, boolean requireId) throws ValidationException {
        Country country = Json.decodeValue(context.getBodyAsString(), Country.class);
        if (requireId && country.getId() == null) {
            throw new ValidationException("Country ID is required");
        } else if (country.getId().length() != 2) {
            throw new ValidationException("Country ID must be exactly 2 characters long, refer to ISO-3166");
        } else if (!country.getId().matches("[A-Z]{2}")) {
            throw new ValidationException("Country ID must match pattern [A-Z]{2}");
        } else if (country.getName() == null) {
            throw new ValidationException("Country name is required", JSON_INVALID);
        } else if (country.getName().length() < 1 || country.getName().length() > 100) {
            throw new ValidationException("Country name must be between 1 and 100 characters long", JSON_INVALID);
        } else if (country.getCapital() == null) {
            throw new ValidationException("Country capital is required", JSON_INVALID);
        } else if (country.getCapital().length() < 1 || country.getCapital().length() > 100) {
            throw new ValidationException("Country capital must be between 1 and 100 characters long", JSON_INVALID);
        }
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
        Country country = Json.decodeValue(context.getBodyAsString(), Country.class);
        country.setId(context.pathParam("id"));
        vertx.eventBus()
                .request("us.vanderlugt.country.update",
                        Json.encode(country),
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
