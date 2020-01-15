package us.vanderlugt.sample.vertx.model;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IslandVerticle extends AbstractVerticle {
    @Override
    public void start(Promise<Void> start) {
        log.debug("Island verticle started");
        start.complete();
    }
}
