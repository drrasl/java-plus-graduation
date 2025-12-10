package ru.practicum.main.service.interfaces;

import ru.practicum.main.dto.response.request.ParticipationRequestDto;

import java.util.List;

public interface RequestService {
    ParticipationRequestDto addRequest(Long userId, Long eventId);

    List<ParticipationRequestDto> getRequestsByRequesterId(Long userId);

    ParticipationRequestDto cancel(Long userId, Long requestId);


}
