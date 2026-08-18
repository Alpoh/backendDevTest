package camedina.co.devtest.adapter.in.web;

import camedina.co.devtest.domain.FindSimilarProducts;
import camedina.co.devtest.domain.ProductDetail;
import camedina.co.devtest.domain.ProductNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
class ProductSimilarController {

    private final FindSimilarProducts findSimilarProducts;

    ProductSimilarController(FindSimilarProducts findSimilarProducts) {
        this.findSimilarProducts = findSimilarProducts;
    }

    @GetMapping("/product/{productId}/similar")
    Mono<List<ProductDetail>> similarProducts(@PathVariable String productId) {
        return findSimilarProducts.findSimilarProducts(productId).collectList();
    }

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    void handleProductNotFound() {
    }
}
