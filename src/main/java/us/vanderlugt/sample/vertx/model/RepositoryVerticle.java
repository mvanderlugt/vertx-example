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
    @Override
    public void start(Promise<Void> start) {
        log.debug("Starting Repository verticle");
        JDBCClient client = JDBCClient.createShared(vertx, new JsonObject()
                .put("url", config().getValue("jdbc.url", "jdbc:h2:./database/test"))
                .put("driver_class", config().getValue("jdbc.driver_class", "org.h2.Driver")));
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
                .setConfig(config())
                .setWorker(true);

        vertx.deployVerticle(supplier, options, promise);

        return promise.future();
    }
}
