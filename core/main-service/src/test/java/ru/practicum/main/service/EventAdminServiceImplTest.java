package ru.practicum.main.service;

import com.querydsl.core.types.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.practicum.main.dto.request.event.SearchOfEventByAdminDto;
import ru.practicum.main.dto.request.event.StateAction;
import ru.practicum.main.dto.request.event.UpdateEventAdminRequest;
import ru.practicum.main.dto.response.event.EventFullDto;
import ru.practicum.main.dto.response.request.ConfirmedRequestsCountDto;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.model.*;
import ru.practicum.main.repository.CategoryRepository;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.LocationRepository;
import ru.practicum.main.repository.RequestRepository;
import ru.practicum.stats.client.StatClient;
import ru.practicum.stats.dto.dto.ViewStatsDto;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventAdminServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private StatClient statClient;

    @InjectMocks
    private EventAdminServiceImpl eventAdminService;

    private Category createCategory() {
        return Category.builder()
                .id(1L)
                .name("Test Category")
                .build();
    }

    private User createUser() {
        return User.builder()
                .id(1L)
                .name("Test User")
                .email("test@email.com")
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
                .annotation("Test annotation with sufficient length for validation")
                .description("Test description with sufficient length for validation")
                .category(createCategory())
                .initiator(createUser())
                .locationEntity(createLocation())
                .eventDate(LocalDateTime.now().plusDays(1))
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .state(Event.EventState.PUBLISHED)
                .createdOn(LocalDateTime.now())
                .build();
    }

    @Test
    void getEvents_whenValidSearchCriteria_thenReturnFilteredEvents() {
        // given
        SearchOfEventByAdminDto searchDto = SearchOfEventByAdminDto.builder()
                .users(List.of(1L))
                .states(List.of("PUBLISHED"))
                .categories(List.of(1L))
                .rangeStart(LocalDateTime.now().minusDays(1))
                .rangeEnd(LocalDateTime.now().plusDays(1))
                .build();

        Pageable pageable = Pageable.unpaged();

        Event event = createEvent();

        Page<Event> eventPage = new PageImpl<>(List.of(event));

        when(eventRepository.findAll(any(Predicate.class), eq(pageable))).thenReturn(eventPage);
        ConfirmedRequestsCountDto countDto = new ConfirmedRequestsCountDto(1L, 5L);

        when(requestRepository.countConfirmedRequestsByEventIds(List.of(1L))).thenReturn(List.of(countDto));

        // Mock statistics
        ViewStatsDto statsDto = new ViewStatsDto("app", "/events/1", 100L);
        ResponseEntity<List<ViewStatsDto>> statsResponse = ResponseEntity.ok(List.of(statsDto));
        when(statClient.getStats(any(), any(), anyList(),eq(true))).thenReturn(statsResponse);

        // when
        List<EventFullDto> result = eventAdminService.getEvents(searchDto, pageable);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getViews());
        assertEquals(5, result.get(0).getConfirmedRequests());

        verify(eventRepository).findAll(any(Predicate.class), eq(pageable));
        verify(statClient).getStats(any(), any(), anyList(), eq(true));
    }

    @Test
    void getEvents_whenEmptyResult_thenReturnEmptyList() {
        // given
        SearchOfEventByAdminDto searchDto = SearchOfEventByAdminDto.builder().build();
        Pageable pageable = Pageable.unpaged();

        Page<Event> emptyPage = new PageImpl<>(List.of());

        when(eventRepository.findAll(any(Predicate.class), eq(pageable))).thenReturn(emptyPage);

        // when
        List<EventFullDto> result = eventAdminService.getEvents(searchDto, pageable);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(eventRepository).findAll(any(Predicate.class), eq(pageable));
    }

    @Test
    void updateEvent_whenValidUpdate_thenReturnUpdatedEvent() {
        // given
        Long eventId = 1L;
        UpdateEventAdminRequest updateRequest = UpdateEventAdminRequest.builder()
                .title("Updated Title")
                .annotation("Updated annotation with sufficient length")
                .category(2L)
                .stateAction(StateAction.PUBLISH_EVENT)
                .eventDate(LocalDateTime.now().plusHours(2))
                .build();

        Event existingEvent = createEvent();
        existingEvent.setState(Event.EventState.PENDING);

        Category newCategory = Category.builder()
                .id(2L)
                .name("New Category")
                .build();

        Event savedEvent = createEvent();
        savedEvent.setTitle("Updated Title");
        savedEvent.setState(Event.EventState.PUBLISHED);
        savedEvent.setCategory(newCategory);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(newCategory));
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);
        when(requestRepository.countConfirmedRequestsByEventId(eventId)).thenReturn(10);

        ViewStatsDto statsDto = new ViewStatsDto("app", "/events/1", 50L);
        ResponseEntity<List<ViewStatsDto>> statsResponse = ResponseEntity.ok(List.of(statsDto));
        when(statClient.getStats(any(), any(), anyList(), anyBoolean())).thenReturn(statsResponse);

        // when
        EventFullDto result = eventAdminService.updateEvent(eventId, updateRequest);

        // then
        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
        assertEquals(50L, result.getViews());
        assertEquals(10, result.getConfirmedRequests());

        verify(eventRepository).findById(eventId);
        verify(categoryRepository).findById(2L);
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void updateEvent_whenEventNotFound_thenThrowNotFoundException() {
        // given
        Long eventId = 999L;
        UpdateEventAdminRequest updateRequest = UpdateEventAdminRequest.builder().build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(NotFoundException.class, () ->
                eventAdminService.updateEvent(eventId, updateRequest));

        verify(eventRepository).findById(eventId);
    }
}
