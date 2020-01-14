package us.vanderlugt.sample.vertx.web;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.LoggerHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebVerticle extends AbstractVerticle {
    @Override
    public void start(Promise<Void> start) {
        log.debug("API verticle started");
        vertx.createHttpServer()
                .requestHandler(routes()
                .mountSubRouter("/api", new CountryRouter().routes(vertx)))
                .listen(config().getInteger("http.port", 8008),
                        result -> {
                            if (result.succeeded()) {
                                log.debug("HTTP server started, API verticle start complete");
                                start.complete();
                            } else {
                                start.fail(result.cause());
                            }
                        });
    }

    private Router routes() {
        Router router = Router.router(vertx);
        router.route().handler(LoggerHandler.create());
        router.get("/").handler(context -> context.response().end("Welcome!"));
        return router;
    }
}
