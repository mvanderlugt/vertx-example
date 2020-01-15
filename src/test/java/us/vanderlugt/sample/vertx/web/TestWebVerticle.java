package us.vanderlugt.sample.vertx.web;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(VertxExtension.class)
public class TestWebVerticle {
    public static final WebClientOptions WEB_CLIENT_OPTIONS = new WebClientOptions()
            .setDefaultHost("localhost")
            .setDefaultPort(8008);

    @Test
    void testWebVerticle(Vertx vertx, VertxTestContext context) {
        vertx.deployVerticle(WebVerticle::new, new DeploymentOptions(), context.succeeding(id -> {
            WebClient client = WebClient.create(vertx, WEB_CLIENT_OPTIONS);
            client.get("/")
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
