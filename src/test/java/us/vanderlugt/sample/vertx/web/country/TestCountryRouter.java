package us.vanderlugt.sample.vertx.web.country;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import us.vanderlugt.sample.vertx.MainVerticle;
import us.vanderlugt.sample.vertx.model.country.Country;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static io.vertx.core.Future.future;
import static io.vertx.core.http.HttpMethod.*;
import static io.vertx.ext.web.client.predicate.ResponsePredicate.status;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static us.vanderlugt.sample.vertx.web.HttpStatus.*;
import static us.vanderlugt.sample.vertx.web.TestWebVerticle.WEB_CLIENT_OPTIONS;

@ExtendWith(VertxExtension.class)
public class TestCountryRouter {
    private static WebClient client;

    @BeforeAll
    static void initialize(Vertx vertx) {
        client = WebClient.create(vertx, WEB_CLIENT_OPTIONS);
    }

    @Test
    void testCreateCountry(Vertx vertx, VertxTestContext context) {
        deployMainVerticle(vertx, context,
                id -> future(promise -> client.request(POST, "/api/country")
                        .expect(status(CREATED.getCode()))
                        .as(BodyCodec.json(Country.class))
                        .sendJson(new Country("BM", "Bermuda", "Hamilton"),
                                context.succeeding(response -> {
                                    context.verify(() -> {
                                        Country country = response.body();
                                        assertThat(country.getId(), equalTo("BM"));
                                        assertThat(country.getName(), equalTo("Bermuda"));
                                    });
                                    promise.complete();
                                })))
                        .setHandler(context.succeeding(none ->
                                client.request(GET, "/api/country/BM")
                                        .expect(status(OK.getCode()))
                                        .as(BodyCodec.json(Country.class))
                                        .send(context.succeeding(response -> {
                                                    context.verify(() -> {
                                                        Country country = response.body();
                                                        assertThat(country.getId(), equalTo("BM"));
                                                        assertThat(country.getName(), equalTo("Bermuda"));
                                                    });
                                                    context.completeNow();
                                                }
                                        )))));
    }

    @Test
    void testCreateDuplicateCountryId(Vertx vertx, VertxTestContext context) {
        deployMainVerticle(vertx, context,
                id -> future(promise ->
                        client.request(POST, "/api/country")
                                .expect(status(CREATED.getCode()))
                                .as(BodyCodec.json(Country.class))
                                .sendJson(new Country("AR", "Argentina", "Buenos Aires"),
                                        context.succeeding(promise::complete)))
                        .compose(v -> future(
                                promise -> client.request(GET, "/api/country/AR")
                                        .expect(status(OK.getCode()))
                                        .as(BodyCodec.json(Country.class))
                                        .send(context.succeeding(promise::complete)))
                        )
                        .compose(v -> future(
                                promise -> client.request(POST, "/api/country")
                                        .expect(status(CONFLICT.getCode()))
                                        .as(BodyCodec.none())
                                        .sendJson(new Country("AR", "Arkansas", "Little Rock"),
                                                context.succeeding(promise::complete)))
                        )
                        .setHandler(context.succeeding(none ->
                                client.request(GET, "/api/country/AR")
                                        .expect(status(OK.getCode()))
                                        .as(BodyCodec.json(Country.class))
                                        .send(context.succeeding(response4 -> {
                                            context.verify(() -> {
                                                Country country = response4.body();
                                                assertThat(country.getId(), equalTo("AR"));
                                                assertThat(country.getName(), equalTo("Argentina"));
                                            });
                                            context.completeNow();
                                        })))));
    }

    @Test
    void testGetCountry(Vertx vertx, VertxTestContext context) {
        deployMainVerticle(vertx, context,
                id -> createCountry(context, new Country("US", "United States", "Washington, D.C."))
                        .setHandler(context.succeeding(none ->
                                client.request(GET, "/api/country/US")
                                        .expect(status(OK.getCode()))
                                        .as(BodyCodec.json(Country.class))
                                        .send(context.succeeding(response2 -> {
                                            context.verify(() -> {
                                                Country country = response2.body();
                                                assertThat(country.getId(), equalTo("US"));
                                                assertThat(country.getName(), equalTo("United States"));
                                            });
                                            context.completeNow();
                                        })))));
    }

    @Test
    void getGetCountryNotFound(Vertx vertx, VertxTestContext context) {
        deployMainVerticle(vertx, context,
                id -> client.request(GET, "/api/country/ZZ")
                        .expect(status(NOT_FOUND.getCode()))
                        .as(BodyCodec.none())
                        .send(context.succeeding(response1 -> context.completeNow())));
    }

    @Test
    void testSearchCountries(Vertx vertx, VertxTestContext context) {
        deployMainVerticle(vertx, context,
                id -> createCountry(context, new Country("BS", "Bahamas", "Nassau"))
                        .compose(v -> createCountry(context, new Country("TD", "The Republic of Chad", "N'Djamena")))
                        .compose(v -> createCountry(context, new Country("HR", "Croatia", "Zagreb")))
                        .setHandler(context.succeeding(r ->
                                client.request(GET, "/api/country")
                                        .expect(status(OK.getCode()))
                                        .as(BodyCodec.jsonArray())
                                        .send(context.succeeding(response -> {
                                            context.verify(() -> {
                                                MatcherAssert.assertThat(response.body().size(), greaterThan(2));
                                            });
                                            context.completeNow();
                                        })))));
    }

    @Test
    void testSearchCountriesNotFound(Vertx vertx, VertxTestContext context) {
        deployMainVerticle(vertx, context,
                id -> future((Promise<List<Country>> promise) ->
                        client.request(GET, "/api/country")
                                .as(BodyCodec.jsonArray())
                                .send(context.succeeding(response -> {
                                    if (response.body() != null) {
                                        promise.complete(response.body().stream()
                                                .map(JsonObject.class::cast)
                                                .map(obj -> new Country(obj.getString("id"), obj.getString("name"), obj.getString("capital", null)))
                                                .collect(Collectors.toList()));
                                    } else {
                                        promise.complete(Collections.emptyList());
                                    }
                                })))
                        .compose(countries -> CompositeFuture.all(
                                countries.stream().map(country -> deleteCountry(context, country.getId())).collect(Collectors.toList())
                        ))
                        .setHandler(none -> client.request(GET, "/api/country")
                                .expect(status(NO_CONTENT.getCode()))
                                .as(BodyCodec.none())
                                .send(context.succeeding(response -> context.completeNow()))));
    }

    @Test
    void testUpdateCountry(Vertx vertx, VertxTestContext context) {
        deployMainVerticle(vertx, context,
                id -> createCountry(context, new Country("XY", "XYZ", "ABC"))
                        .compose(v -> future(promise ->
                                client.request(PUT, "/api/country/XY")
                                        .expect(status(OK.getCode()))
                                        .as(BodyCodec.json(Country.class))
                                        .sendJson(new Country("XY", "Xylophone", "Keyboard"),
                                                context.succeeding(promise::complete))))
                        .setHandler(context.succeeding(result ->
                                client.request(GET, "/api/country/XY")
                                        .expect(status(OK.getCode()))
                                        .as(BodyCodec.json(Country.class))
                                        .send(context.succeeding(response -> {
                                            context.verify(() -> {
                                                Country country = response.body();
                                                assertThat(country.getId(), equalTo("XY"));
                                                assertThat(country.getName(), equalTo("Xylophone"));
                                            });
                                            context.completeNow();
                                        })))));
    }

    @Test
    void testDeleteCountry(Vertx vertx, VertxTestContext context) {
        deployMainVerticle(vertx, context,
                id -> createCountry(context, new Country("BT", "Bhutan", "Thimphu"))
                        .compose(v -> future(
                                promise -> client.request(GET, "/api/country/BT")
                                        .expect(status(OK.getCode()))
                                        .as(BodyCodec.json(Country.class))
                                        .send(context.succeeding(response -> promise.complete()))
                        ))
                        .compose(v -> future(
                                promise -> client.request(DELETE, "/api/country/BT")
                                        .expect(status(OK.getCode()))
                                        .as(BodyCodec.json(Country.class))
                                        .send(context.succeeding(response -> {
                                            context.verify(() -> {
                                                Country country = response.body();
                                                assertThat(country.getId(), equalTo("BT"));
                                                assertThat(country.getName(), equalTo("Bhutan"));
                                            });
                                            promise.complete();
                                        }))
                        ))
                        .setHandler(context.succeeding(
                                none -> client.request(GET, "/api/country/BT")
                                        .expect(status(NOT_FOUND.getCode()))
                                        .as(BodyCodec.none())
                                        .send(context.succeeding(response -> context.completeNow())))));
    }

    @Test
    void deleteCountryThatDoesNotExist(Vertx vertx, VertxTestContext context) {
        deployMainVerticle(vertx, context,
                id -> client.request(DELETE, "/api/country/ZZ")
                        .expect(status(NOT_FOUND.getCode()))
                        .as(BodyCodec.none())
                        .send(context.succeeding(response -> context.completeNow())));
    }

    private void deployMainVerticle(Vertx vertx, VertxTestContext context, Handler<String> handler) {
        vertx.deployVerticle(MainVerticle::new,
                new DeploymentOptions(),
                context.succeeding(handler));
    }

    private Future<Object> createCountry(VertxTestContext context, Country country) {
        return future(promise -> {
            client.request(POST, "/api/country")
                    .expect(status(CREATED.getCode()))
                    .as(BodyCodec.json(Country.class))
                    .sendJson(country, context.succeeding(promise::complete));
        });
    }

    private Future<Object> deleteCountry(VertxTestContext context, String countryId) {
        return future(
                promise -> client.request(DELETE, "/api/country/" + countryId)
                        .expect(status(OK.getCode()))
                        .as(BodyCodec.json(Country.class))
                        .send(context.succeeding(response -> promise.complete())));
    }
}
