package ru.practicum.request.client.event;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.request.dto.response.event.EventDto;


@FeignClient(name = "main-service", fallback = EventServiceClientFallback.class)
public interface EventClient {

    @GetMapping("/internal/events/{eventId}")
    EventDto getEventById(@PathVariable("eventId") Long eventId);

    //TODO Здесь надо будет поменять main-service на event-service
}
