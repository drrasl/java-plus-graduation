package ru.practicum.main.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.practicum.main.dto.request.event.*;
import ru.practicum.main.dto.response.event.EventFullDto;
import ru.practicum.main.dto.response.event.EventRequestStatusUpdateResult;
import ru.practicum.main.dto.response.event.EventShortDto;
import ru.practicum.main.dto.response.request.ParticipationRequestDto;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.model.*;
import ru.practicum.main.repository.*;
import ru.practicum.stats.client.StatClient;
import ru.practicum.stats.dto.dto.ViewStatsDto;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventPrivateServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private StatClient statClient;

    @InjectMocks
    private EventPrivateServiceImpl eventPrivateService;

    private User createUser() {
        return User.builder()
                .id(1L)
                .name("Test User")
                .email("test@email.com")
                .build();
    }

    private Category createCategory() {
        return Category.builder()
                .id(1L)
                .name("Test Category")
                .build();
    }

    private LocationEntity createLocation() {
        return LocationEntity.builder()
                .id(1L)
                .lat(55.7558f)
                .lon(37.6173f)
                .build();
    }

    private Event createEvent() {
        return Event.builder()
                .id(1L)
                .title("Test Event")
                .annotation("Test annotation with sufficient length")
                .description("Test description with sufficient length")
                .category(createCategory())
                .initiator(createUser())
                .locationEntity(createLocation())
                .eventDate(LocalDateTime.now().plusDays(1))
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .state(Event.EventState.PENDING)
                .createdOn(LocalDateTime.now())
                .confirmedRequests(0)
                .build();
    }

    private Request createRequest() {
        return Request.builder()
                .id(1L)
                .event(createEvent())
                .requester(createUser())
                .status(Request.RequestStatus.PENDING)
                .created(LocalDateTime.now())
                .build();
    }

    @Test
    void getEvents_whenUserExists_thenReturnUserEvents() {
        // given
        Long userId = 1L;
        Pageable pageable = Pageable.unpaged();

        Event event = createEvent();
        Page<Event> eventPage = new PageImpl<>(List.of(event));

        when(userRepository.existsById(userId)).thenReturn(true);
        when(eventRepository.findByInitiatorIdOrderByCreatedOnDesc(userId, pageable)).thenReturn(eventPage);

        ViewStatsDto statsDto = new ViewStatsDto("app", "/events/1", 50L);
        ResponseEntity<List<ViewStatsDto>> statsResponse = ResponseEntity.ok(List.of(statsDto));
        when(statClient.getStats(any(), any(), anyList(), anyBoolean())).thenReturn(statsResponse);

        // when
        List<EventShortDto> result = eventPrivateService.getEvents(userId, pageable);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userRepository).existsById(userId);
        verify(eventRepository).findByInitiatorIdOrderByCreatedOnDesc(userId, pageable);
    }

    @Test
    void getEvents_whenUserNotExists_thenThrowNotFoundException() {
        // given
        Long userId = 999L;
        Pageable pageable = Pageable.unpaged();

        when(userRepository.existsById(userId)).thenReturn(false);

        // when & then
        assertThrows(NotFoundException.class, () ->
                eventPrivateService.getEvents(userId, pageable));

        verify(userRepository).existsById(userId);
    }

    @Test
    void addEvent_whenValidData_thenReturnCreatedEvent() {
        // given
        Long userId = 1L;
        NewEventDto newEventDto = NewEventDto.builder()
                .title("New Event")
                .annotation("Valid annotation length more than 20 chars")
                .description("Valid description length more than 20 chars")
                .category(1L)
                .eventDate(LocalDateTime.now().plusHours(3))
                .location(new ru.practicum.main.dto.Location(55.7558f, 37.6173f))
                .build();

        User user = createUser();
        Category category = createCategory();
        LocationEntity location = createLocation();
        Event event = createEvent();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(locationRepository.save(any(LocationEntity.class))).thenReturn(location);
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        // when
        EventFullDto result = eventPrivateService.addEvent(userId, newEventDto);

        // then
        assertNotNull(result);
        verify(userRepository).findById(userId);
        verify(categoryRepository).findById(1L);
        verify(locationRepository).save(any(LocationEntity.class));
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void getEvent_whenValidIds_thenReturnEvent() {
        // given
        Long userId = 1L;
        Long eventId = 1L;
        Event event = createEvent();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(eventRepository.findByIdAndInitiatorId(eventId, userId)).thenReturn(Optional.of(event));
        when(requestRepository.countConfirmedRequestsByEventId(eventId)).thenReturn(5);

        ViewStatsDto statsDto = new ViewStatsDto("app", "/events/1", 100L);
        ResponseEntity<List<ViewStatsDto>> statsResponse = ResponseEntity.ok(List.of(statsDto));
        when(statClient.getStats(any(), any(), anyList(), anyBoolean())).thenReturn(statsResponse);

        // when
        EventFullDto result = eventPrivateService.getEvent(eventId, userId);

        // then
        assertNotNull(result);
        assertEquals(100L, result.getViews());
        verify(userRepository).existsById(userId);
        verify(eventRepository).findByIdAndInitiatorId(eventId, userId);
    }

    @Test
    void updateEvent_whenValidUpdate_thenReturnUpdatedEvent() {
        // given
        UserIdAndEventIdDto ids = UserIdAndEventIdDto.builder()
                .userId(1L)
                .eventId(1L)
                .build();

        UpdateEventUserRequest updateRequest = UpdateEventUserRequest.builder()
                .title("Updated Title")
                .stateAction(StateAction.SEND_TO_REVIEW)
                .build();

        Event event = createEvent();
        event.setState(Event.EventState.CANCELED);

        when(userRepository.existsById(1L)).thenReturn(true);
        when(eventRepository.findByIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(requestRepository.countConfirmedRequestsByEventId(1L)).thenReturn(3);

        ViewStatsDto statsDto = new ViewStatsDto("app", "/events/1", 75L);
        ResponseEntity<List<ViewStatsDto>> statsResponse = ResponseEntity.ok(List.of(statsDto));
        when(statClient.getStats(any(), any(), anyList(), anyBoolean())).thenReturn(statsResponse);

        // when
        EventFullDto result = eventPrivateService.updateEvent(ids, updateRequest);

        // then
        assertNotNull(result);
        verify(userRepository).existsById(1L);
        verify(eventRepository).findByIdAndInitiatorId(1L, 1L);
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void getRequests_whenValidIds_thenReturnRequests() {
        // given
        Long userId = 1L;
        Long eventId = 1L;
        Request request = createRequest();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(eventRepository.findByIdAndInitiatorId(eventId, userId)).thenReturn(Optional.of(createEvent()));
        when(requestRepository.findAllByEventId(eventId)).thenReturn(List.of(request));

        // when
        List<ParticipationRequestDto> result = eventPrivateService.getRequests(userId, eventId);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(userRepository).existsById(userId);
        verify(eventRepository).findByIdAndInitiatorId(eventId, userId);
        verify(requestRepository).findAllByEventId(eventId);
    }

    @Test
    void updateRequests_whenValidConfirmation_thenReturnUpdateResult() {
        // given
        UserIdAndEventIdDto ids = UserIdAndEventIdDto.builder()
                .userId(1L)
                .eventId(1L)
                .build();

        EventRequestStatusUpdateRequest updateRequest = EventRequestStatusUpdateRequest.builder()
                .requestIds(Set.of(1L))
                .status(Request.RequestStatus.CONFIRMED)
                .build();

        Event event = createEvent();
        Request request = createRequest();

        when(userRepository.existsById(1L)).thenReturn(true);
        when(eventRepository.findByIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));
        when(requestRepository.findAllByIdInAndEventId(List.of(1L), 1L)).thenReturn(List.of(request));
        when(requestRepository.countConfirmedRequestsByEventId(1L)).thenReturn(0);
        when(requestRepository.save(any(Request.class))).thenReturn(request);

        // when
        EventRequestStatusUpdateResult result = eventPrivateService.updateRequests(ids, updateRequest);

        // then
        assertNotNull(result);
        assertEquals(1, result.getConfirmedRequests().size());
        verify(userRepository).existsById(1L);
        verify(eventRepository).findByIdAndInitiatorId(1L, 1L);
        verify(requestRepository).findAllByIdInAndEventId(List.of(1L), 1L);
    }
}