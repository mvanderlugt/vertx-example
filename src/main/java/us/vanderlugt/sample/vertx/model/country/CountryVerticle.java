package us.vanderlugt.sample.vertx.model.country;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException;

import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class CountryVerticle extends AbstractVerticle {
    private final JDBCClient client;

    @Override
    public void start(Promise<Void> start) {
        log.debug("Starting country verticle");
        vertx.eventBus().consumer("us.vanderlugt.country.search", this::searchCountries);
        vertx.eventBus().consumer("us.vanderlugt.country.get", this::getCountry);
        vertx.eventBus().consumer("us.vanderlugt.country.create", this::createCountry);
        log.debug("Country verticle started");
        start.complete();
    }

    private void searchCountries(Message<String> message) {
        String sql = "SELECT id, name FROM country ORDER BY UPPER(name)";
        log.debug("Searching for countries, sql = {}", sql);
        client.query(sql, response -> {
            if (response.succeeded()) {
                ResultSet resultSet = response.result();
                log.debug("Found {} countries", resultSet.getNumRows());
                if (resultSet.getNumRows() > 0) {
                    message.reply(Json.encode(resultSet.getRows().stream()
                            .map(Country::map)
                            .peek(c -> log.debug("Mapped country {}", c))
                            .collect(Collectors.toList())));
                } else {
                    log.debug("No countries found, replying with null");
                    message.reply(null);
                }
            } else {
                log.warn("Search for countries failed, failing with 500", response.cause());
                message.fail(500, response.cause().getLocalizedMessage());
            }
        });
    }

    private void getCountry(Message<String> message) {
        String sql = "SELECT id, name FROM country WHERE id = ?";
        log.debug("Getting country with id = {}, sql = {}", message.body(), sql);
        JsonArray params = new JsonArray()
                .add(message.body());
        client.queryWithParams(sql, params, response -> {
            if (response.succeeded()) {
                ResultSet resultSet = response.result();
                log.debug("Found {} countries", resultSet.getNumRows());
                if (resultSet.getNumRows() == 1) {
                    log.debug("Found country = {}", resultSet.getRows().get(0));
                    message.reply(Json.encode(Country.map(resultSet.getRows().get(0))));
                } else if (resultSet.getNumRows() == 0) {
                    log.debug("Country not found");
                    message.reply(null);
                } else {
                    log.debug("Found {} countries, unexpected result for primary key search", resultSet.getNumRows());
                    message.fail(500, "Unexpected number of rows returned");
                }
            } else {
                log.warn("Search for countries failed, failing with 500", response.cause());
                message.fail(500, response.cause().getLocalizedMessage());
            }
        });
    }

    private void createCountry(Message<String> message) {
        Country country = Json.decodeValue(message.body(), Country.class);
        log.debug("Creating country = {}", country);
        String sql = "INSERT INTO country (id, name) " +
                "VALUES (?, ?)";
        JsonArray params = new JsonArray()
                .add(country.getId())
                .add(country.getName());
        log.debug("Insert sql = {}, params = {}", sql, params);
        client.updateWithParams(sql, params, result -> {
            if (result.succeeded()) {
                log.debug("Country created, replying with country");
                message.reply(Json.encode(country));
            } else if (result.cause() instanceof JdbcSQLIntegrityConstraintViolationException) {
                log.debug("SQL integrity constraint violated, failing with 409 failure");
                message.fail(409, "Conflict with existing country");
            } else {
                log.warn("Creating country failed, failing with 500", result.cause());
                message.fail(500, result.cause().getLocalizedMessage());
            }
        });
    }
}
