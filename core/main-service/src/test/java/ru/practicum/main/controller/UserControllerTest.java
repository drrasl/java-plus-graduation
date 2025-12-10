package ru.practicum.main.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.main.dto.request.user.NewUserRequest;
import ru.practicum.main.dto.response.user.UserDto;
import ru.practicum.main.service.interfaces.UserService;
import ru.practicum.stats.client.StatClient;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(UserController.class)
public class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService service;

    @MockBean
    private StatClient statClient;

    //Тесты на получение
    //Получение - список id
    @Test
    void getUsersWhenIdsAreProvidedThenReturnsUsers() throws Exception {
        List<Long> ids = List.of(1L, 2L);
        List<UserDto> users = List.of(
                new UserDto(1L, "User One", "user1@example.com"),
                new UserDto(2L, "User Two", "user2@example.com")
        );
        when(service.getUsers(anyList(), any()))
                .thenReturn(users);
        mockMvc.perform(get("/admin/users")
                        .param("ids", "1", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("User One"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].name").value("User Two"));
    }

    //Получение - пагинация
    @Test
    void getUsersWhenNoIdsThenReturnsAllUsers() throws Exception {
        List<UserDto> users = List.of(
                new UserDto(1L, "User One", "user1@example.com")
        );
        when(service.getUsers(any(), any()))
                .thenReturn(users);

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("User One"));
    }

    //Тесты на добавление
    //Успешное добавление
    @Test
    void addUserWhenValidRequestThenReturnsCreatedStatusAndUser() throws Exception {
        NewUserRequest newUserRequest = new NewUserRequest("new.user@example.com", "New User");
        UserDto createdUser = new UserDto(1L, "New User", "new.user@example.com");
        when(service.addUser(any(NewUserRequest.class)))
                .thenReturn(createdUser);
        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUserRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("New User"));
    }

    //Тесты на удаление
    @Test
    void deleteUserWhenExistsThenReturnsNoContentStatus() throws Exception {
        mockMvc.perform(delete("/admin/users/1"))
                .andExpect(status().isNoContent());
    }
}
