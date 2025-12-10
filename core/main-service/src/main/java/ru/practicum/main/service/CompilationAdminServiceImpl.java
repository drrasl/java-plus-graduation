package ru.practicum.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.dto.request.compilation.NewCompilationDto;
import ru.practicum.main.dto.request.compilation.UpdateCompilationRequest;
import ru.practicum.main.dto.response.compilation.CompilationDto;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.model.Compilation;
import ru.practicum.main.model.Event;
import ru.practicum.main.repository.CompilationRepository;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.service.interfaces.CompilationAdminService;
import ru.practicum.main.util.Updater;

import java.util.Set;

import static ru.practicum.main.dto.mappers.CompilationMapper.toDto;
import static ru.practicum.main.dto.mappers.CompilationMapper.toEntity;


@Service
@Slf4j
@RequiredArgsConstructor
public class CompilationAdminServiceImpl implements CompilationAdminService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CompilationDto add(NewCompilationDto newCompilation) {
        log.debug("добавление новой подборки{}", newCompilation);
        Set<Event> events = eventRepository.findAllByIdIn(newCompilation.getEvents());
        Compilation compilation = toEntity(newCompilation, events);
        return toDto(compilationRepository.save(compilation));
    }

    @Override
    @Transactional
    public void deleteById(Long compilationId) {
        log.debug("удаление подборки с id{}", compilationId);
        compilationRepository.deleteById(compilationId);
    }

    @Override
    @Transactional
    public CompilationDto update(Long compilationId, UpdateCompilationRequest updatedCompilation) {
        log.debug("обновление подборки с id{}", compilationId);
        Compilation oldCompilation = getById(compilationId);
        Updater.update(updatedCompilation.getEvents(), () -> oldCompilation.setEvents(eventRepository.findAllByIdIn(updatedCompilation.getEvents())));
        Updater.update(updatedCompilation.getTitle(), () -> oldCompilation.setTitle(updatedCompilation.getTitle()));
        Updater.update(updatedCompilation.getPinned(), () -> oldCompilation.setPinned(updatedCompilation.getPinned()));
        log.info("обновленная подборка{}", updatedCompilation);
        return toDto(compilationRepository.save(oldCompilation));
    }

    private Compilation getById(Long compilationId) {
        return compilationRepository.findById(compilationId).orElseThrow(() ->
                new NotFoundException("подборка с id " + compilationId + " не найдена"));
    }
}
