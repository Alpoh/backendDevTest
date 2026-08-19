package camedina.co.devtest.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Configuration
class WebClientConfig {

    private static final int EXPECTED_CONCURRENT_REQUESTS = 200;
    private static final int MAX_CONCURRENT_UPSTREAM_CALLS_PER_REQUEST = 5;
    private static final int UPSTREAM_MAX_CONNECTIONS =
            EXPECTED_CONCURRENT_REQUESTS * MAX_CONCURRENT_UPSTREAM_CALLS_PER_REQUEST;

    @Bean
    WebClient upstreamWebClient(Builder builder, @Value("${upstream.base-url}") String baseUrl) {
        var connectionProvider = ConnectionProvider.builder("upstream")
                .maxConnections(UPSTREAM_MAX_CONNECTIONS)
                .build();
        var httpClient = HttpClient.create(connectionProvider);

        return builder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
