package ru.practicum.main.service;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.practicum.main.dto.request.user.NewUserRequest;
import ru.practicum.main.dto.response.user.UserDto;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.model.User;
import ru.practicum.main.repository.UserRepository;
import ru.practicum.main.service.interfaces.UserService;
import ru.practicum.stats.client.StatClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class UserServiceImpTest {
    @Autowired
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private StatClient statClient;

    //Тесты на получение пользователей
    //Получение - список id
    @Test
    void getUsersWhenIdsProvidedThenReturnsUsers() {
        List<Long> userIds = List.of(1L, 2L);
        List<User> users = List.of(
                new User(1L, "User One", "user1@example.com"),
                new User(2L, "User Two", "user2@example.com")
        );
        when(userRepository.findAllById(userIds)).thenReturn(users);
        List<UserDto> result = userService.getUsers(userIds, PageRequest.of(0, 10));
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("User One", result.get(0).getName());
        assertEquals("User Two", result.get(1).getName());
        verify(userRepository).findAllById(userIds);
    }

    //Получение - пагинация
    @Test
    void getUsersWhenNoIdsThenReturnsAllUsersPaginated() {
        Pageable pageable = PageRequest.of(0, 10);
        List<User> users = List.of(new User(1L, "User One", "user1@example.com"));
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(users));
        List<UserDto> result = userService.getUsers(null, pageable);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("User One", result.get(0).getName());
        verify(userRepository).findAll(pageable);
    }

    //Тесты на добавление пользователя
    //Успешное добавление
    @Test
    void addUser_shouldSaveUser_whenEmailIsUnique() {
        NewUserRequest newUserRequest = new NewUserRequest("new.user@example.com", "New User");
        User savedUser = new User(1L, "New User", "new.user@example.com");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        UserDto result = userService.addUser(newUserRequest);
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("New User", result.getName());
        assertEquals("new.user@example.com", result.getEmail());
        verify(userRepository).save(any(User.class));
    }

    //Тесты на удаление пользователя
    //Успешное удаление
    @Test
    void deleteUser_shouldDeleteUser_whenUserExists() {
        Long userId = 1L;
        when(userRepository.existsById(userId)).thenReturn(true);
        doNothing().when(userRepository).deleteById(userId);
        userService.deleteUser(userId);
        verify(userRepository).existsById(userId);
        verify(userRepository).deleteById(userId);
    }

    //Удаление - пользователь не найден
    @Test
    void deleteUser_shouldThrowNotFoundException_whenUserDoesNotExist() {
        Long nonExistentUserId = 99L;
        when(userRepository.existsById(nonExistentUserId)).thenReturn(false);
        assertThrows(NotFoundException.class, () -> userService.deleteUser(nonExistentUserId));
        verify(userRepository).existsById(nonExistentUserId);
        verify(userRepository, never()).deleteById(anyLong());
    }
}
