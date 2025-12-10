package ru.practicum.main.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.practicum.main.dto.request.event.*;
import ru.practicum.main.dto.response.event.EventFullDto;
import ru.practicum.main.dto.response.event.EventRequestStatusUpdateResult;
import ru.practicum.main.dto.response.event.EventShortDto;
import ru.practicum.main.dto.response.request.ParticipationRequestDto;
import ru.practicum.main.model.Request;
import ru.practicum.main.service.interfaces.EventPrivateService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventPrivateControllerTest {

    @Mock
    private EventPrivateService eventPrivateService;

    @InjectMocks
    private EventPrivateController eventPrivateController;

    @Test
    void getEvents_whenCalled_thenReturnUserEvents() {
        Long userId = 1L;
        EventShortDto event = EventShortDto.builder().id(1L).title("Event").build();

        when(eventPrivateService.getEvents(anyLong(), any(Pageable.class)))
                .thenReturn(List.of(event));

        List<EventShortDto> result = eventPrivateController.getEvents(userId, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(eventPrivateService).getEvents(userId, PageRequest.of(0, 10));
    }

    @Test
    void addEvent_whenValidRequest_thenReturnCreatedEvent() {
        Long userId = 1L;
        NewEventDto newEventDto = NewEventDto.builder()
                .title("Title")
                .annotation("Valid annotation length more than 20 chars")
                .description("Valid description length more than 20 chars")
                .eventDate(LocalDateTime.now().plusDays(1))
                .category(1L)
                .build();

        EventFullDto createdEvent = EventFullDto.builder().id(1L).build();

        when(eventPrivateService.addEvent(anyLong(), any(NewEventDto.class)))
                .thenReturn(createdEvent);

        EventFullDto result = eventPrivateController.addEvent(userId, newEventDto);

        assertNotNull(result);
        verify(eventPrivateService).addEvent(userId, newEventDto);
    }

    @Test
    void getEvent_whenValidIds_thenReturnEvent() {
        Long userId = 1L;
        Long eventId = 1L;
        EventFullDto event = EventFullDto.builder().id(eventId).build();

        when(eventPrivateService.getEvent(anyLong(), anyLong()))
                .thenReturn(event);

        EventFullDto result = eventPrivateController.getEvent(userId, eventId);

        assertNotNull(result);
        assertEquals(eventId, result.getId());
        verify(eventPrivateService).getEvent(eventId, userId);
    }

    @Test
    void updateEvent_whenValidRequest_thenReturnUpdatedEvent() {
        Long userId = 1L;
        Long eventId = 1L;
        UpdateEventUserRequest updateRequest = UpdateEventUserRequest.builder()
                .title("Updated Title")
                .build();

        EventFullDto updatedEvent = EventFullDto.builder().id(eventId).build();

        when(eventPrivateService.updateEvent(any(UserIdAndEventIdDto.class), any(UpdateEventUserRequest.class)))
                .thenReturn(updatedEvent);

        EventFullDto result = eventPrivateController.updateEvent(userId, eventId, updateRequest);

        assertNotNull(result);
        verify(eventPrivateService).updateEvent(any(UserIdAndEventIdDto.class), any(UpdateEventUserRequest.class));
    }

    @Test
    void getRequests_whenValidIds_thenReturnRequests() {
        Long userId = 1L;
        Long eventId = 1L;
        ParticipationRequestDto request = ParticipationRequestDto.builder().id(1L).build();

        when(eventPrivateService.getRequests(anyLong(), anyLong()))
                .thenReturn(List.of(request));

        List<ParticipationRequestDto> result = eventPrivateController.getRequests(userId, eventId);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(eventPrivateService).getRequests(userId, eventId);
    }

    @Test
    void updateRequests_whenValidRequest_thenReturnUpdateResult() {
        Long userId = 1L;
        Long eventId = 1L;
        EventRequestStatusUpdateRequest updateRequest = EventRequestStatusUpdateRequest.builder()
                .requestIds(Set.of(1L))
                .status(Request.RequestStatus.CONFIRMED)
                .build();

        EventRequestStatusUpdateResult resultDto = EventRequestStatusUpdateResult.builder().build();

        when(eventPrivateService.updateRequests(any(UserIdAndEventIdDto.class), any(EventRequestStatusUpdateRequest.class)))
                .thenReturn(resultDto);

        EventRequestStatusUpdateResult result = eventPrivateController.updateRequests(userId, eventId, updateRequest);

        assertNotNull(result);
        verify(eventPrivateService).updateRequests(any(UserIdAndEventIdDto.class), any(EventRequestStatusUpdateRequest.class));
    }
}