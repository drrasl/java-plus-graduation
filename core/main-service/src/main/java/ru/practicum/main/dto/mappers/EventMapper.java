package ru.practicum.main.dto.mappers;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.main.dto.request.event.NewEventDto;
import ru.practicum.main.dto.response.event.EventFullDto;
import ru.practicum.main.dto.response.event.EventShortDto;
import ru.practicum.main.model.Category;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.LocationEntity;
import ru.practicum.main.model.User;

import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EventMapper {
    public static EventShortDto toEventShortDto(Event event) {
        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(CategoryMapper.toDto(event.getCategory()))
                .confirmedRequests(event.getConfirmedRequests())
                .eventDate(event.getEventDate())
                .initiator(UserMapper.toUserShortDto(event.getInitiator()))
                .paid(event.getPaid())
                .title(event.getTitle())
                // views будет установлен отдельно в сервисе
                .build();
    }

    public static Set<EventShortDto> toEventShortDto(Set<Event> events) {
        return events.stream().map(EventMapper::toEventShortDto).collect(Collectors.toSet());
    }

    public static Event toEventFromNewEventDto(NewEventDto newEventDto, User user, Category category, LocationEntity savedLocationEntity) {
        return Event.builder()
                .annotation(newEventDto.getAnnotation())
                .category(category)
                .description(newEventDto.getDescription())
                .eventDate(newEventDto.getEventDate())
                .initiator(user)
                .locationEntity(savedLocationEntity)
                .paid(newEventDto.getPaid() != null ? newEventDto.getPaid() : false)
                .participantLimit(newEventDto.getParticipantLimit() != null ?
                        newEventDto.getParticipantLimit() : 0)
                .requestModeration(newEventDto.getRequestModeration() != null ?
                        newEventDto.getRequestModeration() : true)
                .title(newEventDto.getTitle())
                .state(Event.EventState.PENDING)
                .confirmedRequests(0)
                .build();
    }

    public static EventFullDto toEventFullDto(Event event) {
        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(CategoryMapper.toDto(event.getCategory()))
                .confirmedRequests(event.getConfirmedRequests())
                .createdOn(event.getCreatedOn())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .initiator(UserMapper.toUserShortDto(event.getInitiator()))
                .location(LocationMapper.toLocationDto(event.getLocationEntity()))
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn())
                .requestModeration(event.getRequestModeration())
                .state(event.getState())
                .title(event.getTitle())
                // views устанавливается отдельно в сервисе
                .build();
    }

}
