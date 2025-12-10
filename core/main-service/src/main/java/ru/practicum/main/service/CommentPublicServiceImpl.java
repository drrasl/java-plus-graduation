package ru.practicum.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.main.dto.mappers.CommentMapper;
import ru.practicum.main.dto.response.comment.CommentDto;
import ru.practicum.main.repository.CommentRepository;
import ru.practicum.main.service.interfaces.CommentPublicService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentPublicServiceImpl implements CommentPublicService {

    private final CommentRepository commentRepository;

    @Override
    public List<CommentDto> getCommentsByEventId(Long eventId, Pageable pageable) {
        log.info("Получение комментариев для события с ID: {}", eventId);
        return commentRepository.findByEventIdOrderByCreatedOnDesc(eventId, pageable)
                .stream()
                .map(CommentMapper::toDto)
                .collect(Collectors.toList());
    }
}