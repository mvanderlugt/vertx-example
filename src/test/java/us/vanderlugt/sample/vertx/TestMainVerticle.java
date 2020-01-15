package us.vanderlugt.sample.vertx;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import us.vanderlugt.sample.vertx.model.RepositoryVerticle;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {

    @Test
    void startMainVerticle(Vertx vertx, VertxTestContext context) {
        vertx.deployVerticle(MainVerticle::new,
                new DeploymentOptions(),
                context.succeeding(id -> context.completeNow()));
    }

    @Test
    void startRepositoryVerticle(Vertx vertx, VertxTestContext context) {
        vertx.deployVerticle(RepositoryVerticle::new, new DeploymentOptions(), context.succeeding(id -> {
            JDBCClient client = JDBCClient.createShared(vertx, JsonObject.mapFrom(RepositoryVerticle.DATABASE_CONFIG));
            client.query("SELECT count(*) AS country_count FROM country", async -> {
                assertThat(async.succeeded(), equalTo(true));
                assertThat(async.result().getNumRows(), equalTo(1));
                assertThat(async.result().getRows().get(0).getInteger("COUNTRY_COUNT"), greaterThan(0));
                context.completeNow();
            });
        }));
    }
}
