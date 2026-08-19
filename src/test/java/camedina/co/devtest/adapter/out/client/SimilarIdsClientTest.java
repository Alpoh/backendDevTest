package camedina.co.devtest.adapter.out.client;

import camedina.co.devtest.domain.ProductNotFoundException;
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
import static org.assertj.core.api.Assertions.assertThat;

class SimilarIdsClientTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance().build();

    @Test
    void returnsSimilarIdsInUpstreamOrder() {
        wireMock.stubFor(get(urlEqualTo("/product/1/similarids"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[\"2\",\"3\",\"4\"]")));

        var client = new SimilarIdsClient(WebClient.create(wireMock.baseUrl()), CircuitBreaker.ofDefaults("test"));

        StepVerifier.create(client.obtainSimilarIds("1"))
                .expectNext("2", "3", "4")
                .verifyComplete();
    }

    @Test
    void raisesProductNotFoundWhenUpstreamSimilarIdsIs404() {
        wireMock.stubFor(get(urlEqualTo("/product/1/similarids"))
                .willReturn(aResponse().withStatus(404)));

        var client = new SimilarIdsClient(WebClient.create(wireMock.baseUrl()), CircuitBreaker.ofDefaults("test"));

        StepVerifier.create(client.obtainSimilarIds("1"))
                .expectError(ProductNotFoundException.class)
                .verify();
    }

    @Test
    void returnsEmptyWhenUpstreamSimilarIdsIsAnEmptyList() {
        wireMock.stubFor(get(urlEqualTo("/product/1/similarids"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        var client = new SimilarIdsClient(WebClient.create(wireMock.baseUrl()), CircuitBreaker.ofDefaults("test"));

        StepVerifier.create(client.obtainSimilarIds("1"))
                .verifyComplete();
    }

    @Test
    void returnsEmptyWithoutCallingUpstreamWhenCircuitIsOpen() {
        var circuitBreaker = CircuitBreaker.ofDefaults("test");
        circuitBreaker.transitionToOpenState();

        var client = new SimilarIdsClient(WebClient.create(wireMock.baseUrl()), circuitBreaker);

        StepVerifier.create(client.obtainSimilarIds("1"))
                .verifyComplete();

        wireMock.verify(0, getRequestedFor(urlEqualTo("/product/1/similarids")));
    }

    @Test
    void opensCircuitAfterRepeatedFailuresAndStopsCallingUpstream() {
        wireMock.stubFor(get(urlEqualTo("/product/9/similarids"))
                .willReturn(aResponse().withStatus(500)));

        var circuitBreakerConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50f)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .build();
        var circuitBreaker = CircuitBreaker.of("test", circuitBreakerConfig);
        var client = new SimilarIdsClient(WebClient.create(wireMock.baseUrl()), circuitBreaker);

        StepVerifier.create(client.obtainSimilarIds("9"))
                .expectError()
                .verify();
        StepVerifier.create(client.obtainSimilarIds("9"))
                .expectError()
                .verify();

        var requestsBeforeOpen = wireMock.getServeEvents().getServeEvents().size();

        StepVerifier.create(client.obtainSimilarIds("9"))
                .verifyComplete();

        wireMock.verify(requestsBeforeOpen, getRequestedFor(urlEqualTo("/product/9/similarids")));
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }
}
