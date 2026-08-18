package camedina.co.devtest.adapter.in.web;

import camedina.co.devtest.domain.FindSimilarProducts;
import camedina.co.devtest.domain.ProductDetail;
import camedina.co.devtest.domain.ProductDetailUnavailableException;
import camedina.co.devtest.domain.ProductNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.mockito.Mockito.when;

@WebFluxTest(ProductSimilarController.class)
class ProductSimilarControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private FindSimilarProducts findSimilarProducts;

    @Test
    void returnsSimilarProductsForKnownProduct() {
        var product2 = new ProductDetail("2", "Product 2", 20.5, true);
        when(findSimilarProducts.findSimilarProducts("1"))
                .thenReturn(Flux.just(product2));

        webTestClient.get().uri("/product/1/similar")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductDetail.class)
                .isEqualTo(List.of(product2));
    }

    @Test
    void returnsNotFoundWhenProductDoesNotExist() {
        when(findSimilarProducts.findSimilarProducts("1"))
                .thenReturn(Flux.error(new ProductNotFoundException("1")));

        webTestClient.get().uri("/product/1/similar")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void returnsEmptyListWhenProductHasNoSimilarProducts() {
        when(findSimilarProducts.findSimilarProducts("1"))
                .thenReturn(Flux.empty());

        webTestClient.get().uri("/product/1/similar")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductDetail.class)
                .isEqualTo(List.of());
    }

    @Test
    void returnsBadGatewayWhenASimilarProductDetailIsUnavailable() {
        var product2 = new ProductDetail("2", "Product 2", 20.5, true);
        when(findSimilarProducts.findSimilarProducts("1"))
                .thenReturn(Flux.concat(Flux.just(product2), Flux.error(new ProductDetailUnavailableException("3"))));

        webTestClient.get().uri("/product/1/similar")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_GATEWAY);
    }
}
