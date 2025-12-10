package ru.practicum.main.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import ru.practicum.main.dto.request.event.SearchOfEventByPublicDto;
import ru.practicum.main.dto.request.event.SortOfEvent;
import ru.practicum.main.dto.response.event.EventFullDto;
import ru.practicum.main.dto.response.event.EventShortDto;
import ru.practicum.main.service.interfaces.EventPublicService;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventPublicControllerTest {

    @Mock
    private EventPublicService eventPublicService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private EventPublicController eventPublicController;

    @Test
    void getEvents_whenAllParameters_thenReturnFilteredEvents() {
        // given
        String text = "test";
        List<Long> categories = List.of(1L, 2L);
        Boolean paid = true;
        LocalDateTime rangeStart = LocalDateTime.now();
        LocalDateTime rangeEnd = LocalDateTime.now().plusDays(1);
        Boolean onlyAvailable = true;
        SortOfEvent sort = SortOfEvent.EVENT_DATE;
        Integer from = 0;
        Integer size = 10;

        EventShortDto event = EventShortDto.builder()
                .id(1L)
                .title("Test Event")
                .build();

        when(eventPublicService.getEvents(any(SearchOfEventByPublicDto.class), any(Pageable.class), any(HttpServletRequest.class)))
                .thenReturn(List.of(event));

        // when
        List<EventShortDto> result = eventPublicController.getEvents(
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size, httpServletRequest
        );

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(eventPublicService).getEvents(any(SearchOfEventByPublicDto.class), any(Pageable.class), any(HttpServletRequest.class));
    }

    @Test
    void getEvent_whenValidId_thenReturnEvent() {
        // given
        Long eventId = 1L;
        EventFullDto event = EventFullDto.builder()
                .id(eventId)
                .title("Test Event")
                .build();

        when(eventPublicService.getEvent(anyLong(), any(HttpServletRequest.class)))
                .thenReturn(event);

        // when
        EventFullDto result = eventPublicController.getEvent(eventId, httpServletRequest);

        // then
        assertNotNull(result);
        assertEquals(eventId, result.getId());
        verify(eventPublicService).getEvent(eventId, httpServletRequest);
    }
}