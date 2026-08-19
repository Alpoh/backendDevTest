package camedina.co.devtest.adapter.out.client;

import camedina.co.devtest.domain.ObtainSimilarIds;
import camedina.co.devtest.domain.ProductNotFoundException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException.NotFound;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
class SimilarIdsClient implements ObtainSimilarIds {

    private final WebClient webClient;
    private final CircuitBreaker similarIdsCircuitBreaker;

    SimilarIdsClient(WebClient webClient, CircuitBreaker similarIdsCircuitBreaker) {
        this.webClient = webClient;
        this.similarIdsCircuitBreaker = similarIdsCircuitBreaker;
    }

    @Override
    public Flux<String> obtainSimilarIds(String productId) {
        return webClient.get()
                .uri("/product/{productId}/similarids", productId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
                .transformDeferred(CircuitBreakerOperator.of(similarIdsCircuitBreaker))
                .flatMapMany(Flux::fromIterable)
                .onErrorMap(NotFound.class, ex -> new ProductNotFoundException(productId))
                .onErrorResume(CallNotPermittedException.class, _ -> Flux.empty());
    }
}
