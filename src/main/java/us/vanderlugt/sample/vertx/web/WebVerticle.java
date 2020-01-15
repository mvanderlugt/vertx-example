package us.vanderlugt.sample.vertx.web;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.LoggerHandler;
import lombok.extern.slf4j.Slf4j;

import static us.vanderlugt.sample.vertx.web.HttpStatus.OK;

@Slf4j
public class WebVerticle extends AbstractVerticle {
    @Override
    public void start(Promise<Void> start) {
        log.debug("Starting Web verticle");
        vertx.createHttpServer()
                .requestHandler(routes()
                .mountSubRouter("/api", new CountryRouter().routes(vertx)))
                .listen(config().getInteger("http.port", 8008), //todo externalize port
                        result -> {
                            if (result.succeeded()) {
                                HttpServer server = result.result();
                                log.debug("Web verticle started on port " + server.actualPort());
                                start.complete();
                            } else {
                                start.fail(result.cause());
                            }
                        });
    }

    private Router routes() {
        Router router = Router.router(vertx);
        router.route().handler(LoggerHandler.create());
        router.get("/").handler(context ->
                context.response()
                        .setStatusCode(OK.getCode())
                        .end("Welcome!"));
        return router;
    }
}
