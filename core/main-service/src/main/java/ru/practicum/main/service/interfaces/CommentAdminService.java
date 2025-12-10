package ru.practicum.main.service.interfaces;

import org.springframework.data.domain.Pageable;
import ru.practicum.main.dto.request.comment.SearchOfCommentByAdminDto;
import ru.practicum.main.dto.response.comment.CommentDto;

import java.util.List;

public interface CommentAdminService {
    void deleteComment(Long commentId);

    List<CommentDto> getComments(SearchOfCommentByAdminDto searchDto, Pageable pageable);
}
