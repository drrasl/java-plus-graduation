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
import ru.practicum.main.dto.request.event.SearchOfEventByPublicDto;
import ru.practicum.main.dto.response.event.EventFullDto;
import ru.practicum.main.dto.response.event.EventShortDto;
import ru.practicum.main.dto.response.request.ConfirmedRequestsCountDto;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.exception.ValidationException;
import ru.practicum.main.model.*;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.RequestRepository;
import ru.practicum.stats.client.StatClient;
import ru.practicum.stats.dto.dto.EndpointHitDto;
import ru.practicum.stats.dto.dto.ViewStatsDto;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventPublicServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private StatClient statClient;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private EventPublicServiceImpl eventPublicService;

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

    private Event createPublishedEvent() {
        return Event.builder()
                .id(1L)
                .title("Published Event")
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
                .confirmedRequests(5)
                .build();
    }

    @Test
    void getEvents_whenValidSearch_thenReturnFilteredEvents() {
        // given
        SearchOfEventByPublicDto searchDto = SearchOfEventByPublicDto.builder()
                .text("test")
                .categories(List.of(1L))
                .paid(false)
                .rangeStart(LocalDateTime.now().minusDays(1))
                .rangeEnd(LocalDateTime.now().plusDays(2))
                .onlyAvailable(true)
                .build();

        Pageable pageable = Pageable.unpaged();
        Event event = createPublishedEvent();
        Page<Event> eventPage = new PageImpl<>(List.of(event));

        when(eventRepository.findAll(any(Predicate.class), eq(pageable))).thenReturn(eventPage);
        ConfirmedRequestsCountDto countDto = new ConfirmedRequestsCountDto(1L, 5L);
        when(requestRepository.countConfirmedRequestsByEventIds(List.of(1L))).thenReturn(List.of(countDto));

        ViewStatsDto statsDto = new ViewStatsDto("app", "/events/1", 100L);
        ResponseEntity<List<ViewStatsDto>> statsResponse = ResponseEntity.ok(List.of(statsDto));
        when(statClient.getStats(any(), any(), anyList(), anyBoolean())).thenReturn(statsResponse);
        when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.1");
        when(httpServletRequest.getRequestURI()).thenReturn("/events");

        // when
        List<EventShortDto> result = eventPublicService.getEvents(searchDto, pageable, httpServletRequest);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getViews());
        assertEquals(5, result.get(0).getConfirmedRequests());

        verify(eventRepository).findAll(any(Predicate.class), eq(pageable));
        verify(statClient).getStats(any(), any(), anyList(), eq(true));
        verify(statClient).hit(any(EndpointHitDto.class));
    }

    @Test
    void getEvents_whenInvalidDateRange_thenThrowValidationException() {
        // given
        SearchOfEventByPublicDto searchDto = SearchOfEventByPublicDto.builder()
                .rangeStart(LocalDateTime.now().plusDays(1))
                .rangeEnd(LocalDateTime.now().minusDays(1)) // Неправильный порядок
                .build();

        Pageable pageable = Pageable.unpaged();

        // when & then
        assertThrows(ValidationException.class, () ->
                eventPublicService.getEvents(searchDto, pageable, httpServletRequest));
    }

    @Test
    void getEvent_whenPublishedEventExists_thenReturnEvent() {
        // given
        Long eventId = 1L;
        Event event = createPublishedEvent();

        when(eventRepository.findByIdAndState(eventId, Event.EventState.PUBLISHED))
                .thenReturn(Optional.of(event));
        when(requestRepository.countConfirmedRequestsByEventId(eventId)).thenReturn(5);

        ViewStatsDto statsDto = new ViewStatsDto("app", "/events/1", 50L);
        ResponseEntity<List<ViewStatsDto>> statsResponse = ResponseEntity.ok(List.of(statsDto));
        when(statClient.getStats(any(), any(), anyList(), anyBoolean())).thenReturn(statsResponse);
        when(httpServletRequest.getRemoteAddr()).thenReturn("192.168.1.1");
        when(httpServletRequest.getRequestURI()).thenReturn("/events/1");

        // when
        EventFullDto result = eventPublicService.getEvent(eventId, httpServletRequest);

        // then
        assertNotNull(result);
        assertEquals(50L, result.getViews());
        assertEquals(5, result.getConfirmedRequests());

        verify(eventRepository).findByIdAndState(eventId, Event.EventState.PUBLISHED);
        verify(statClient).getStats(any(), any(), anyList(), eq(true));
        verify(statClient).hit(any(EndpointHitDto.class));
    }

    @Test
    void getEvent_whenEventNotPublished_thenThrowNotFoundException() {
        // given
        Long eventId = 999L;

        when(eventRepository.findByIdAndState(eventId, Event.EventState.PUBLISHED))
                .thenReturn(Optional.empty());

        // when & then
        assertThrows(NotFoundException.class, () ->
                eventPublicService.getEvent(eventId, httpServletRequest));

        verify(eventRepository).findByIdAndState(eventId, Event.EventState.PUBLISHED);
    }
}