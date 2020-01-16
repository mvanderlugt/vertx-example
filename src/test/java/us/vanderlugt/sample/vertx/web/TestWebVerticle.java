package us.vanderlugt.sample.vertx.web;

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
import static us.vanderlugt.sample.vertx.TestMainVerticle.getTestDeploymentOptions;
import static us.vanderlugt.sample.vertx.TestMainVerticle.randomPort;

@ExtendWith(VertxExtension.class)
public class TestWebVerticle {
    @Test
    void testWebVerticle(Vertx vertx, VertxTestContext context) throws Exception {
        Integer port = randomPort();
        vertx.deployVerticle(WebVerticle::new, getTestDeploymentOptions(port), context.succeeding(id -> {
            WebClient client = WebClient.create(vertx, new WebClientOptions()
                    .setDefaultHost("localhost")
                    .setDefaultPort(port));
            client.get("/")
                    .as(BodyCodec.string())
                    .send(context.succeeding(response -> {
                        context.verify(() -> assertThat(response.body(), equalTo("Welcome!")));
                        context.completeNow();
                    }));
        }));
    }
}
