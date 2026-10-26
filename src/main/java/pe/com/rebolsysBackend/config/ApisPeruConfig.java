package pe.com.rebolsysBackend.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ApisPeruConfig {

    @Bean
    public RestClient apisPeruRestClient(
        @Value("${apis-peru.url:https://dniruc.apisperu.com/api/v1}") String url,
        @Value("${apis-peru.timeout-millis:8000}") int timeoutMillis
    ) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMillis)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(timeoutMillis));
        return RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }
}
