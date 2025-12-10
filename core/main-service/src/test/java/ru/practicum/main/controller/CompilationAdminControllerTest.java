package ru.practicum.main.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.main.dto.request.compilation.NewCompilationDto;
import ru.practicum.main.dto.request.compilation.UpdateCompilationRequest;
import ru.practicum.main.dto.response.category.CategoryDto;
import ru.practicum.main.dto.response.compilation.CompilationDto;
import ru.practicum.main.dto.response.event.EventShortDto;
import ru.practicum.main.service.interfaces.CompilationAdminService;
import ru.practicum.stats.client.StatClient;

import java.time.LocalDateTime;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CompilationAdminController.class)
public class CompilationAdminControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    private StatClient statClient;

    @MockBean
    CompilationAdminService compilationAdminService;

    private EventShortDto eventShortDto;

    private CompilationDto compilationDto;

    private NewCompilationDto newCompilationDto;

    private UpdateCompilationRequest updateCompilationDto;

    @BeforeEach
    void setUp() {
        this.eventShortDto = EventShortDto
                .builder()
                .title("tittle")
                .annotation("annotation")
                .paid(true)
                .id(1L)
                .views(100L)
                .eventDate(LocalDateTime.now())
                .category(CategoryDto.builder().id(1L).name("category").build())
                .build();

        this.compilationDto = CompilationDto
                .builder()
                .title("compilation")
                .pinned(true)
                .id(1L)
                .events(Set.of(eventShortDto))
                .build();

        this.newCompilationDto = NewCompilationDto
                .builder()
                .events(Set.of(1L))
                .pinned(true)
                .title("compilation")
                .build();

        this.updateCompilationDto = UpdateCompilationRequest
                .builder()
                .events(Set.of(1L))
                .pinned(false)
                .title("updated compilation")
                .build();
    }

    @Test
    public void addCompilation_shouldSaveNewCompilation() throws Exception {
        when(compilationAdminService.add(newCompilationDto))
                .thenReturn(compilationDto);

        mockMvc.perform(post("/admin/compilations")
                        .content(objectMapper.writeValueAsString(newCompilationDto))
                        .contentType(MediaType.APPLICATION_JSON)
                ).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("compilation"));
    }

    @Test
    public void deleteCompilation_shouldDeleteCompilationSuccessfully() throws Exception {
        Long compilationId = 1L;

        doNothing().when(compilationAdminService).deleteById(compilationId);

        mockMvc.perform(delete("/admin/compilations/{compId}", compilationId))
                .andExpect(status().isNoContent());

        verify(compilationAdminService, times(1)).deleteById(compilationId);
    }

    @Test
    public void updateCompilation_shouldUpdateCompilationSuccessfully() throws Exception {
        Long compilationId = 1L;
        CompilationDto updatedDto = CompilationDto.builder()
                .id(compilationId)
                .title("updated compilation")
                .pinned(false)
                .events(Set.of(eventShortDto))
                .build();

        when(compilationAdminService.update(compilationId, updateCompilationDto))
                .thenReturn(updatedDto);

        mockMvc.perform(patch("/admin/compilations/{compId}", compilationId)
                        .content(objectMapper.writeValueAsString(updateCompilationDto))
                        .contentType(MediaType.APPLICATION_JSON)
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(compilationId))
                .andExpect(jsonPath("$.title").value("updated compilation"))
                .andExpect(jsonPath("$.pinned").value(false));
    }

}
