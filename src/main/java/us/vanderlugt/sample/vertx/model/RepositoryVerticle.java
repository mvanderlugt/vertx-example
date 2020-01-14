package us.vanderlugt.sample.vertx.model;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RepositoryVerticle extends AbstractVerticle {
    @Override
    public void start(Promise<Void> start) throws Exception {
        log.debug("Repository verticle started");
        start.complete();
    }
}
