package ru.practicum.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.main.dto.response.compilation.CompilationDto;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.model.Compilation;
import ru.practicum.main.repository.CompilationRepository;
import ru.practicum.main.service.interfaces.CompilationPublicService;

import java.util.Collections;
import java.util.List;

import static ru.practicum.main.dto.mappers.CompilationMapper.toDto;

@Service
@Slf4j
@RequiredArgsConstructor
public class CompilationPublicServiceImpl implements CompilationPublicService {
    private final CompilationRepository compilationRepository;

    @Override
    public List<CompilationDto> findAllByFilters(Boolean pinned, Pageable pageable) {
        log.info("запрос на поиск по фильтрам");
        Page<Compilation> compilations;
        if (pinned == null) {
            compilations = compilationRepository.findAll(pageable);
        } else {
            compilations = compilationRepository.findAllByPinned(pinned, pageable);
        }
        if (compilations.isEmpty()) {
            log.debug("по заданным фильтрам ничего не найдено");
            return Collections.emptyList();
        }
        return toDto(compilations.getContent());
    }

    @Override
    public CompilationDto findById(Long compilationId) {
        log.debug("поиск подборки с id {}", compilationId);
        return toDto(getById(compilationId));
    }

    private Compilation getById(Long compilationId) {
        return compilationRepository.findById(compilationId).orElseThrow(() ->
                new NotFoundException("подборка с id " + compilationId + " не найдена"));
    }
}
