package camedina.co.devtest.adapter.out.client;

import camedina.co.devtest.domain.FindProductDetail;
import camedina.co.devtest.domain.ProductDetail;
import camedina.co.devtest.domain.ProductDetailUnavailableException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException.InternalServerError;
import org.springframework.web.reactive.function.client.WebClientResponseException.NotFound;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Component
class ProductDetailClient implements FindProductDetail {

    private static final Duration UPSTREAM_TIMEOUT = Duration.ofSeconds(6);
    private static final int MAX_RETRIES = 2;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(1);

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
                .retryWhen(Retry.fixedDelay(MAX_RETRIES, RETRY_DELAY)
                        .filter(ProductDetailClient::isTransientUpstreamError)
                        .onRetryExhaustedThrow((_, signal) -> signal.failure()))
                .map(upstream -> new ProductDetail(upstream.id(), upstream.name(), upstream.price(), upstream.availability()))
                .onErrorMap(NotFound.class, _ -> new ProductDetailUnavailableException(productId))
                .onErrorMap(InternalServerError.class, _ -> new ProductDetailUnavailableException(productId))
                .onErrorMap(TimeoutException.class, _ -> new ProductDetailUnavailableException(productId));
    }

    private static boolean isTransientUpstreamError(Throwable throwable) {
        return throwable instanceof InternalServerError || throwable instanceof TimeoutException;
    }
}
