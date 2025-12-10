package ru.practicum.main.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import ru.practicum.main.dto.request.event.SearchOfEventByAdminDto;
import ru.practicum.main.dto.request.event.UpdateEventAdminRequest;
import ru.practicum.main.dto.response.event.EventFullDto;
import ru.practicum.main.model.Event;
import ru.practicum.main.service.interfaces.EventAdminService;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventAdminControllerTest {

    @Mock
    private EventAdminService eventAdminService;

    @InjectMocks
    private EventAdminController eventAdminController;

    @Test
    void getEvents_whenAllParametersProvided_thenReturnFilteredEvents() {
        // given
        List<Long> users = List.of(1L, 2L);
        List<String> states = List.of("PUBLISHED", "PENDING");
        List<Long> categories = List.of(1L, 3L);
        LocalDateTime rangeStart = LocalDateTime.now().minusDays(1);
        LocalDateTime rangeEnd = LocalDateTime.now().plusDays(1);
        Integer from = 0;
        Integer size = 10;

        EventFullDto event1 = EventFullDto.builder()
                .id(1L)
                .title("Event 1")
                .annotation("Annotation 1")
                .build();

        EventFullDto event2 = EventFullDto.builder()
                .id(2L)
                .title("Event 2")
                .annotation("Annotation 2")
                .build();

        List<EventFullDto> expectedEvents = List.of(event1, event2);

        when(eventAdminService.getEvents(any(SearchOfEventByAdminDto.class), any(Pageable.class)))
                .thenReturn(expectedEvents);

        // when
        List<EventFullDto> actualEvents = eventAdminController.getEvents(
                users, states, categories, rangeStart, rangeEnd, from, size
        );

        // then
        assertThat(actualEvents).hasSize(2);
        assertEquals(expectedEvents, actualEvents);
        verify(eventAdminService).getEvents(any(SearchOfEventByAdminDto.class), any(Pageable.class));
    }

    @Test
    void getEvents_whenNullParameters_thenReturnAllEvents() {
        // given
        Integer from = 0;
        Integer size = 10;

        EventFullDto event = EventFullDto.builder()
                .id(1L)
                .title("Default Event")
                .annotation("Default Annotation")
                .build();

        List<EventFullDto> expectedEvents = List.of(event);

        when(eventAdminService.getEvents(any(SearchOfEventByAdminDto.class), any(Pageable.class)))
                .thenReturn(expectedEvents);

        // when
        List<EventFullDto> actualEvents = eventAdminController.getEvents(
                null, null, null, null, null, from, size
        );

        // then
        assertThat(actualEvents).hasSize(1);
        assertEquals(expectedEvents, actualEvents);
        verify(eventAdminService).getEvents(any(SearchOfEventByAdminDto.class), any(Pageable.class));
    }

    @Test
    void getEvents_whenDifferentPagination_thenReturnPaginatedResults() {
        // given
        Integer from = 2;
        Integer size = 5;

        EventFullDto event1 = EventFullDto.builder().id(3L).title("Page 2 Event 1").build();
        EventFullDto event2 = EventFullDto.builder().id(4L).title("Page 2 Event 2").build();

        List<EventFullDto> expectedEvents = List.of(event1, event2);

        when(eventAdminService.getEvents(any(SearchOfEventByAdminDto.class), any(Pageable.class)))
                .thenReturn(expectedEvents);

        // when
        List<EventFullDto> actualEvents = eventAdminController.getEvents(
                null, null, null, null, null, from, size
        );

        // then
        assertThat(actualEvents).hasSize(2);
        assertEquals(expectedEvents, actualEvents);
        verify(eventAdminService).getEvents(any(SearchOfEventByAdminDto.class), any(Pageable.class));
    }

    @Test
    void updateEvent_whenValidRequest_thenReturnUpdatedEvent() {
        // given
        Long eventId = 1L;

        UpdateEventAdminRequest updateRequest = UpdateEventAdminRequest.builder()
                .title("Updated Title")
                .annotation("Updated annotation that is long enough")
                .description("Updated description that is also long enough to pass validation")
                .eventDate(LocalDateTime.now().plusDays(1))
                .paid(true)
                .participantLimit(50)
                .requestModeration(true)
                .build();

        EventFullDto updatedEvent = EventFullDto.builder()
                .id(eventId)
                .title("Updated Title")
                .annotation("Updated annotation that is long enough")
                .description("Updated description that is also long enough to pass validation")
                .eventDate(updateRequest.getEventDate())
                .paid(true)
                .participantLimit(50)
                .requestModeration(true)
                .state(Event.EventState.PUBLISHED)
                .build();

        when(eventAdminService.updateEvent(anyLong(), any(UpdateEventAdminRequest.class)))
                .thenReturn(updatedEvent);

        // when
        EventFullDto actualEvent = eventAdminController.updateEvent(eventId, updateRequest);

        // then
        assertNotNull(actualEvent);
        assertEquals(updatedEvent.getId(), actualEvent.getId());
        assertEquals(updatedEvent.getTitle(), actualEvent.getTitle());
        assertEquals(updatedEvent.getAnnotation(), actualEvent.getAnnotation());
        verify(eventAdminService).updateEvent(eventId, updateRequest);
    }

    @Test
    void updateEvent_whenPartialUpdate_thenReturnPartiallyUpdatedEvent() {
        // given
        Long eventId = 1L;

        UpdateEventAdminRequest updateRequest = UpdateEventAdminRequest.builder()
                .title("Only Title Updated")
                .build();

        EventFullDto updatedEvent = EventFullDto.builder()
                .id(eventId)
                .title("Only Title Updated")
                .annotation("Original annotation remains unchanged")
                .description("Original description remains unchanged")
                .eventDate(LocalDateTime.now().plusDays(2))
                .paid(false)
                .participantLimit(20)
                .requestModeration(true)
                .state(Event.EventState.PUBLISHED)
                .build();

        when(eventAdminService.updateEvent(anyLong(), any(UpdateEventAdminRequest.class)))
                .thenReturn(updatedEvent);

        // when
        EventFullDto actualEvent = eventAdminController.updateEvent(eventId, updateRequest);

        // then
        assertNotNull(actualEvent);
        assertEquals("Only Title Updated", actualEvent.getTitle());
        verify(eventAdminService).updateEvent(eventId, updateRequest);
    }
}