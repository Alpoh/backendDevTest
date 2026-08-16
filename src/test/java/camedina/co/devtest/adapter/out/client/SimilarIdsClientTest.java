package camedina.co.devtest.adapter.out.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

class SimilarIdsClientTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance().build();

    @Test
    void returnsSimilarIdsInUpstreamOrder() {
        wireMock.stubFor(get(urlEqualTo("/product/1/similarids"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[\"2\",\"3\",\"4\"]")));

        var client = new SimilarIdsClient(WebClient.create(wireMock.baseUrl()));

        StepVerifier.create(client.obtainSimilarIds("1"))
                .expectNext("2", "3", "4")
                .verifyComplete();
    }
}
