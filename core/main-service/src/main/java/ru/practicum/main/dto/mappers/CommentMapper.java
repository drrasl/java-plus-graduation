package ru.practicum.main.dto.mappers;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.main.dto.request.comment.NewCommentDto;
import ru.practicum.main.dto.response.comment.CommentDto;
import ru.practicum.main.dto.response.user.UserDto;
import ru.practicum.main.model.Comment;

import static ru.practicum.main.dto.mappers.UserMapper.toUserShortDto;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommentMapper {

    public static Comment toEntity(NewCommentDto newCommentDto) {
        return Comment.builder()
                .id(0L)
                .text(newCommentDto.getText())
                .build();
    }

    public static CommentDto toDto(Comment comment, UserDto userDto) {
        return CommentDto.builder()
                .id(comment.getId())
                .user(toUserShortDto(userDto))
                .text(comment.getText())
                .createdOn(comment.getCreatedOn())
                .updatedOn(comment.getUpdatedOn())
                .build();
    }
}
