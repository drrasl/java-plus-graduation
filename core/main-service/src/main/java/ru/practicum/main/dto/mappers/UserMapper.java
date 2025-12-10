package ru.practicum.main.dto.mappers;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.main.dto.request.user.NewUserRequest;
import ru.practicum.main.dto.response.user.UserDto;
import ru.practicum.main.dto.response.user.UserShortDto;
import ru.practicum.main.model.User;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserMapper {
    public static User toEntity(NewUserRequest newUserRequest) {
        return User.builder()
                .id(0L)
                .name(newUserRequest.getName())
                .email(newUserRequest.getEmail())
                .build();
    }

    public static UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    public static UserShortDto toUserShortDto(User user) {
        return UserShortDto.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
    }
}
