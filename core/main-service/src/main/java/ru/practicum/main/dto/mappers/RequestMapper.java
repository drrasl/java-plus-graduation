package ru.practicum.main.dto.mappers;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.main.dto.response.request.ParticipationRequestDto;
import ru.practicum.main.model.Request;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RequestMapper {

    public static ParticipationRequestDto toParticipationRequestDto(Request request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(request.getCreated())
                .event(request.getEvent().getId())
                .requester(request.getRequester().getId())
                .status(request.getStatus().name())
                .build();
    }

    public static List<ParticipationRequestDto> toDto(List<Request> requests) {
        return requests.stream().map(RequestMapper::toParticipationRequestDto).toList();
    }
}
