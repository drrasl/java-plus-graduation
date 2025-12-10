package ru.practicum.main.service;

import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.dto.mappers.CommentMapper;
import ru.practicum.main.dto.request.comment.SearchOfCommentByAdminDto;
import ru.practicum.main.dto.response.comment.CommentDto;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.model.QComment;
import ru.practicum.main.repository.CommentRepository;
import ru.practicum.main.service.interfaces.CommentAdminService;

import java.util.List;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CommentAdminServiceImpl implements CommentAdminService {

    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public void deleteComment(Long commentId) {
        log.info("Администратор удаляет комментарий с ID: {}", commentId);
        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException(String.format("Comment with id=%s was not found", commentId));
        }
        commentRepository.deleteById(commentId);
        log.info("Комментарий с ID: {} удален", commentId);
    }

    @Override
    public List<CommentDto> getComments(SearchOfCommentByAdminDto searchDto, Pageable pageable) {
        log.info("Администратор получает комментарии с фильтрами: {}", searchDto);

        QComment comment = QComment.comment;
        BooleanBuilder predicate = new BooleanBuilder();

        if (searchDto.getUsers() != null && !searchDto.getUsers().isEmpty()) {
            predicate.and(comment.user.id.in(searchDto.getUsers()));
        }
        if (searchDto.getEvents() != null && !searchDto.getEvents().isEmpty()) {
            predicate.and(comment.event.id.in(searchDto.getEvents()));
        }
        if (searchDto.getRangeStart() != null) {
            predicate.and(comment.createdOn.goe(searchDto.getRangeStart()));
        }
        if (searchDto.getRangeEnd() != null) {
            predicate.and(comment.createdOn.loe(searchDto.getRangeEnd()));
        }

        return commentRepository.findAll(predicate, pageable).stream()
                .map(CommentMapper::toDto)
                .collect(Collectors.toList());
    }
}