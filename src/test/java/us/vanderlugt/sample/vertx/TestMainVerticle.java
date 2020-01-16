package us.vanderlugt.sample.vertx;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import us.vanderlugt.sample.vertx.model.RepositoryVerticle;

import java.io.IOException;
import java.net.ServerSocket;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {

    public static final String JDBC_URL = "jdbc:h2:mem:test";
    public static final String JDBC_DRIVER_CLASS = "org.h2.Driver";

    @Test
    void startMainVerticle(Vertx vertx, VertxTestContext context) throws Exception {
        vertx.deployVerticle(MainVerticle::new, getTestDeploymentOptions(randomPort()),
                context.succeeding(id -> context.completeNow()));
    }

    @Test
    void startRepositoryVerticle(Vertx vertx, VertxTestContext context) throws Exception {
        vertx.deployVerticle(RepositoryVerticle::new, getTestDeploymentOptions(randomPort()),
                context.succeeding(id -> {
                    JDBCClient client = JDBCClient.createShared(vertx,
                            new JsonObject()
                                    .put("url", JDBC_URL)
                                    .put("driver_class", JDBC_DRIVER_CLASS));
                    client.query("SELECT count(*) AS country_count FROM country", async -> {
                        assertThat(async.succeeded(), equalTo(true));
                        context.completeNow();
                    });
                }));
    }

    public static DeploymentOptions getTestDeploymentOptions(Integer port) throws IOException {
        return new DeploymentOptions()
                .setConfig(new JsonObject()
                        .put("http.port", port != null ? port : randomPort())
                        .put("jdbc.url", JDBC_URL)
                        .put("jdbc.driver_class", JDBC_DRIVER_CLASS));
    }


    public static Integer randomPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
