package camedina.co.devtest.adapter.out.client;

import camedina.co.devtest.domain.ObtainSimilarIds;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
class SimilarIdsClient implements ObtainSimilarIds {

    private final WebClient webClient;

    SimilarIdsClient(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public Flux<String> obtainSimilarIds(String productId) {
        return webClient.get()
                .uri("/product/{productId}/similarids", productId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
                .flatMapMany(Flux::fromIterable);
    }
}
