package ru.practicum.main.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.main.dto.response.request.ParticipationRequestDto;
import ru.practicum.main.service.interfaces.RequestService;
import ru.practicum.stats.client.StatClient;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RequestController.class)
public class RequestControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    private StatClient statClient;

    @MockBean
    RequestService requestService;

    private static final Long REQUESTER_ID = 1L;

    private static final Long EVENT_ID = 1L;


    @Test
    public void testAddRequest_shouldSaveNewRequest() throws Exception {
        ParticipationRequestDto dto = ParticipationRequestDto
                .builder()
                .id(1L)
                .event(EVENT_ID)
                .requester(REQUESTER_ID)
                .created(LocalDateTime.now())
                .status("PENDING")
                .build();
        when(requestService.addRequest(REQUESTER_ID, EVENT_ID))
                .thenReturn(dto);

        mockMvc.perform(post("/users/{userId}/requests", REQUESTER_ID)
                        .param("eventId", EVENT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.requester").value(REQUESTER_ID))
                .andExpect(jsonPath("$.event").value(EVENT_ID));
    }

    @Test
    public void cancel_shouldReturnCancelledRequest() throws Exception {
        ParticipationRequestDto dto = ParticipationRequestDto
                .builder()
                .id(1L)
                .event(EVENT_ID)
                .requester(REQUESTER_ID)
                .created(LocalDateTime.now())
                .status("CANCEL")
                .build();

        when(requestService.cancel(REQUESTER_ID, 1L))
                .thenReturn(dto);

        mockMvc.perform(patch("/users/{userId}/requests/{requestId}/cancel", REQUESTER_ID, 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.requester").value(REQUESTER_ID))
                .andExpect(jsonPath("$.status").value(dto.getStatus()));
    }

    @Test
    public void findRequestsByRequesterId_shouldReturnRequestsOfUser() throws Exception {
        ParticipationRequestDto request1 = ParticipationRequestDto
                .builder()
                .id(1L)
                .event(EVENT_ID)
                .requester(REQUESTER_ID)
                .created(LocalDateTime.now())
                .status("PENDING")
                .build();

        ParticipationRequestDto request2 = ParticipationRequestDto
                .builder()
                .id(2L)
                .event(EVENT_ID)
                .requester(REQUESTER_ID)
                .created(LocalDateTime.now())
                .status("PENDING")
                .build();

        when(requestService.getRequestsByRequesterId(REQUESTER_ID))
                .thenReturn(List.of(request1, request2));

        mockMvc.perform(get("/users/{userId}/requests", REQUESTER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
