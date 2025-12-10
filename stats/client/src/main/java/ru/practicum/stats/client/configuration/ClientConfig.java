package ru.practicum.stats.client.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.stats.client.StatClient;

@Configuration
public class ClientConfig {
    @Bean
    public StatClient statClient(@Value("${stat-server.url}") String serverUrl,
                                 RestTemplateBuilder builder) {
        return new StatClient(serverUrl, builder);
    }
}
