package ec.edu.epn.fis.aeis.rental.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Clientes internos hacia locker-service y auth-service (ver PLAN.md §6.2).
 * Timeouts cortos para que las fallas de red se detecten rápido y el
 * Circuit Breaker pueda actuar sin bloquear al usuario.
 */
@Configuration
public class RestClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    public RestClient lockerServiceRestClient(
            @Value("${locker-service.url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory())
                .build();
    }

    @Bean
    public RestClient authServiceRestClient(
            @Value("${auth-service.url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory())
                .build();
    }

    private ClientHttpRequestFactory requestFactory() {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(CONNECT_TIMEOUT)
                .withReadTimeout(READ_TIMEOUT);
        return ClientHttpRequestFactoryBuilder.detect().build(settings);
    }
}
