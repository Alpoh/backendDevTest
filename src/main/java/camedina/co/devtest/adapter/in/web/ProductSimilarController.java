package camedina.co.devtest.adapter.in.web;

import camedina.co.devtest.domain.FindSimilarProducts;
import camedina.co.devtest.domain.ProductDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
class ProductSimilarController {

    private final FindSimilarProducts findSimilarProducts;

    ProductSimilarController(FindSimilarProducts findSimilarProducts) {
        this.findSimilarProducts = findSimilarProducts;
    }

    @GetMapping("/product/{productId}/similar")
    Flux<ProductDetail> similarProducts(@PathVariable String productId) {
        return findSimilarProducts.findSimilarProducts(productId);
    }
}
