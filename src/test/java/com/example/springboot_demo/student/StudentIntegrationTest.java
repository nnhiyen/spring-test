package com.example.springboot_demo.student;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class StudentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Dọn database trước mỗi test để các test độc lập và không phụ thuộc thứ tự chạy.
        studentRepository.deleteAll();
    }

    @Test
    void createStudent_shouldSaveStudentToDatabase() throws Exception {

        // Mục tiêu: kiểm tra toàn bộ luồng Controller -> Service -> Repository -> H2.
        // Bước 1 - ARRANGE: chuẩn bị request hợp lệ.
        CreateStudentRequest request =
                new CreateStudentRequest(
                        "Nguyen Van A",
                        "a@example.com"
                );

        // Bước 2 - ACT + ASSERT HTTP: gửi request qua Spring context thật và kiểm tra response.
        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Nguyen Van A"))
                .andExpect(jsonPath("$.email").value("a@example.com"));

        // Bước 3 - ASSERT DATABASE: xác nhận bản ghi thực sự tồn tại trong H2.
        assertEquals(1, studentRepository.count());
        Student savedStudent = studentRepository
                .findAll()
                .stream()
                .filter(student -> "a@example.com".equals(student.getEmail()))
                .findFirst()
                .orElseThrow();
        assertEquals("Nguyen Van A", savedStudent.getName());
    }

    @Test
    void createStudent_shouldReturn500_whenEmailAlreadyExists() throws Exception {
        // Mục tiêu: kiểm tra toàn bộ luồng phát hiện email trùng.
        // Bước 1 - ARRANGE: lưu trước một sinh viên có email cần kiểm tra.
        Student existingStudent = new Student(
                "Existing Student",
                "duplicate@example.com"
        );
        studentRepository.saveAndFlush(existingStudent);

        // Bước 2 - ARRANGE: tạo request mới nhưng dùng lại email đã tồn tại.
        CreateStudentRequest request = new CreateStudentRequest(
                "New Student",
                "duplicate@example.com"
        );

        // Bước 3 - ACT: gửi request; service phải dừng với lỗi "Email already exists".
        ServletException exception = assertThrows(
                ServletException.class,
                () -> mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
        );

        // Bước 4 - ASSERT: kiểm tra đúng lỗi và bảo đảm database không tạo thêm bản ghi.
        assertEquals("Email already exists", exception.getCause().getMessage());
        assertEquals(1, studentRepository.count());
    }

    @Test
    void createManyFindUpdateAndDelete_shouldWorkWithDatabase() throws Exception {
        String bulkRequest = """
                [
                  {"name":"Nguyen An","email":"an@example.com"},
                  {"name":"Tran Binh","email":"binh@example.com"}
                ]
                """;

        mockMvc.perform(post("/api/students/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bulkRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/students")
                        .param("keyword", "nguyen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Nguyen An"));

        Long studentId = studentRepository.findAll().stream()
                .filter(student -> "an@example.com".equals(student.getEmail()))
                .findFirst()
                .orElseThrow()
                .getId();

        mockMvc.perform(put("/api/students/" + studentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"An Updated","email":"an.updated@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("An Updated"))
                .andExpect(jsonPath("$.email").value("an.updated@example.com"));

        mockMvc.perform(delete("/api/students/" + studentId))
                .andExpect(status().isNoContent());

        assertEquals(1, studentRepository.count());
    }
}