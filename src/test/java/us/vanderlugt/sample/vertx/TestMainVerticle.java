package us.vanderlugt.sample.vertx;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {
    @Test
    void startMainVerticle(Vertx vertx, VertxTestContext context) {
        vertx.deployVerticle(MainVerticle::new, new DeploymentOptions(), context.succeeding(id -> {
            WebClient client = WebClient.create(vertx);
            client.get(8008, "localhost", "/")
                    .as(BodyCodec.string())
                    .send(context.succeeding(response -> {
                        context.verify(() -> {
                            assertThat(response.body(), equalTo("Welcome!"));
                            context.completeNow();
                        });
                    }));
        }));
    }
}
