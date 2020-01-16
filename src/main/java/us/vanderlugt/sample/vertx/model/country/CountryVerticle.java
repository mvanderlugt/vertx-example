package us.vanderlugt.sample.vertx.model.country;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.ext.jdbc.JDBCClient;
import lombok.extern.slf4j.Slf4j;
import us.vanderlugt.sample.vertx.model.ConflictException;
import us.vanderlugt.sample.vertx.model.EntityNotFoundException;

@Slf4j
public class CountryVerticle extends AbstractVerticle {
    private final JDBCClient client;
    private final CountryRepository repository;

    public CountryVerticle(JDBCClient client) {
        this.client = client;
        this.repository = new CountryRepository(client);
    }

    @Override
    public void start(Promise<Void> start) {
        log.debug("Starting country verticle");
        vertx.eventBus().consumer("us.vanderlugt.country.create", this::createCountry);
        vertx.eventBus().consumer("us.vanderlugt.country.get", this::getCountryHandler);
        vertx.eventBus().consumer("us.vanderlugt.country.search", this::searchCountries);
        vertx.eventBus().consumer("us.vanderlugt.country.update", this::updateCountry);
        vertx.eventBus().consumer("us.vanderlugt.country.delete", (Handler<Message<String>>) this::deleteCountry);
        log.debug("Country verticle started");
        start.complete();
    }

    private void createCountry(Message<String> message) {
        Country country = Json.decodeValue(message.body(), Country.class);
        repository.createCountry(country)
                .setHandler(result -> {
                    if (result.succeeded()) {
                        log.debug("Country created, replying with country");
                        message.reply(Json.encode(result.result()));
                    } else if (result.cause() instanceof ConflictException) {
                        log.debug("Conflict detected, failing with 409 failure");
                        message.fail(409, "Conflict with existing country " + country.getId());
                    } else {
                        log.warn("Creating country failed, failing with 500", result.cause());
                        message.fail(500, result.cause().getLocalizedMessage());
                    }
                });
    }

    private void getCountryHandler(Message<String> message) {
        repository.getCountry(message.body())
                .setHandler(response -> {
                    if (response.succeeded()) {
                        Country country = response.result();
                        if (country != null) {
                            log.debug("Found country = {}", country);
                            message.reply(Json.encode(country));
                        } else {
                            log.debug("Country not found");
                            message.reply(null);
                        }
                    } else {
                        log.warn("Search for countries failed, failing with 500", response.cause());
                        message.fail(500, response.cause().getLocalizedMessage());
                    }
                });
    }

    private void searchCountries(Message<String> message) {
        repository.searchCountries()
                .setHandler(async -> {
                    if (async.succeeded()) {
                        if (async.result().size() > 0) {
                            log.debug("Replying with {} countries", async.result().size());
                            message.reply(Json.encode(async.result()));
                        } else {
                            log.debug("No countries found, replying with null");
                            message.reply(null);
                        }
                    } else {
                        log.warn("Search for countries failed, failing with 500", async.cause());
                        message.fail(500, async.cause().getLocalizedMessage());
                    }
                });
    }

    private void updateCountry(Message<String> message) {
        log.debug("Received message to update country, message = {}", message);
        Country update = Json.decodeValue(message.body(), Country.class);
        repository.updateCountry(update)
                .setHandler(result -> {
                    if (result.succeeded()) { //todo do we need to check rows updated?
                        message.reply(Json.encode(update));
                    } else if (result.cause() instanceof EntityNotFoundException) {
                        message.fail(404, "Country not found with id " + update.getId()); //todo decouple workers from http responses
                    } else {
                        message.fail(500, result.cause().getLocalizedMessage());
                    }
                });
    }

    private void deleteCountry(Message<String> message) {
        log.debug("Received message to delete country, message = {}", message);
        String id = message.body();
        repository.deleteCountry(message.body())
                .setHandler(response -> {
                    if (response.succeeded()) {
                        message.reply(Json.encode(response.result()));
                    } else if (response.cause() instanceof EntityNotFoundException) {
                        message.fail(404, "Country not found with id " + id); //todo decouple workers from http responses
                    } else {
                        message.fail(500, response.cause().getLocalizedMessage());
                    }
                });
    }
}
