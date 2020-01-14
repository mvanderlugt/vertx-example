package us.vanderlugt.sample.vertx.web;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CountryRouter {
    public Router routes(Vertx vertx) {
        Router router = Router.router(vertx);
        router.get("/country").handler(this::search);
        router.get("/country/:id").handler(this::get);
        router.post("/country").handler(context -> requestNewCountry(vertx, context));
        return router;
    }

    private void search(RoutingContext context) {

    }

    private void get(RoutingContext context) {

    }

    private void requestNewCountry(Vertx vertx, RoutingContext context) {

    }
}
