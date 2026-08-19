package camedina.co.devtest.domain;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
class FindSimilarProductsService implements FindSimilarProducts {

    private static final int MAX_CONCURRENT_DETAIL_LOOKUPS = 5;

    private final ObtainSimilarIds obtainSimilarIds;
    private final FindProductDetail findProductDetail;

    FindSimilarProductsService(ObtainSimilarIds obtainSimilarIds, FindProductDetail findProductDetail) {
        this.obtainSimilarIds = obtainSimilarIds;
        this.findProductDetail = findProductDetail;
    }

    @Override
    public Flux<ProductDetail> findSimilarProducts(String productId) {
        return obtainSimilarIds.obtainSimilarIds(productId)
                .flatMapSequential(this::findProductDetailOrSkip, MAX_CONCURRENT_DETAIL_LOOKUPS);
    }

    private Mono<ProductDetail> findProductDetailOrSkip(String id) {
        return findProductDetail.findProductDetail(id)
                .onErrorResume(ProductDetailUnavailableException.class, _ -> Mono.empty());
    }
}
