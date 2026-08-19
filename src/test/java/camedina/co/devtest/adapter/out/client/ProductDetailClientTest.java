package camedina.co.devtest.adapter.out.client;

import camedina.co.devtest.domain.ProductDetail;
import camedina.co.devtest.domain.ProductDetailUnavailableException;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;

class ProductDetailClientTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance().build();

    @Test
    void returnsProductDetailForKnownProduct() {
        wireMock.stubFor(get(urlEqualTo("/product/2"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"2\",\"name\":\"Product 2\",\"price\":20.5,\"availability\":true}")));

        var client = new ProductDetailClient(WebClient.create(wireMock.baseUrl()), CircuitBreaker.ofDefaults("test"));

        StepVerifier.create(client.findProductDetail("2"))
                .expectNext(new ProductDetail("2", "Product 2", 20.5, true))
                .verifyComplete();
    }

    @Test
    void raisesProductDetailUnavailableWhenUpstreamProductIs404() {
        wireMock.stubFor(get(urlEqualTo("/product/5"))
                .willReturn(aResponse().withStatus(404)));

        var client = new ProductDetailClient(WebClient.create(wireMock.baseUrl()), CircuitBreaker.ofDefaults("test"));

        StepVerifier.create(client.findProductDetail("5"))
                .expectError(ProductDetailUnavailableException.class)
                .verify();
    }

    @Test
    void raisesProductDetailUnavailableWhenUpstreamProductReturns500() {
        wireMock.stubFor(get(urlEqualTo("/product/6"))
                .willReturn(aResponse().withStatus(500)));

        var client = new ProductDetailClient(WebClient.create(wireMock.baseUrl()), CircuitBreaker.ofDefaults("test"));

        StepVerifier.create(client.findProductDetail("6"))
                .expectError(ProductDetailUnavailableException.class)
                .verify();
    }

    @Test
    void retriesOnceAndSucceedsWhenUpstreamProductRecoversAfterA500() {
        wireMock.stubFor(get(urlEqualTo("/product/7"))
                .inScenario("upstream-recovery")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("recovered"));

        wireMock.stubFor(get(urlEqualTo("/product/7"))
                .inScenario("upstream-recovery")
                .whenScenarioStateIs("recovered")
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"7\",\"name\":\"Product 7\",\"price\":7.5,\"availability\":true}")));

        var client = new ProductDetailClient(WebClient.create(wireMock.baseUrl()), CircuitBreaker.ofDefaults("test"));

        StepVerifier.create(client.findProductDetail("7"))
                .expectNext(new ProductDetail("7", "Product 7", 7.5, true))
                .verifyComplete();
    }

    @Test
    void raisesProductDetailUnavailableWhenUpstreamProductTimesOut() {
        wireMock.stubFor(get(urlEqualTo("/product/10000"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"10000\",\"name\":\"Leather jacket\",\"price\":89.99,\"availability\":true}")
                        .withFixedDelay(7000)));

        var client = new ProductDetailClient(WebClient.create(wireMock.baseUrl()), CircuitBreaker.ofDefaults("test"));

        StepVerifier.create(client.findProductDetail("10000"))
                .expectError(ProductDetailUnavailableException.class)
                .verify(Duration.ofSeconds(22));
    }

    @Test
    void opensCircuitAfterRepeatedFailuresAndFailsFastWithoutCallingUpstream() {
        wireMock.stubFor(get(urlEqualTo("/product/8"))
                .willReturn(aResponse().withStatus(500)));

        var circuitBreakerConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50f)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .build();
        var circuitBreaker = CircuitBreaker.of("test", circuitBreakerConfig);
        var client = new ProductDetailClient(WebClient.create(wireMock.baseUrl()), circuitBreaker);

        StepVerifier.create(client.findProductDetail("8"))
                .expectError(ProductDetailUnavailableException.class)
                .verify(Duration.ofSeconds(10));
        StepVerifier.create(client.findProductDetail("8"))
                .expectError(ProductDetailUnavailableException.class)
                .verify(Duration.ofSeconds(10));

        var requestsBeforeOpen = wireMock.getServeEvents().getServeEvents().size();

        StepVerifier.create(client.findProductDetail("8"))
                .expectError(ProductDetailUnavailableException.class)
                .verify(Duration.ofMillis(500));

        wireMock.verify(requestsBeforeOpen, getRequestedFor(urlEqualTo("/product/8")));
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }
}
