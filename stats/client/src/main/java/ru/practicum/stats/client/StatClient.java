package ru.practicum.stats.client;

import ru.practicum.stats.dto.dto.EndpointHitDto;
import ru.practicum.stats.dto.dto.ViewStatsDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class StatClient {
    private final RestTemplate restTemplate;
    private static final String API_PREFIX = "/";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatClient(@Value("${stat-server.url}") String serverUrl, RestTemplateBuilder builder) {
        this.restTemplate = builder
                .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl + API_PREFIX))
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory())
                .build();
    }

    public ResponseEntity<Void> hit(EndpointHitDto hitDto) {
        return restTemplate.postForEntity("hit", hitDto, Void.class);
    }

    public ResponseEntity<List<ViewStatsDto>> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath("stats")
                .queryParam("start", start.format(DATE_TIME_FORMATTER)) //Было так: encodeValue(start.format(DATE_TIME_FORMATTER)))
                .queryParam("end", end.format(DATE_TIME_FORMATTER));    //Было так: encodeValue(start.format(DATE_TIME_FORMATTER)))

        if (uris != null && !uris.isEmpty()) {
            uriBuilder.queryParam("uris", uris.toArray());
        }

        if (unique != null) {
            uriBuilder.queryParam("unique", unique);
        }

        return restTemplate.exchange(
                uriBuilder.toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ViewStatsDto>>() {
                }
        );
    }
}
