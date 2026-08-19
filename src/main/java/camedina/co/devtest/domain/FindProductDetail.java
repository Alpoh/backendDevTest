package camedina.co.devtest.domain;

import reactor.core.publisher.Mono;

public interface FindProductDetail {
    Mono<ProductDetail> findProductDetail(String productId);
}
