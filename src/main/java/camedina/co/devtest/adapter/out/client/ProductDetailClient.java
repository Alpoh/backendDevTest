package camedina.co.devtest.adapter.out.client;

import camedina.co.devtest.domain.FindProductDetail;
import camedina.co.devtest.domain.ProductDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
class ProductDetailClient implements FindProductDetail {

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
                .map(upstream -> new ProductDetail(upstream.id(), upstream.name(), upstream.price(), upstream.availability()));
    }
}
