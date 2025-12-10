package ru.practicum.main.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.main.dto.request.category.NewCategoryDto;
import ru.practicum.main.dto.response.category.CategoryDto;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.service.interfaces.CategoryService;
import ru.practicum.stats.client.StatClient;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService service;

    @MockBean
    private StatClient statClient;

    private final Long existingCatId = 1L;

    private NewCategoryDto request;

    @BeforeEach
    void setUp() {
        request = new NewCategoryDto("Test category");
    }

    //Тесты на добавление категории
    //Успешное добавление
    @Test
    void addCategory_shouldReturnOkAndAddedCategory() throws Exception {
        CategoryDto response = new CategoryDto(existingCatId, "Test category");

        when(service.addCategory(any(NewCategoryDto.class)))
                .thenReturn(response);

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(existingCatId))
                .andExpect(jsonPath("$.name").value("Test category"));
    }

    //Добавление - не уникальное имя
    @Test
    void addCategory_shouldReturnConflict_whenNameNotUnique() throws Exception {
        doThrow(new ConflictException("Имя категории должно быть уникальным!"))
                .when(service).addCategory(any(NewCategoryDto.class));

        mockMvc.perform(post("/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        verify(service, times(1)).addCategory(any(NewCategoryDto.class));
    }

    //Тесты на удаление
    //Успешное удаление
    @Test
    void deleteCategory_shouldReturnOk() throws Exception {
        doNothing().when(service).deleteCategory(anyLong());

        mockMvc.perform(delete("/admin/categories/{catId}", existingCatId))
                .andExpect(status().isNoContent());

        verify(service, times(1)).deleteCategory(existingCatId);
    }

    //Удаление - категория не найдена
    @Test
    void deleteCategory_shouldReturnNotFound_whenCategoryDoesNotExist() throws Exception {
        doThrow(new NotFoundException("Category with id=" + existingCatId + " was not found"))
                .when(service).deleteCategory(anyLong());

        mockMvc.perform(delete("/admin/categories/{catId}", existingCatId))
                .andExpect(status().isNotFound());

        verify(service, times(1)).deleteCategory(existingCatId);
    }

    //Удаление - категория привязана к событию
    @Test
    void deleteCategory_shouldReturnConflict_whenCategoryIsRelatedToEvents() throws Exception {
        doThrow(new ConflictException("The category is not empty"))
                .when(service).deleteCategory(anyLong());

        mockMvc.perform(delete("/admin/categories/{catId}", existingCatId))
                .andExpect(status().isConflict());

        verify(service, times(1)).deleteCategory(existingCatId);
    }

    //Тесты на обновление
    //Успешное обновление
    @Test
    void updateCategory_shouldReturnOkAndUpdatedCategory() throws Exception {
        CategoryDto updatedCategoryDto = new CategoryDto(1L, "Updated category");

        when(service.updateCategory(any(NewCategoryDto.class), anyLong()))
                .thenReturn(updatedCategoryDto);

        mockMvc.perform(patch("/admin/categories/{catId}", existingCatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedCategoryDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingCatId))
                .andExpect(jsonPath("$.name").value(updatedCategoryDto.getName()));

        verify(service, times(1)).updateCategory(any(NewCategoryDto.class), anyLong());
    }

    //Обновление - не найдена категория
    @Test
    void updateCategory_shouldReturnNotFound_whenCategoryDoesNotExist() throws Exception {
        NewCategoryDto request = new NewCategoryDto("Updated category");

        doThrow(new NotFoundException("Category with id=" + existingCatId + " was not found"))
                .when(service).updateCategory(any(NewCategoryDto.class), anyLong());

        mockMvc.perform(patch("/admin/categories/{catId}", existingCatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        verify(service, times(1)).updateCategory(any(NewCategoryDto.class), anyLong());
    }

    // Обновление - нарушение целостности данных
    @Test
    void updateCategory_shouldReturnConflict_whenNameNotUnique() throws Exception {
        doThrow(new ConflictException("Нарушение целостности данных"))
                .when(service).updateCategory(any(NewCategoryDto.class), anyLong());
        mockMvc.perform(patch("/admin/categories/{catId}", existingCatId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
        verify(service, times(1)).updateCategory(any(NewCategoryDto.class), eq(existingCatId));
    }

    //Тесты на получение
    //Успешное получение категорий
    @Test
    void getCategories_shouldReturnOkAndListOfCategories_whenCalledWithDefaultParams() throws Exception {
        List<CategoryDto> responseList = List.of(
                new CategoryDto(1L, "Category 1"),
                new CategoryDto(2L, "Category 2")
        );

        when(service.getCategories(any(Pageable.class)))
                .thenReturn(responseList);

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Category 1"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].name").value("Category 2"));

        verify(service, times(1)).getCategories(any(Pageable.class));
    }

    //Получение пустого списка
    @Test
    void getCategories_shouldReturnOkAndEmptyList_whenNoCategoriesFound() throws Exception {
        when(service.getCategories(any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(service, times(1)).getCategories(any(Pageable.class));
    }

    //Успешное получение категории
    @Test
    void getCategory_shouldReturnOkAndCategory_whenCategoryExists() throws Exception {
        CategoryDto response = new CategoryDto(existingCatId, "Test category");

        when(service.getCategory(anyLong()))
                .thenReturn(response);

        mockMvc.perform(get("/categories/{catId}", existingCatId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingCatId))
                .andExpect(jsonPath("$.name").value("Test category"));

        verify(service, times(1)).getCategory(existingCatId);
    }

    //Получение несуществующей категории
    @Test
    void getCategory_shouldReturnNotFound_whenCategoryDoesNotExist() throws Exception {
        doThrow(new NotFoundException("Category with id=" + existingCatId + " was not found"))
                .when(service).getCategory(anyLong());

        mockMvc.perform(get("/categories/{catId}", existingCatId))
                .andExpect(status().isNotFound());

        verify(service, times(1)).getCategory(existingCatId);
    }

}