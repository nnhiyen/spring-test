package com.example.springboot_demo.student;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class StudentRepositoryJpaTest {

    @Autowired
    private StudentRepository repository;

    @Test
    void save_shouldPersistStudentAndGenerateId() {
        // Mục tiêu: kiểm tra JPA persist entity, sinh ID và truy vấn lại được dữ liệu.
        // Bước 1 - ACT: lưu entity và flush ngay xuống database H2.
        Student saved = repository.saveAndFlush(
                new Student("An", "an@example.com")
        );

        // Bước 2 - ASSERT: ID được sinh, bản ghi tìm thấy và email được nhận diện.
        assertTrue(saved.getId() > 0);
        assertEquals(
                saved.getId(),
                repository.findById(saved.getId()).orElseThrow().getId()
        );
        assertTrue(repository.existsByEmail("an@example.com"));
    }

    @Test
    void existsByEmail_shouldReturnFalseForUnknownEmail() {
        // Mục tiêu: kiểm tra truy vấn existsByEmail trả false cho email chưa có.
        // Bước 1-2 - ACT + ASSERT: truy vấn database với email không tồn tại.
        assertFalse(repository.existsByEmail("unknown@example.com"));
    }

    @Test
    void save_shouldFail_whenEmailIsDuplicated() {
        // Mục tiêu: kiểm tra database thực thi unique constraint trên cột email.
        // Bước 1 - ARRANGE: lưu bản ghi đầu tiên.
        repository.saveAndFlush(new Student("First", "same@example.com"));

        // Bước 2-3 - ACT + ASSERT: bản ghi trùng phải ném DataIntegrityViolationException.
        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(
                        new Student("Second", "same@example.com")
                )
        );
    }
}
