package camedina.co.devtest.adapter.out.client;

import camedina.co.devtest.domain.FindProductDetail;
import camedina.co.devtest.domain.ProductDetail;
import camedina.co.devtest.domain.ProductDetailUnavailableException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException.InternalServerError;
import org.springframework.web.reactive.function.client.WebClientResponseException.NotFound;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Component
class ProductDetailClient implements FindProductDetail {

    private static final Duration UPSTREAM_TIMEOUT = Duration.ofSeconds(6);

    private final WebClient webClient;

    ProductDetailClient(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Mono<ProductDetail> findProductDetail(String productId) {
        return webClient.get()
                .uri("/product/{productId}", productId)
                .retrieve()
                .bodyToMono(UpstreamProductDetail.class)
                .timeout(UPSTREAM_TIMEOUT)
                .map(upstream -> new ProductDetail(upstream.id(), upstream.name(), upstream.price(), upstream.availability()))
                .onErrorMap(NotFound.class, _ -> new ProductDetailUnavailableException(productId))
                .onErrorMap(InternalServerError.class, _ -> new ProductDetailUnavailableException(productId))
                .onErrorMap(TimeoutException.class, _ -> new ProductDetailUnavailableException(productId));
    }
}
