package camedina.co.devtest.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;

@Configuration
class WebClientConfig {

    @Bean
    WebClient upstreamWebClient(Builder builder, @Value("${upstream.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}
