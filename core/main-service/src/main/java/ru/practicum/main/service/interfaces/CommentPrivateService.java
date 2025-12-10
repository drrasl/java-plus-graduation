package ru.practicum.main.service.interfaces;

import org.springframework.data.domain.Pageable;
import ru.practicum.main.dto.request.comment.NewCommentDto;
import ru.practicum.main.dto.response.comment.CommentDto;

import java.util.List;

public interface CommentPrivateService {
    CommentDto addComment(Long userId, Long eventId, NewCommentDto newCommentDto);

    void deleteComment(Long userId, Long commentId);

    CommentDto updateComment(Long userId, Long commentId, NewCommentDto updateCommentDto);

    List<CommentDto> getCommentsByUserId(Long userId, Pageable pageable);
}
