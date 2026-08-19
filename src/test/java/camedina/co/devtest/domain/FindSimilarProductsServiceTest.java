package camedina.co.devtest.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindSimilarProductsServiceTest {

    @Mock
    private ObtainSimilarIds obtainSimilarIds;

    @Mock
    private FindProductDetail findProductDetail;

    private FindSimilarProductsService service;

    @BeforeEach
    void setUp() {
        service = new FindSimilarProductsService(obtainSimilarIds, findProductDetail);
    }

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

    @Test
    void boundsConcurrentProductDetailLookupsPerRequest() {
        var ids = List.of("1", "2", "3", "4", "5", "6", "7");
        when(obtainSimilarIds.obtainSimilarIds("1")).thenReturn(Flux.fromIterable(ids));

        var activeCalls = new AtomicInteger();
        var maxObservedConcurrency = new AtomicInteger();

        when(findProductDetail.findProductDetail(any())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            return Mono.fromSupplier(() -> new ProductDetail(id, "Product " + id, 1.0, true))
                    .delayElement(Duration.ofMillis(50))
                    .doOnSubscribe(_ -> maxObservedConcurrency.updateAndGet(
                            current -> Math.max(current, activeCalls.incrementAndGet())))
                    .doFinally(_ -> activeCalls.decrementAndGet());
        });

        StepVerifier.create(service.findSimilarProducts("1"))
                .expectNextCount(ids.size())
                .verifyComplete();

        assertThat(maxObservedConcurrency.get()).isLessThanOrEqualTo(5);
    }
}
