package us.vanderlugt.sample.vertx.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import us.vanderlugt.sample.vertx.model.country.CountryVerticle;

import java.util.function.Supplier;

@Slf4j
public class RepositoryVerticle extends AbstractVerticle {
    //todo externalize
    public static final DatabaseConfig DATABASE_CONFIG = DatabaseConfig.builder()
            .url("jdbc:h2:mem:test")
            .driverClass("org.h2.Driver")
            .build();

    @Data
    @Builder
    private static class DatabaseConfig {
        @JsonProperty("url")
        private String url;
        @JsonProperty("driver_class")
        private String driverClass;
    }

    @Override
    public void start(Promise<Void> start) {
        log.debug("Starting Repository verticle");
        JDBCClient client = JDBCClient.createShared(vertx, JsonObject.mapFrom(DATABASE_CONFIG));
        client.getConnection(connect -> {
            if (connect.succeeded()) {
                try (SQLConnection connection = connect.result()) {
                    runLiquibaseMigration(connection);
                    //todo add DB management routes
                    deploy(() -> new CountryVerticle(client))
                            .setHandler(async -> {
                                if (async.succeeded()) {
                                    log.debug("Repository verticle started");
                                    start.complete();
                                } else {
                                    start.fail(async.cause());
                                }
                            });
                } catch (LiquibaseException exception) {
                    start.fail(exception);
                }
            } else {
                start.fail(connect.cause());
            }
        });
    }

    private void runLiquibaseMigration(SQLConnection connection) throws LiquibaseException {
        log.debug("Running liquibase migration");
        new Liquibase("db/changelog-master.yml", //todo external property
                new ClassLoaderResourceAccessor(),
                new JdbcConnection(connection.unwrap()))
                .update("");
    }

    private Future<String> deploy(Supplier<Verticle> supplier) {
        final Promise<String> promise = Promise.promise();
        final DeploymentOptions options = new DeploymentOptions()
                .setWorker(true);

        vertx.deployVerticle(supplier, options, promise);

        return promise.future();
    }
}
