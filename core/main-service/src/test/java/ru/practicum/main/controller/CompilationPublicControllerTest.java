package ru.practicum.main.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.main.dto.response.category.CategoryDto;
import ru.practicum.main.dto.response.compilation.CompilationDto;
import ru.practicum.main.dto.response.event.EventShortDto;
import ru.practicum.main.service.interfaces.CompilationPublicService;
import ru.practicum.stats.client.StatClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CompilationPublicController.class)
public class CompilationPublicControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    private StatClient statClient;

    @MockBean
    CompilationPublicService compilationPublicService;

    private EventShortDto eventShortDto;
    private CompilationDto compilationDto;

    @BeforeEach
    void setUp() {
        this.eventShortDto = EventShortDto.builder()
                .title("title")
                .annotation("annotation")
                .paid(true)
                .id(1L)
                .views(100L)
                .eventDate(LocalDateTime.now())
                .category(CategoryDto.builder().id(1L).name("category").build())
                .build();

        this.compilationDto = CompilationDto.builder()
                .title("compilation")
                .pinned(true)
                .id(1L)
                .events(Set.of(eventShortDto))
                .build();
    }


    @Test
    public void findAllByFilters_shouldReturnCompilations() throws Exception {
        List<CompilationDto> compilations = List.of(compilationDto);

        when(compilationPublicService.findAllByFilters(eq(true), Mockito.any()))
                .thenReturn(compilations);

        mockMvc.perform(get("/compilations")
                        .param("pinned", "true")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("compilation"))
                .andExpect(jsonPath("$[0].pinned").value(true));
    }

    @Test
    public void findById_shouldReturnCompilation() throws Exception {
        Long compilationId = 1L;

        when(compilationPublicService.findById(compilationId))
                .thenReturn(compilationDto);

        mockMvc.perform(get("/compilations/{compId}", compilationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(compilationId))
                .andExpect(jsonPath("$.title").value("compilation"))
                .andExpect(jsonPath("$.pinned").value(true))
                .andExpect(jsonPath("$.events", hasSize(1)));
    }
}
