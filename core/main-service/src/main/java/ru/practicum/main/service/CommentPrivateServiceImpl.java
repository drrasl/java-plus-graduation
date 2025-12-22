package ru.practicum.main.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.client.user.UserClient;
import ru.practicum.main.dto.mappers.CommentMapper;
import ru.practicum.main.dto.request.comment.NewCommentDto;
import ru.practicum.main.dto.response.comment.CommentDto;
import ru.practicum.main.dto.response.user.UserDto;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.model.Comment;
import ru.practicum.main.model.Event;
import ru.practicum.main.repository.CommentRepository;
import ru.practicum.main.repository.EventRepository;
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
    private final UserClient userClient;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long eventId, NewCommentDto newCommentDto) {
        log.info("Пользователь с ID: {} добавляет комментарий к событию с ID: {}", userId, eventId);
        UserDto user = getUserById(userId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Event with id=%s was not found", eventId)));

        Comment comment = toEntity(newCommentDto);
        comment.setUserId(userId);
        comment.setEvent(event);
        comment.setCreatedOn(LocalDateTime.now());
        comment.setUpdatedOn(LocalDateTime.now()); // Установим initial timestamp

        Comment savedComment = commentRepository.save(comment);
        log.info("Добавлен новый комментарий: {}", savedComment);

        return toDto(savedComment, user);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        log.info("Пользователь с ID: {} пытается удалить свой комментарий с ID: {}", userId, commentId);
        getUserById(userId);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("Comment with id=%s was not found", commentId)));

        if (!comment.getUserId().equals(userId)) {
            throw new IllegalArgumentException(String.format("User with ID: %s is not the author of the comment with ID: %s", userId, commentId));
        }

        commentRepository.delete(comment);
        log.info("Комментарий с ID: {} удален", commentId);
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, NewCommentDto updateCommentDto) {
        log.info("Пользователь с ID: {} пытается обновить свой комментарий с ID: {}", userId, commentId);
        UserDto user = getUserById(userId);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException(String.format("Comment with id=%s was not found", commentId)));

        if (!comment.getUserId().equals(userId)) {
            throw new IllegalArgumentException(String.format("User with ID: %s is not the author of the comment with ID: %s", userId, commentId));
        }

        comment.setText(updateCommentDto.getText());
        comment.setUpdatedOn(LocalDateTime.now());
        Comment updatedComment = commentRepository.save(comment);

        log.info("Комментарий с ID: {} обновлен", commentId);
        return toDto(updatedComment, user);
    }

    @Override
    public List<CommentDto> getCommentsByUserId(Long userId, Pageable pageable) {
        log.info("Получение комментариев пользователя с ID: {}", userId);
        UserDto user = getUserById(userId);
        return commentRepository.findByUserIdOrderByCreatedOnDesc(userId, pageable).stream()
                .map(comment -> CommentMapper.toDto(comment, user))
                .collect(Collectors.toList());
    }

    private UserDto getUserById(Long userId) {
        //Получаем пользователя через клиент
        try {
            UserDto user = userClient.getUserById(userId);
            log.debug("Existing User received from user-service: {}", user);
            return user;
        } catch (Exception e) {
            log.debug("Failed to get user from user-service: {}", e.getMessage());
            throw new NotFoundException("Пользователь c userId " + userId + " не найден");
        }
    }
}