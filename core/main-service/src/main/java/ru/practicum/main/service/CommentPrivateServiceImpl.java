package ru.practicum.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.dto.mappers.CommentMapper;
import ru.practicum.main.dto.request.comment.NewCommentDto;
import ru.practicum.main.dto.response.comment.CommentDto;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.model.Comment;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.User;
import ru.practicum.main.repository.CommentRepository;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.UserRepository;
import ru.practicum.main.service.interfaces.CommentPrivateService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static ru.practicum.main.dto.mappers.CommentMapper.toEntity;
import static ru.practicum.main.dto.mappers.CommentMapper.toDto;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CommentPrivateServiceImpl implements CommentPrivateService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long eventId, NewCommentDto newCommentDto) {
        log.info("Пользователь с ID: {} добавляет комментарий к событию с ID: {}", userId, eventId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("User with id=%s was not found", userId)));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Event with id=%s was not found", eventId)));

        Comment comment = toEntity(newCommentDto);
        comment.setUser(user);
        comment.setEvent(event);
        comment.setCreatedOn(LocalDateTime.now());
        comment.setUpdatedOn(LocalDateTime.now()); // Установим initial timestamp

        Comment savedComment = commentRepository.save(comment);
        log.info("Добавлен новый комментарий: {}", savedComment);

        return toDto(savedComment);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        log.info("Пользователь с ID: {} пытается удалить свой комментарий с ID: {}", userId, commentId);
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("User with id=%s was not found", userId)));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("Comment with id=%s was not found", commentId)));

        if (!comment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException(String.format("User with ID: %s is not the author of the comment with ID: %s", userId, commentId));
        }

        commentRepository.delete(comment);
        log.info("Комментарий с ID: {} удален", commentId);
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, NewCommentDto updateCommentDto) {
        log.info("Пользователь с ID: {} пытается обновить свой комментарий с ID: {}", userId, commentId);
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("User with id=%s was not found", userId)));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("Comment with id=%s was not found", commentId)));

        if (!comment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException(String.format("User with ID: %s is not the author of the comment with ID: %s", userId, commentId));
        }

        comment.setText(updateCommentDto.getText());
        comment.setUpdatedOn(LocalDateTime.now());
        Comment updatedComment = commentRepository.save(comment);

        log.info("Комментарий с ID: {} обновлен", commentId);
        return toDto(updatedComment);
    }

    @Override
    public List<CommentDto> getCommentsByUserId(Long userId, Pageable pageable) {
        log.info("Получение комментариев пользователя с ID: {}", userId);
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("User with id=%s was not found", userId)));

        return commentRepository.findByUserIdOrderByCreatedOnDesc(userId, pageable).stream()
                .map(CommentMapper::toDto)
                .collect(Collectors.toList());
    }
}