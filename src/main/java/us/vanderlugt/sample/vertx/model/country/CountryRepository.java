package us.vanderlugt.sample.vertx.model.country;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException;
import us.vanderlugt.sample.vertx.model.ConflictException;
import us.vanderlugt.sample.vertx.model.EntityNotFoundException;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class CountryRepository {
    private final JDBCClient client;

    public Future<Country> createCountry(Country country) {
        return Future.future(promise -> {
            String sql = "INSERT INTO country (id, name, capital) " +
                    "VALUES (?, ?, ?)";
            JsonArray params = new JsonArray()
                    .add(country.getId())
                    .add(country.getName())
                    .add(country.getCapital());
            log.debug("Insert sql = {}, params = {}", sql, params);
            client.updateWithParams(sql, params, result -> {
                if (result.succeeded()) {
                    log.debug("Country created, replying with country");
                    promise.complete(country);
                } else if (result.cause() instanceof JdbcSQLIntegrityConstraintViolationException) {
                    log.debug("SQL integrity constraint violated, failing with 409 failure");
                    promise.fail(new ConflictException(country.getId()));
                } else {
                    log.warn("Creating country failed, failing with 500", result.cause());
                    promise.fail(result.cause());
                }
            });
        });

    }

    public Future<Country> getCountry(String id) {
        return Future.future(promise -> {
            String sql = "SELECT id, name, capital FROM country WHERE id = ?";
            log.debug("Getting country with id = {}, sql = {}", id, sql);
            JsonArray params = new JsonArray()
                    .add(id);
            client.queryWithParams(sql, params, response -> {
                if (response.succeeded()) {
                    ResultSet resultSet = response.result();
                    log.debug("Found {} countries", resultSet.getNumRows());
                    if (resultSet.getNumRows() == 1) {
                        log.debug("Found country = {}", resultSet.getRows().get(0));
                        promise.complete(Country.map(resultSet.getRows().get(0)));
                    } else if (resultSet.getNumRows() == 0) {
                        log.debug("Country not found");
                        promise.complete();
                    } else {
                        log.debug("Found {} countries, unexpected result for primary key search", resultSet.getNumRows());
                        promise.fail("Unexpected number of rows searching for country with id " + id);
                    }
                } else {
                    log.warn("Search for countries failed, failing with 500", response.cause());
                    promise.fail(response.cause());
                }
            });
        });
    }

    public Future<List<Country>> searchCountries() {
        return Future.future(promise -> {
            String sql = "SELECT id, name, capital FROM country ORDER BY UPPER(name)";
            log.debug("Searching for countries, sql = {}", sql);
            client.query(sql, response -> {
                if (response.succeeded()) {
                    ResultSet resultSet = response.result();
                    log.debug("Found {} countries", resultSet.getNumRows());
                    if (resultSet.getNumRows() > 0) {
                        promise.complete(resultSet.getRows().stream()
                                .map(Country::map)
                                .peek(c -> log.debug("Mapped country {}", c))
                                .collect(Collectors.toList()));
                    } else {
                        log.debug("No countries found, replying with null");
                        promise.complete(Collections.emptyList());
                    }
                } else {
                    log.warn("Search for countries failed, failing with 500", response.cause());
                    promise.fail(response.cause());
                }
            });
        });
    }

    public Future<Country> updateCountry(Country update) {
        return getCountry(update.getId())
                .compose(existing -> Future.future(promise -> {
                    if (existing != null) {
                        log.debug("Updated country = {}", existing);
                        String sql = "UPDATE country SET name = ?, capital = ? WHERE id = ?";
                        JsonArray params = new JsonArray()
                                .add(update.getName())
                                .add(update.getCapital())
                                .add(update.getId());
                        client.updateWithParams(sql, params, result -> {
                            if (result.succeeded()) { //todo do we need to check rows updated?
                                promise.complete(update);
                            } else {
                                promise.fail(result.cause());
                            }
                        });
                    } else {
                        promise.fail(new EntityNotFoundException(update.getId()));
                    }
                }));
    }

    public Future<Country> deleteCountry(String id) {
        return getCountry(id)
                .compose(country -> Future.future(
                        promise -> {
                            if (country != null) {
                                log.debug("Deleting country = {}", country);
                                String sql = "DELETE FROM country WHERE id = ?";
                                JsonArray params = new JsonArray()
                                        .add(id);
                                client.updateWithParams(sql, params, result -> {
                                    if (result.succeeded()) { //todo do we need to check rows updated?
                                        promise.complete(country);
                                    } else {
                                        promise.fail(result.cause());
                                    }
                                });
                            } else {
                                promise.fail(new EntityNotFoundException(id));
                            }
                        }));
    }
}
