package camedina.co.devtest.domain;

import reactor.core.publisher.Flux;

public interface ObtainSimilarIds {
    Flux<String> obtainSimilarIds(String productId);
}
