package us.vanderlugt.sample.vertx.web.country;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import us.vanderlugt.sample.vertx.MainVerticle;
import us.vanderlugt.sample.vertx.model.country.Country;
import us.vanderlugt.sample.vertx.web.TestWebVerticle;

import static io.vertx.ext.web.client.predicate.ResponsePredicate.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static us.vanderlugt.sample.vertx.web.TestWebVerticle.WEB_CLIENT_OPTIONS;

@ExtendWith(VertxExtension.class)
public class TestCountryRouter {

    @Test
    void testSearchCountries(Vertx vertx, VertxTestContext context) {
        vertx.deployVerticle(MainVerticle::new,
                new DeploymentOptions(),
                context.succeeding(id -> {
                    WebClient client = WebClient.create(vertx, WEB_CLIENT_OPTIONS);
                    client.get("/api/country")
                            .expect(SC_OK)
                            .as(BodyCodec.jsonArray())
                            .send(context.succeeding(response -> {
                                context.verify(() -> {
                                    assertThat(response.body().size(), greaterThan(0));
                                    context.completeNow();
                                });
                            }));
                }));
    }

    @Test
    void testGetCountry(Vertx vertx, VertxTestContext context) {
        vertx.deployVerticle(MainVerticle::new,
                new DeploymentOptions(),
                context.succeeding(id -> {
                    WebClient client = WebClient.create(vertx, WEB_CLIENT_OPTIONS);
                    client.get("/api/country/US")
                            .expect(SC_OK)
                            .as(BodyCodec.json(Country.class))
                            .send(context.succeeding(response -> {
                                Country country = response.body();
                                context.verify(() -> {
                                    assertThat(country.getId(), equalTo("US"));
                                    assertThat(country.getName(), equalTo("United States"));
                                    context.completeNow();
                                });
                            }));
                }));
    }

    @Test
    void testCountryNotFound(Vertx vertx, VertxTestContext context) {
        vertx.deployVerticle(MainVerticle::new,
                new DeploymentOptions(),
                context.succeeding(id -> {
                    WebClient client = WebClient.create(vertx, WEB_CLIENT_OPTIONS);
                    client.get("/api/country/BR")
                            .expect(SC_NOT_FOUND)
                            .send(context.succeeding(response -> context.completeNow()));
                }));
    }

    @Test
    void testCreateCountry(Vertx vertx, VertxTestContext context) {
        vertx.deployVerticle(MainVerticle::new,
                new DeploymentOptions(),
                context.succeeding(id -> {
                    WebClient client = WebClient.create(vertx, WEB_CLIENT_OPTIONS);
                    client.post("/api/country")
                            .expect(SC_CREATED)
                            .sendJson(new Country("BR", "Brazil"),
                                    context.succeeding(response -> {
                                        context.verify(() -> {
                                            Country country = response.bodyAsJson(Country.class);
                                            assertThat(country.getId(), equalTo("BR"));
                                            assertThat(country.getName(), equalTo("Brazil"));
                                        });
                                        context.completeNow();
                                    }));
                }));
    }
}
