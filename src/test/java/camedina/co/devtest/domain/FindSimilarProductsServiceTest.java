package camedina.co.devtest.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindSimilarProductsServiceTest {

    @Mock
    private ObtainSimilarIds obtainSimilarIds;

    @Mock
    private FindProductDetail findProductDetail;

    private final FindSimilarProductsService service =
            new FindSimilarProductsService(obtainSimilarIds, findProductDetail);

    @Test
    void returnsProductDetailsInSimilarityOrderEvenWhenTheyResolveOutOfOrder() {
        var second = new ProductDetail("2", "Product 2", 20.0, true);
        var third = new ProductDetail("3", "Product 3", 30.0, true);

        when(obtainSimilarIds.obtainSimilarIds("1")).thenReturn(Flux.just("2", "3"));
        when(findProductDetail.findProductDetail("2"))
                .thenReturn(Mono.just(second).delayElement(Duration.ofMillis(50)));
        when(findProductDetail.findProductDetail("3"))
                .thenReturn(Mono.just(third));

        StepVerifier.create(service.findSimilarProducts("1"))
                .expectNext(second, third)
                .verifyComplete();
    }
}
