package ru.practicum.main.dto.mappers;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.main.dto.request.compilation.NewCompilationDto;
import ru.practicum.main.dto.response.compilation.CompilationDto;
import ru.practicum.main.model.Compilation;
import ru.practicum.main.model.Event;

import java.util.List;
import java.util.Set;

import static ru.practicum.main.dto.mappers.EventMapper.toEventShortDto;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CompilationMapper {

    public static Compilation toEntity(NewCompilationDto newCompilation, Set<Event> events) {
        return Compilation
                .builder()
                .title(newCompilation.getTitle())
                .events(events)
                .pinned(newCompilation.getPinned())
                .build();
    }

    public static CompilationDto toDto(Compilation compilation) {
        return CompilationDto
                .builder()
                .events(toEventShortDto(compilation.getEvents()))
                .id(compilation.getId())
                .pinned(compilation.getPinned())
                .title(compilation.getTitle())
                .build();
    }

    public static List<CompilationDto> toDto(List<Compilation> compilations) {
        return compilations.stream().map(CompilationMapper::toDto).toList();
    }
}
