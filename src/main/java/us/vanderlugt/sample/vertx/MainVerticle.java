package us.vanderlugt.sample.vertx;

import io.vertx.core.*;
import lombok.extern.slf4j.Slf4j;
import us.vanderlugt.sample.vertx.model.RepositoryVerticle;
import us.vanderlugt.sample.vertx.web.WebVerticle;

import java.util.function.Supplier;

import static io.vertx.core.CompositeFuture.all;

@Slf4j
public class MainVerticle extends AbstractVerticle {
    @Override
    public void start(Promise<Void> start) {
        log.debug("Starting main verticle");
        all(deploy(WebVerticle::new),
                deploy(RepositoryVerticle::new))
                .setHandler(async -> {
                    if (async.succeeded()) {
                        log.debug("Main verticle start complete");
                        start.complete();
                    } else {
                        log.debug("Main verticle start failed", async.cause());
                        start.fail(async.cause());
                    }
                });
    }

    private Future<String> deploy(Supplier<Verticle> supplier) {
        final Promise<String> promise = Promise.promise();
        final DeploymentOptions options = new DeploymentOptions();

        vertx.deployVerticle(supplier, options, promise);

        return promise.future();
    }
}
