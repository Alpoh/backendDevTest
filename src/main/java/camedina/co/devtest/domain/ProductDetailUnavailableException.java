package camedina.co.devtest.domain;

public class ProductDetailUnavailableException extends RuntimeException {

    public ProductDetailUnavailableException(String productId) {
        super("Product detail unavailable: " + productId);
    }
}
