package pl.cwtwcz.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // Można tu ustawić domyślne timeouty, interceptory itp.
        return builder
                .setConnectTimeout(Duration.ofSeconds(180))
                .setReadTimeout(Duration.ofSeconds(180))
                .build();
    }
} 