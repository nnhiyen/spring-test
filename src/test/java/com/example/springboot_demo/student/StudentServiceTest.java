package com.example.springboot_demo.student;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository repository;

    @InjectMocks
    private StudentService service;

    // Mục tiêu: kiểm tra service tạo và lưu sinh viên khi email chưa tồn tại.
    @Test
    void create_shouldSaveStudent_whenEmailIsNew() {

        // Bước 1 - ARRANGE: tạo request hợp lệ và giả lập repository chưa có email.
        var request = new CreateStudentRequest(
                "An",
                "an@example.com"
        );

        when(repository.existsByEmail(request.email()))
                .thenReturn(false);

        when(repository.save(any(Student.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Bước 2 - ACT: gọi service để tạo sinh viên.
        Student result = service.create(request);

        // Bước 3 - ASSERT: kiểm tra dữ liệu trả về và xác nhận repository đã lưu.
        assertEquals("An", result.getName());
        assertEquals("an@example.com", result.getEmail());

        verify(repository).save(any(Student.class));
    }

    // Mục tiêu: không cho tạo sinh viên nếu email đã được sử dụng.
    @Test
    void create_shouldThrow_whenEmailAlreadyExists() {

        // Bước 1 - ARRANGE: giả lập repository phát hiện email trùng.
        var request = new CreateStudentRequest(
                "An",
                "an@example.com"
        );

        when(repository.existsByEmail(request.email()))
                .thenReturn(true);

        // Bước 2-3 - ACT + ASSERT: service phải ném lỗi và không được lưu dữ liệu.
        assertThrows(
                IllegalStateException.class,
                () -> service.create(request)
        );

        verify(repository, never()).save(any(Student.class));
    }

    // Mục tiêu: từ chối request có tên chỉ chứa khoảng trắng.
    @Test
    void create_shouldThrow_whenNameIsBlank() {

        // Bước 1 - ARRANGE: tạo request có tên không hợp lệ.
        var request = new CreateStudentRequest(
                " ",
                "an@example.com"
        );

        // Bước 2-3 - ACT + ASSERT: service phải ném lỗi trước khi gọi repository.
        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(request)
        );

        verify(repository, never()).save(any(Student.class));
    }

    @Test
    void create_shouldThrow_whenNameIsNull() {
        // Mục tiêu: từ chối request bị thiếu tên.
        // Bước 1 - ARRANGE: tạo request có name = null.
        var request = new CreateStudentRequest(
                null,
                "an@example.com"
        );

        // Bước 2-3 - ACT + ASSERT: kiểm tra lỗi và xác nhận repository không tương tác.
        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(request)
        );

        verifyNoInteractions(repository);
    }

    @Test
    void create_shouldNotSave_whenEmailAlreadyExists() {
        // Mục tiêu: kiểm tra lại quy tắc email duy nhất trong một kịch bản độc lập.
        // Bước 1 - ARRANGE: cấu hình repository trả về email đã tồn tại.
        var request = new CreateStudentRequest(
                "An",
                "an@example.com"
        );
        when(repository.existsByEmail(request.email())).thenReturn(true);

        // Bước 2-3 - ACT + ASSERT: yêu cầu thất bại và không phát sinh thao tác save.
        assertThrows(
                IllegalStateException.class,
                () -> service.create(request)
        );

        verify(repository, never()).save(any(Student.class));
    }

    @Test
    void findAll_shouldFilterByNameOrEmailIgnoringCase() {
        when(repository.findAll()).thenReturn(List.of(
                new Student(1L, "Nguyen An", "an@example.com"),
                new Student(2L, "Tran Binh", "binh@example.com")
        ));

        List<Student> result = service.findAll(" NGUYEN ");

        assertEquals(1, result.size());
        assertEquals("Nguyen An", result.getFirst().getName());
    }

    @Test
    void findAll_shouldReturnAllStudents_whenKeywordIsBlank() {
        when(repository.findAll()).thenReturn(List.of(
                new Student(1L, "Nguyen An", "an@example.com"),
                new Student(2L, "Tran Binh", "binh@example.com")
        ));

        assertEquals(2, service.findAll(null).size());
        assertEquals(2, service.findAll(" ").size());
    }

    @Test
    void update_shouldSaveChangedStudent_whenIdExistsAndEmailIsNew() {
        when(repository.findById(1L))
                .thenReturn(java.util.Optional.of(
                        new Student(1L, "Old Name", "old@example.com")
                ));
        when(repository.existsByEmail("new@example.com")).thenReturn(false);
        when(repository.save(any(Student.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Student result = service.update(
                1L,
                new CreateStudentRequest("New Name", "new@example.com")
        );

        assertEquals(1L, result.getId());
        assertEquals("New Name", result.getName());
        assertEquals("new@example.com", result.getEmail());
        verify(repository).save(any(Student.class));
    }

    @Test
    void update_shouldThrow_whenStudentDoesNotExist() {
        when(repository.findById(99L)).thenReturn(java.util.Optional.empty());

        assertThrows(
                StudentNotFoundException.class,
                () -> service.update(
                        99L,
                        new CreateStudentRequest("An", "an@example.com")
                )
        );

        verify(repository, never()).save(any(Student.class));
    }

    @Test
    void update_shouldThrow_whenNewEmailAlreadyExists() {
        when(repository.findById(1L))
                .thenReturn(java.util.Optional.of(
                        new Student(1L, "An", "an@example.com")
                ));
        when(repository.existsByEmail("other@example.com")).thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () -> service.update(
                        1L,
                        new CreateStudentRequest("An", "other@example.com")
                )
        );

        verify(repository, never()).save(any(Student.class));
    }

    @Test
    void delete_shouldDeleteStudent_whenIdExists() {
        when(repository.existsById(1L)).thenReturn(true);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void delete_shouldThrow_whenStudentDoesNotExist() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThrows(
                StudentNotFoundException.class,
                () -> service.delete(99L)
        );

        verify(repository, never()).deleteById(anyLong());
    }
}
