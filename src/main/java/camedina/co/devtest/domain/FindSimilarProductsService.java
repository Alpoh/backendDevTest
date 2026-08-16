package camedina.co.devtest.domain;

import reactor.core.publisher.Flux;

class FindSimilarProductsService implements FindSimilarProducts {

    private final ObtainSimilarIds obtainSimilarIds;
    private final FindProductDetail findProductDetail;

    FindSimilarProductsService(ObtainSimilarIds obtainSimilarIds, FindProductDetail findProductDetail) {
        this.obtainSimilarIds = obtainSimilarIds;
        this.findProductDetail = findProductDetail;
    }

    @Override
    public Flux<ProductDetail> findSimilarProducts(String productId) {
        return obtainSimilarIds.obtainSimilarIds(productId)
                .flatMapSequential(findProductDetail::findProductDetail);
    }
}
