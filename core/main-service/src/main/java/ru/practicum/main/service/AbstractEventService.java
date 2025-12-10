package ru.practicum.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import ru.practicum.main.dto.response.request.ConfirmedRequestsCountDto;
import ru.practicum.main.model.Event;
import ru.practicum.main.repository.RequestRepository;
import ru.practicum.stats.client.StatClient;
import ru.practicum.stats.dto.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractEventService {

    protected final RequestRepository requestRepository;
    protected final StatClient statClient;

    protected Map<Long, Long> getEventsViews(List<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> uris = events.stream()
                .map(event -> "/events/" + event.getId())
                .collect(Collectors.toList());

        LocalDateTime start = LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.now();

        try {
            ResponseEntity<List<ViewStatsDto>> response = statClient.getStats(start, end, uris, true);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().stream()
                        .collect(Collectors.toMap(
                                stats -> extractEventIdFromUri(stats.getUri()),
                                ViewStatsDto::getHits
                        ));
            }
        } catch (Exception e) {
            log.warn("Ошибка при получении статистики просмотров: {}", e.getMessage());
        }

        return events.stream()
                .collect(Collectors.toMap(Event::getId, event -> 0L));
    }

    protected Long extractEventIdFromUri(String uri) {
        try {
            return Long.parseLong(uri.replace("/events/", ""));
        } catch (NumberFormatException e) {
            log.warn("Не удалось извлечь ID события из URI: {}", uri);
            return 0L;
        }
    }

    protected Long getEventViews(Long eventId) {
        try {
            LocalDateTime start = LocalDateTime.of(2000, 1, 1, 0, 0);
            LocalDateTime end = LocalDateTime.now();
            List<String> uris = List.of("/events/" + eventId);

            ResponseEntity<List<ViewStatsDto>> response = statClient.getStats(start, end, uris, true);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().stream()
                        .findFirst()
                        .map(ViewStatsDto::getHits)
                        .orElse(0L);
            }
        } catch (Exception e) {
            log.warn("Ошибка при получении статистики просмотров для события {}: {}", eventId, e.getMessage());
        }
        return 0L;
    }

    protected Map<Long, Integer> getConfirmedRequests(List<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();
        List<ConfirmedRequestsCountDto> results = requestRepository.countConfirmedRequestsByEventIds(eventIds);
        Map<Long, Long> confirmedRequestsMap = results.stream()
                .collect(Collectors.toMap(
                        ConfirmedRequestsCountDto::getEventId,
                        ConfirmedRequestsCountDto::getCount
                ));
        return eventIds.stream()
                .collect(Collectors.toMap(
                        eventId -> eventId,
                        eventId -> confirmedRequestsMap.getOrDefault(eventId, 0L).intValue()
                ));
    }

    protected Integer getConfirmedRequests(Long eventId) {
        return requestRepository.countConfirmedRequestsByEventId(eventId);
    }
}
