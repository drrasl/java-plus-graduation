package ru.practicum.main.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.practicum.main.dto.mappers.EventMapper;
import ru.practicum.main.dto.request.event.SearchOfEventByPublicDto;
import ru.practicum.main.dto.response.event.EventFullDto;
import ru.practicum.main.dto.response.event.EventShortDto;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.exception.ValidationException;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.QEvent;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.RequestRepository;
import ru.practicum.main.service.interfaces.EventPublicService;
import ru.practicum.stats.client.StatClient;
import ru.practicum.stats.dto.dto.EndpointHitDto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional(readOnly = true)
public class EventPublicServiceImpl extends AbstractEventService implements EventPublicService {

    private final EventRepository eventRepository;

    public EventPublicServiceImpl(RequestRepository requestRepository,
                                  StatClient statClient,
                                  EventRepository eventRepository) {
        super(requestRepository, statClient);
        this.eventRepository = eventRepository;
    }

    @Override
    public List<EventShortDto> getEvents(SearchOfEventByPublicDto searchDto, Pageable pageable, HttpServletRequest request) {
        log.debug("Публичный поиск событий по критериям: {}", searchDto);
        if (searchDto.getRangeStart() != null
                && searchDto.getRangeEnd() != null
                && searchDto.getRangeEnd().isBefore(searchDto.getRangeStart())) {
            throw new ValidationException("Дата окончания события должна быть после даты начала");
        }
        Predicate predicate = buildPredicate(searchDto);
        Page<Event> eventsPage = eventRepository.findAll(predicate, pageable);
        if (eventsPage.isEmpty()) {
            log.debug("События по заданным критериям не найдены");
            // Все равно сохраняем hit даже если нет результатов
            saveHit(request, "/events");
            return Collections.emptyList();
        }
        List<Event> events = eventsPage.getContent();
        Map<Long, Long> views = getEventsViews(events);
        Map<Long, Integer> confirmedRequests = getConfirmedRequests(events);
        List<EventShortDto> result = events.stream()
                .map(event -> {
                    EventShortDto dto = EventMapper.toEventShortDto(event);
                    dto.setViews(views.getOrDefault(event.getId(), 0L));
                    dto.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0));
                    return dto;
                })
                .toList();
        saveHit(request, "/events");
        return result;
    }

    @Override
    public EventFullDto getEvent(Long id, HttpServletRequest request) {
        log.debug("Получение публичного события {}", id);
        Event event = eventRepository.findByIdAndState(id, Event.EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Событие с id=%d не было найдено или не опубликовано", id)));
        Long views = getEventViews(id);
        Integer confirmedRequests = requestRepository.countConfirmedRequestsByEventId(id);
        event.setConfirmedRequests(confirmedRequests);
        EventFullDto result = EventMapper.toEventFullDto(event);
        result.setViews(views);
        result.setConfirmedRequests(confirmedRequests);

        saveHit(request, "/events/" + id);
        log.debug("Событие {} найдено", id);
        return result;
    }

    private Predicate buildPredicate(SearchOfEventByPublicDto searchDto) {
        QEvent event = QEvent.event;
        BooleanBuilder predicate = new BooleanBuilder();

        // Только опубликованные события
        predicate.and(event.state.eq(Event.EventState.PUBLISHED));

        // Текст в аннотации или описании
        if (StringUtils.hasText(searchDto.getText())) {
            String text = searchDto.getText().toLowerCase();
            predicate.and(event.annotation.toLowerCase().contains(text)
                    .or(event.description.toLowerCase().contains(text)));
        }

        // Категории
        if (searchDto.getCategories() != null && !searchDto.getCategories().isEmpty()) {
            predicate.and(event.category.id.in(searchDto.getCategories()));
        }

        // Платные/бесплатные
        if (searchDto.getPaid() != null) {
            predicate.and(event.paid.eq(searchDto.getPaid()));
        }

        // Диапазон дат
        if (searchDto.getRangeStart() != null) {
            predicate.and(event.eventDate.goe(searchDto.getRangeStart()));
        }
        if (searchDto.getRangeEnd() != null) {
            predicate.and(event.eventDate.loe(searchDto.getRangeEnd()));
        }

        // Если не указан диапазон - только будущие события
        if (searchDto.getRangeStart() == null && searchDto.getRangeEnd() == null) {
            predicate.and(event.eventDate.after(LocalDateTime.now()));
        }

        // Только доступные (если требуется)
        if (Boolean.TRUE.equals(searchDto.getOnlyAvailable())) {
            predicate.and(event.participantLimit.eq(0)
                    .or(event.participantLimit.gt(event.confirmedRequests)));
        }

        return predicate;
    }

    private void saveHit(HttpServletRequest request, String uri) {
        try {
            String clientIp = request.getRemoteAddr();
            String requestUri = request.getRequestURI();

            log.info("Client IP: {}, Endpoint: {}", clientIp, requestUri);

            EndpointHitDto hitDto = EndpointHitDto.builder()
                    .app("ewm-main-service")
                    .uri(uri)
                    .ip(clientIp)
                    .timestamp(LocalDateTime.now())
                    .build();

            statClient.hit(hitDto);
            log.debug("Hit saved: {}", hitDto);

        } catch (Exception e) {
            log.warn("Ошибка при сохранении статистики: {}", e.getMessage());
        }
    }
}
