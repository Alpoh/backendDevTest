package camedina.co.devtest.domain;

import reactor.core.publisher.Flux;

public interface FindSimilarProducts {
    Flux<ProductDetail> findSimilarProducts(String productId);
}
