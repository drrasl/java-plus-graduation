package ru.practicum.main.service;

import com.google.protobuf.Timestamp;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.practicum.main.client.request.RequestClient;
import ru.practicum.main.client.user.UserClient;
import ru.practicum.main.dto.mappers.EventMapper;
import ru.practicum.main.dto.request.event.SearchOfEventByPublicDto;
import ru.practicum.main.dto.response.event.EventFullDto;
import ru.practicum.main.dto.response.event.EventShortDto;
import ru.practicum.main.dto.response.user.UserDto;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.exception.ValidationException;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.QEvent;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.service.interfaces.EventPublicService;
import ru.practicum.stats.client.CollectorClient;
import ru.practicum.stats.client.RecommendationsClient;
import ru.practicum.stats.proto.ActionTypeProto;
import ru.practicum.stats.proto.RecommendedEventProto;
import ru.practicum.stats.proto.UserActionProto;
import ru.practicum.stats.proto.UserPredictionsRequestProto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class EventPublicServiceImpl extends AbstractEventService implements EventPublicService {

    private final EventRepository eventRepository;

    private static final int MAX_RESULTS = 10; //ограничение количества мероприятий в результате выполнения запроса.

    public EventPublicServiceImpl(RequestClient requestClient,
                                  CollectorClient collectorClient,
                                  RecommendationsClient recommendationsClient,
                                  EventRepository eventRepository,
                                  UserClient userClient) {
        super(requestClient, collectorClient, recommendationsClient, userClient);
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
            return Collections.emptyList();
        }
        List<Event> events = eventsPage.getContent();
        Map<Long, UserDto> initiatorsMap = getInitiatorsMap(events);
        Map<Long, Double> ratings = getEventsRatings(events);
        Map<Long, Integer> confirmedRequests = getConfirmedRequests(events);
        List<EventShortDto> result = events.stream()
                .map(event -> {

                    UserDto userDto = initiatorsMap.get(event.getInitiatorId());
                    if (userDto == null) {
                        log.warn("Пользователь с ID {} не найден для события {}",
                                event.getInitiatorId(), event.getId());
                        throw new NotFoundException("Пользователь c userId " + event.getInitiatorId() + " не найден");
                    }

                    EventShortDto dto = EventMapper.toEventShortDto(event, userDto);
                    dto.setRating(ratings.get(event.getId()));
                    dto.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0));
                    return dto;
                })
                .toList();
        return result;
    }

    @Override
    public EventFullDto getEvent(Long id, Long userId, HttpServletRequest request) {
        collectorClient.sendUserAction(createUserAction(id, userId, ActionTypeProto.ACTION_VIEW));
        log.debug("Получение публичного события {}", id);
        Event event = eventRepository.findByIdAndState(id, Event.EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Событие с id=%d не было найдено или не опубликовано", id)));

        UserDto userDto = getUserById(event.getInitiatorId());
        Integer confirmedRequests = getConfirmedRequestsCount(id);
        event.setConfirmedRequests(confirmedRequests);
        EventFullDto result = EventMapper.toEventFullDto(event, userDto);
        result.setRating(getEventRating(id));
        result.setConfirmedRequests(confirmedRequests);

        log.debug("Событие {} найдено", id);
        return result;
    }

    @Override
    public List<EventShortDto> getRecommendations(Long userId) {
        log.debug("Получение рекомендаций для пользователя: {}", userId);
        List<RecommendedEventProto> recommendedEventProtos =  recommendationsClient.getRecommendationsForUser(
                UserPredictionsRequestProto.newBuilder()
                        .setUserId(userId)
                        .setMaxResults(MAX_RESULTS)
                        .build()
        );
        List<Long> eventIds = recommendedEventProtos.stream()
                .map(RecommendedEventProto::getEventId)
                .toList();
        List<Event> events = eventRepository.findAllById(eventIds);
        Map<Long, UserDto> initiatorsMap = getInitiatorsMap(events);
        Map<Long, Double> ratings = getEventsRatings(events);
        Map<Long, Integer> confirmedRequests = getConfirmedRequests(events);
        return events.stream()
                .map(event -> {

                    UserDto userDto = initiatorsMap.get(event.getInitiatorId());
                    if (userDto == null) {
                        log.warn("Пользователь с ID {} не найден для события {}",
                                event.getInitiatorId(), event.getId());
                        throw new NotFoundException("Пользователь c userId " + event.getInitiatorId() + " не найден");
                    }

                    EventShortDto dto = EventMapper.toEventShortDto(event, userDto);
                    dto.setRating(ratings.get(event.getId()));
                    dto.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0));
                    return dto;
                })
                .toList();
    }

    @Override
    public void like(Long eventId, Long userId) {
        if (!requestClient.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ValidationException("Пользователь " + userId + " не принимал участи в событии " + eventId);
        }
        collectorClient.sendUserAction(createUserAction(eventId, userId, ActionTypeProto.ACTION_LIKE));
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

    private Map<Long, UserDto> getInitiatorsMap(List<Event> events) {
        Set<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .collect(Collectors.toSet());

        return getUsersByIds(new ArrayList<>(initiatorIds));
    }

    UserActionProto createUserAction(Long eventId, Long userId, ActionTypeProto typeProto) {
        Instant timestamp = Instant.now();
        return UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(typeProto)
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(timestamp.getEpochSecond())
                        .setNanos(timestamp.getNano())
                        .build())
                .build();
    }
}
