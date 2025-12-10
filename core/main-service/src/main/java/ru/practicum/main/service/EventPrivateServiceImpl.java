package ru.practicum.main.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.dto.mappers.EventMapper;
import ru.practicum.main.dto.mappers.LocationMapper;
import ru.practicum.main.dto.mappers.RequestMapper;
import ru.practicum.main.dto.request.event.*;
import ru.practicum.main.dto.response.event.EventFullDto;
import ru.practicum.main.dto.response.event.EventRequestStatusUpdateResult;
import ru.practicum.main.dto.response.event.EventShortDto;
import ru.practicum.main.dto.response.request.ParticipationRequestDto;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.exception.ValidationException;
import ru.practicum.main.model.*;
import ru.practicum.main.repository.*;
import ru.practicum.main.service.interfaces.EventPrivateService;
import ru.practicum.stats.client.StatClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class EventPrivateServiceImpl extends AbstractEventService implements EventPrivateService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;

    public EventPrivateServiceImpl(RequestRepository requestRepository,
                                   StatClient statClient,
                                   EventRepository eventRepository,
                                   UserRepository userRepository,
                                   CategoryRepository categoryRepository,
                                   LocationRepository locationRepository) {
        super(requestRepository, statClient);
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.locationRepository = locationRepository;
    }

    @Override
    public List<EventShortDto> getEvents(Long userId, Pageable pageable) {
        validateUserExisted(userId);
        log.debug("Получаем события пользователя {} с пагинацией: {}", userId, pageable);
        Page<Event> eventsPage = eventRepository.findByInitiatorIdOrderByCreatedOnDesc(userId, pageable);
        if (eventsPage.isEmpty()) {
            log.debug("События для пользователя {} не найдены", userId);
            return Collections.emptyList();
        }
        List<Event> events = eventsPage.getContent();
        Map<Long, Long> views = getEventsViews(events);
        return events.stream()
                .map(event -> {
                    EventShortDto dto = EventMapper.toEventShortDto(event);
                    dto.setViews(views.getOrDefault(event.getId(), 0L));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto addEvent(Long userId, NewEventDto newEventDto) {
        log.debug("Создание события пользователем {}: {}", userId, newEventDto);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь c userId " + userId + " не найден"));
        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException(
                        "Категория c id " + newEventDto.getCategory() + " не найдена"));
        validateEventDate(newEventDto.getEventDate());
        LocationEntity locationEntity = LocationMapper.toLocation(newEventDto.getLocation());
        LocationEntity savedLocationEntity = locationRepository.save(locationEntity);
        Event event = EventMapper.toEventFromNewEventDto(newEventDto, user, category, savedLocationEntity);
        event.setConfirmedRequests(0);
        Event savedEvent = eventRepository.save(event);
        log.info("Событие создано успешно: ID {}", savedEvent.getId());
        EventFullDto result = EventMapper.toEventFullDto(savedEvent);
        result.setViews(0L);
        return result;
    }

    @Override
    public EventFullDto getEvent(Long eventId, Long userId) {
        validateUserExisted(userId);
        Event event = validateEventOfInitiator(eventId, userId);
        Integer confirmedRequests = requestRepository.countConfirmedRequestsByEventId(eventId);
        event.setConfirmedRequests(confirmedRequests);
        Long views = getEventViews(eventId);
        EventFullDto result = EventMapper.toEventFullDto(event);
        result.setViews(views);
        log.debug("Событие {} пользователя {} найдено", eventId, userId);
        return result;
    }

    @Override
    @Transactional
    public EventFullDto updateEvent(UserIdAndEventIdDto userIdAndEventIdDto, UpdateEventUserRequest updateEventUserRequest) {
        Long userId = userIdAndEventIdDto.getUserId();
        Long eventId = userIdAndEventIdDto.getEventId();
        log.debug("Обновление события {} пользователя {}: {}", eventId, userId, updateEventUserRequest);
        validateUserExisted(userId);
        Event event = validateEventOfInitiator(eventId, userId);
        validateEventCanBeUpdated(event);
        updateEventFields(event, updateEventUserRequest);
        if (updateEventUserRequest.getEventDate() != null) {
            validateEventDate(updateEventUserRequest.getEventDate());
            event.setEventDate(updateEventUserRequest.getEventDate());
        }
        if (updateEventUserRequest.getStateAction() != null) {
            processStateAction(event, updateEventUserRequest.getStateAction());
        }
        Integer confirmedRequests = requestRepository.countConfirmedRequestsByEventId(eventId);
        event.setConfirmedRequests(confirmedRequests);
        Event updatedEvent = eventRepository.save(event);
        Long views = getEventViews(eventId);
        EventFullDto result = EventMapper.toEventFullDto(updatedEvent);
        result.setViews(views);
        log.info("Событие {} пользователя {} успешно обновлено", eventId, userId);
        return result;
    }

    private void updateEventFields(Event event, UpdateEventUserRequest updateRequest) {
        if (updateRequest.getAnnotation() != null) {
            event.setAnnotation(updateRequest.getAnnotation());
        }
        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> new NotFoundException(
                            String.format("Категория с id=%d не найдена", updateRequest.getCategory())));
            event.setCategory(category);
        }
        if (updateRequest.getDescription() != null) {
            event.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getLocation() != null) {
            LocationEntity locationEntity = LocationMapper.toLocation(updateRequest.getLocation());
            LocationEntity savedLocation = locationRepository.save(locationEntity);
            event.setLocationEntity(savedLocation);
        }
        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }
        if (updateRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }
        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
        }
        if (updateRequest.getTitle() != null) {
            event.setTitle(updateRequest.getTitle());
        }
    }

    private void processStateAction(Event event, StateAction stateAction) {
        switch (stateAction) {
            case SEND_TO_REVIEW:
                event.setState(Event.EventState.PENDING);
                break;
            case CANCEL_REVIEW:
                event.setState(Event.EventState.CANCELED);
                break;
            default:
                throw new ValidationException("Неверное действие: " + stateAction);
        }
    }

    @Override
    public List<ParticipationRequestDto> getRequests(Long userId, Long eventId) {
        log.debug("Получение запросов на участие в событии {} пользователя {}", eventId, userId);
        validateUserExisted(userId);
        Event event = validateEventOfInitiator(eventId, userId);
        List<Request> requests = requestRepository.findAllByEventId(eventId);
        if (requests.isEmpty()) {
            log.debug("Запросы на участие в событии {} не найдены", eventId);
            return Collections.emptyList();
        }
        return requests.stream()
                .map(RequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequests(UserIdAndEventIdDto userIdAndEventIdDto, EventRequestStatusUpdateRequest updateRequest) {
        Long userId = userIdAndEventIdDto.getUserId();
        Long eventId = userIdAndEventIdDto.getEventId();
        log.debug("Обработка изменения статуса заявок для события {} пользователя {}: {}",
                eventId, userId, updateRequest);
        validateUserExisted(userId);
        Event event = validateEventOfInitiator(eventId, userId);
        if (!isModerationRequired(event)) {
            throw new ConflictException("Подтверждение заявок не требуется для этого события");
        }
        List<Request> requestsToProcess = getRequestsToProcess(updateRequest.getRequestIds().stream().toList(), eventId);
        validateRequestsCanBeProcessed(requestsToProcess, event, updateRequest.getStatus());
        EventRequestStatusUpdateResult result = processRequests(requestsToProcess, event, updateRequest.getStatus());
        log.info("Статусы заявок для события {} обновлены: подтверждено {}, отклонено {}",
                eventId, result.getConfirmedRequests().size(), result.getRejectedRequests().size());
        return result;
    }

    private void validateEventDate(LocalDateTime eventDate) {
        LocalDateTime minAllowedDate = LocalDateTime.now().plusHours(2);
        if (eventDate.isBefore(minAllowedDate)) {
            throw new ValidationException(
                    String.format("Дата события должна быть не раньше чем через 2 часа. " +
                                    "Текущее время: %s, указанное время: %s",
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            eventDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            );
        }
    }

    private void validateUserExisted(Long userId) {
        log.debug("Проверяем, что пользователь с userId {} существует", userId);
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь c userId " + userId + " не найден");
        }
    }

    private Event validateEventOfInitiator(Long eventId, Long userId) {
        log.debug("Проверяем, что событие {} создано пользователем с userId {}", eventId, userId);
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Событие с id=%d не найдено для пользователя с id=%d", eventId, userId)));
    }

    private void validateEventCanBeUpdated(Event event) {
        // Можно редактировать только отмененные события или события в состоянии ожидания модерации
        if (event.getState() != Event.EventState.PENDING && event.getState() != Event.EventState.CANCELED) {
            throw new ConflictException("Изменить можно только отмененные события или события в состоянии ожидания модерации");
        }
    }

    private boolean isModerationRequired(Event event) {
        // Модерация не требуется если:
        // 1. Лимит участников = 0 (безлимитно)
        // 2. Пре-модерация отключена
        return event.getParticipantLimit() != 0 && event.getRequestModeration();
    }

    private List<Request> getRequestsToProcess(List<Long> requestIds, Long eventId) {
        List<Request> requests = requestRepository.findAllByIdInAndEventId(requestIds, eventId);

        if (requests.size() != requestIds.size()) {
            throw new NotFoundException("Некоторые запросы не найдены или не принадлежат событию");
        }
        return requests;
    }

    private void validateRequestsCanBeProcessed(List<Request> requests, Event event, Request.RequestStatus newStatus) {
        // Проверяем что все запросы в состоянии PENDING
        requests.forEach(request -> {
            if (request.getStatus() != Request.RequestStatus.PENDING) {
                throw new ConflictException(
                        String.format("Запрос %d уже обработан (статус: %s)",
                                request.getId(), request.getStatus()));
            }
        });

        // Проверяем лимит участников для подтверждения
        if (newStatus == Request.RequestStatus.CONFIRMED) {
            int confirmedCount = requestRepository.countConfirmedRequestsByEventId(event.getId());
            int availableSlots = event.getParticipantLimit() - confirmedCount;

            if (availableSlots <= 0) {
                throw new ConflictException("Лимит участников для события исчерпан");
            }

            if (requests.size() > availableSlots) {
                throw new ConflictException(
                        String.format("Недостаточно свободных мест: доступно %d, запрошено %d",
                                availableSlots, requests.size()));
            }
        }
    }

    private EventRequestStatusUpdateResult processRequests(List<Request> requests, Event event,
                                                           Request.RequestStatus newStatus) {
        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();

        if (newStatus == Request.RequestStatus.CONFIRMED) {
            processConfirmation(requests, event, result);
        } else if (newStatus == Request.RequestStatus.REJECTED) {
            processRejection(requests, result);
        }

        return result;
    }

    private void processConfirmation(List<Request> requests, Event event,
                                     EventRequestStatusUpdateResult result) {
        int confirmedCount = requestRepository.countConfirmedRequestsByEventId(event.getId());
        int availableSlots = event.getParticipantLimit() - confirmedCount;

        // Подтверждаем сколько можем
        List<Request> toConfirm = requests.stream()
                .limit(availableSlots)
                .toList();

        // Отклоняем остальные (если лимит исчерпан)
        List<Request> toReject = requests.stream()
                .skip(availableSlots)
                .toList();

        // Подтверждаем запросы
        toConfirm.forEach(request -> {
            request.setStatus(Request.RequestStatus.CONFIRMED);
            requestRepository.save(request);
        });

        // Отклоняем запросы (если есть)
        if (!toReject.isEmpty()) {
            toReject.forEach(request -> {
                request.setStatus(Request.RequestStatus.REJECTED);
                requestRepository.save(request);
            });
        }

        // Заполняем результат
        result.setConfirmedRequests(toConfirm.stream()
                .map(RequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList()));

        result.setRejectedRequests(toReject.stream()
                .map(RequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList()));
    }

    private void processRejection(List<Request> requests, EventRequestStatusUpdateResult result) {
        requests.forEach(request -> {
            request.setStatus(Request.RequestStatus.REJECTED);
            requestRepository.save(request);
        });

        result.setRejectedRequests(requests.stream()
                .map(RequestMapper::toParticipationRequestDto)
                .collect(Collectors.toList()));

        result.setConfirmedRequests(Collections.emptyList());
    }
}
