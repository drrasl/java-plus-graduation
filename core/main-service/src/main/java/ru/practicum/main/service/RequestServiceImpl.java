package ru.practicum.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.main.dto.response.request.ParticipationRequestDto;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.exception.OwnershipMismatchException;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.Request;
import ru.practicum.main.model.User;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.RequestRepository;
import ru.practicum.main.repository.UserRepository;
import ru.practicum.main.service.interfaces.RequestService;

import java.time.LocalDateTime;
import java.util.List;

import static ru.practicum.main.dto.mappers.RequestMapper.toDto;
import static ru.practicum.main.dto.mappers.RequestMapper.toParticipationRequestDto;

@Service
@Slf4j
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;

    @Override
    public List<ParticipationRequestDto> getRequestsByRequesterId(Long userId) {
        return toDto(requestRepository.findAllByRequesterId(userId));
    }

    @Override
    public ParticipationRequestDto cancel(Long userId, Long requestId) {
        getUserById(userId);
        Request request = getRequestById(requestId);
        if (!request.getRequester().getId().equals(userId)) {
            throw new OwnershipMismatchException("пользователь " + userId + " не отправлял запрос " + request.getId());
        }
        request.setStatus(Request.RequestStatus.CANCELED);
        return toParticipationRequestDto(requestRepository.save(request));
    }

    @Override
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        if (eventId == null) {
            throw new IllegalArgumentException("параметр eventId обязателен");
        }
        Event event = getEventById(eventId);
        User user = getUserById(userId);
        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("запрос на участие в событии " + eventId + " уже создан");
        }
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException(userId + " является инициатором события");
        }
        if (event.getState() != Event.EventState.PUBLISHED) {
            throw new ConflictException("нельзя участвовать в неопубликованном событии");
        }
//        if (requestRepository.countByEvent_Id(eventId) >= event.getParticipantLimit() && event.getParticipantLimit() != 0) {
//            throw new ConflictException("достигнут лимит запросов на участие");
//        }
        if (!event.getRequestModeration()) {
            int confirmedRequests = requestRepository.countConfirmedRequestsByEventId(eventId);
            if (confirmedRequests >= event.getParticipantLimit() && event.getParticipantLimit() != 0) {
                throw new ConflictException("Достигнут лимит подтверждённых участников");
            }
        }
        Request request = Request
                .builder()
                .requester(user)
                .event(event)
                .created(LocalDateTime.now())
                .status(event.getRequestModeration() ? Request.RequestStatus.PENDING : Request.RequestStatus.CONFIRMED)
                .build();
        if (event.getParticipantLimit() == 0) {
            request.setStatus(Request.RequestStatus.CONFIRMED);
        }
        return toParticipationRequestDto(requestRepository.save(request));
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(() ->
                new NotFoundException("пользователь с id " + userId + " не найден"));
    }

    private Event getEventById(Long eventId) {
        return eventRepository.findById(eventId).orElseThrow(() ->
                new NotFoundException("событие с id " + eventId + " не найдено"));
    }

    private Request getRequestById(Long requestId) {
        return requestRepository.findById(requestId).orElseThrow(() ->
                new NotFoundException("запрос с id " + requestId + " не найден"));
    }


}
