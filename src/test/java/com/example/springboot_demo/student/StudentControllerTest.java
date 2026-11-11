package com.example.springboot_demo.student;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StudentController.class)
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    @Test
    void create_shouldReturn201_whenRequestIsValid() throws Exception {
        when(studentService.create(any(CreateStudentRequest.class)))
                .thenReturn(new Student(10L, "An", "an@example.com"));

        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"An","email":"an@example.com"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("An"))
                .andExpect(jsonPath("$.email").value("an@example.com"));

        verify(studentService).create(any(CreateStudentRequest.class));
    }

    @Test
    void createMany_shouldReturn201WithAllStudents() throws Exception {
        when(studentService.create(any(CreateStudentRequest.class)))
                .thenReturn(
                        new Student(1L, "An", "an@example.com"),
                        new Student(2L, "Binh", "binh@example.com")
                );

        mockMvc.perform(post("/api/students/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {"name":"An","email":"an@example.com"},
                                  {"name":"Binh","email":"binh@example.com"}
                                ]
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("An"))
                .andExpect(jsonPath("$[1].email").value("binh@example.com"));

        verify(studentService, times(2)).create(any(CreateStudentRequest.class));
    }

    @Test
    void findAll_shouldReturnStudentsMatchingKeyword() throws Exception {
        when(studentService.findAll("an")).thenReturn(List.of(
                new Student(1L, "Nguyen An", "an@example.com")
        ));

        mockMvc.perform(get("/api/students").param("keyword", "an"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Nguyen An"));

        verify(studentService).findAll("an");
    }

    @Test
    void update_shouldReturnUpdatedStudent() throws Exception {
        when(studentService.update(eq(1L), any(CreateStudentRequest.class)))
                .thenReturn(new Student(1L, "Updated", "updated@example.com"));

        mockMvc.perform(put("/api/students/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Updated","email":"updated@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Updated"));

        verify(studentService).update(eq(1L), any(CreateStudentRequest.class));
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        doNothing().when(studentService).delete(1L);

        mockMvc.perform(delete("/api/students/1"))
                .andExpect(status().isNoContent());

        verify(studentService).delete(1L);
    }

    @Test
    void create_shouldReturn400_whenRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","email":"not-an-email"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(studentService);
    }
}
