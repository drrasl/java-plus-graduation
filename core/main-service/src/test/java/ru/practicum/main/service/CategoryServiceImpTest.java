package ru.practicum.main.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.dto.request.category.NewCategoryDto;
import ru.practicum.main.dto.response.category.CategoryDto;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.model.Category;
import ru.practicum.main.repository.CategoryRepository;
import ru.practicum.main.service.interfaces.CategoryService;
import ru.practicum.stats.client.StatClient;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class CategoryServiceImpTest {
    @Autowired
    private CategoryService categoryService;

    @MockBean
    private CategoryRepository categoryRepository;

    @MockBean
    private StatClient statClient;

    private final Long existingCatId = 1L;
    private final Long nonExistentCatId = 99L;

    //Тесты на добавление
    @Test
    void addCategory_shouldAddCategory_whenNameIsUnique() {
        NewCategoryDto newCategoryDto = new NewCategoryDto("New Category");
        Category savedCategory = new Category(1L, "New Category");
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        CategoryDto result = categoryService.addCategory(newCategoryDto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("New Category", result.getName());
        verify(categoryRepository).save(any(Category.class));
    }

    //Удаление - не найдена категория
    @Test
    void deleteCategory_shouldThrowNotFoundException_whenCategoryDoesNotExist() {
        when(categoryRepository.existsById(nonExistentCatId)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> categoryService.deleteCategory(nonExistentCatId));
        verify(categoryRepository).existsById(nonExistentCatId);
        verify(categoryRepository, never()).deleteById(anyLong());
    }

    //Тесты на обновление
    //Успешное обновление
    @Test
    void updateCategory_shouldUpdateCategory_whenCategoryExists() {
        NewCategoryDto newCategoryDto = new NewCategoryDto("Updated Category");
        Category existingCategory = new Category(existingCatId, "Test Category");
        when(categoryRepository.findById(existingCatId)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(new Category(existingCatId, "Updated Category"));

        CategoryDto result = categoryService.updateCategory(newCategoryDto, existingCatId);

        assertNotNull(result);
        assertEquals(existingCatId, result.getId());
        assertEquals("Updated Category", result.getName());
        verify(categoryRepository).findById(existingCatId);
        verify(categoryRepository).save(any(Category.class));
    }

    //Обновление - не найдена категория
    @Test
    void updateCategory_shouldThrowNotFoundException_whenCategoryDoesNotExist() {
        NewCategoryDto newCategoryDto = new NewCategoryDto("Updated Category");
        when(categoryRepository.findById(nonExistentCatId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> categoryService.updateCategory(newCategoryDto, nonExistentCatId));
        verify(categoryRepository).findById(nonExistentCatId);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    //Тесты на получение
    //Успешное получение категорий
    @Test
    void getCategories_shouldReturnListOfCategories_whenCalled() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Category> categories = List.of(
                new Category(1L, "Category 1"),
                new Category(2L, "Category 2")
        );
        Page<Category> categoryPage = new PageImpl<>(categories);
        when(categoryRepository.findAll(pageable)).thenReturn(categoryPage);

        List<CategoryDto> result = categoryService.getCategories(pageable);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Category 1", result.get(0).getName());
        assertEquals("Category 2", result.get(1).getName());
        verify(categoryRepository).findAll(pageable);
    }

    //Получение пустого списка
    @Test
    void getCategories_shouldReturnEmptyList_whenNoCategoriesFound() {
        Pageable pageable = PageRequest.of(0, 10);
        when(categoryRepository.findAll(pageable)).thenReturn(Page.empty());

        List<CategoryDto> result = categoryService.getCategories(pageable);

        assertTrue(result.isEmpty());
        verify(categoryRepository).findAll(pageable);
    }

    //Получение категории
    @Test
    void getCategory_shouldReturnCategory_whenCategoryExists() {
        Category category = new Category(existingCatId, "Test Category");
        when(categoryRepository.findById(existingCatId)).thenReturn(Optional.of(category));

        CategoryDto result = categoryService.getCategory(existingCatId);

        assertNotNull(result);
        assertEquals(existingCatId, result.getId());
        assertEquals("Test Category", result.getName());
        verify(categoryRepository).findById(existingCatId);
    }

    //Получение несуществующей категории
    @Test
    void getCategory_shouldThrowNotFoundException_whenCategoryDoesNotExist() {
        when(categoryRepository.findById(nonExistentCatId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> categoryService.getCategory(nonExistentCatId));
        verify(categoryRepository).findById(nonExistentCatId);
    }
}
