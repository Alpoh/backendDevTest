package camedina.co.devtest.adapter.out.client;

import camedina.co.devtest.domain.ProductDetail;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

class ProductDetailClientTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance().build();

    @Test
    void returnsProductDetailForKnownProduct() {
        wireMock.stubFor(get(urlEqualTo("/product/2"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"2\",\"name\":\"Product 2\",\"price\":20.5,\"availability\":true}")));

        var client = new ProductDetailClient(WebClient.create(wireMock.baseUrl()));

        StepVerifier.create(client.findProductDetail("2"))
                .expectNext(new ProductDetail("2", "Product 2", 20.5, true))
                .verifyComplete();
    }
}
