package us.vanderlugt.sample.vertx.model;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.extern.slf4j.Slf4j;
import us.vanderlugt.sample.vertx.model.country.CountryVerticle;

import java.util.function.Supplier;

import static io.vertx.core.Future.future;

@Slf4j
public class RepositoryVerticle extends AbstractVerticle {
    @Override
    public void start(Promise<Void> start) {
        log.debug("Starting Repository verticle");
        final JDBCClient client = JDBCClient.createShared(vertx, new JsonObject()
                .put("url", config().getValue("jdbc.url", "jdbc:h2:./database/test"))
                .put("driver_class", config().getValue("jdbc.driver_class", "org.h2.Driver")));

        future(client::getConnection)
                .compose(connection -> {
                    Promise<Void> promise = Promise.promise();
                    vertx.executeBlocking(runLiquibaseMigration(connection), promise);
                    return promise.future();
                })
                .compose(r -> {
                    return deploy(() -> new CountryVerticle(client));
                })
                .compose(id -> Future.<Void>succeededFuture())
                .setHandler(start);
    }

    private Handler<Promise<Void>> runLiquibaseMigration(SQLConnection connection) {
        return blockingPromise -> {
            try {
                log.debug("Running liquibase migration");
                new Liquibase("db/changelog-master.yml", //todo external property
                        new ClassLoaderResourceAccessor(),
                        new JdbcConnection(connection.unwrap()))
                        .update("");
                blockingPromise.complete();
            } catch (LiquibaseException exception) {
                blockingPromise.fail(exception);
            }
        };
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
