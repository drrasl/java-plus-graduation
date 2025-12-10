package ru.practicum.main.service.interfaces;

import org.springframework.data.domain.Pageable;
import ru.practicum.main.dto.request.user.NewUserRequest;
import ru.practicum.main.dto.response.user.UserDto;


import java.util.List;

public interface UserService {
    List<UserDto> getUsers(List<Long> ids, Pageable pageable);

    UserDto addUser(NewUserRequest newUserRequest);

    void deleteUser(Long id);
}
