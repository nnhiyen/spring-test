package com.example.springboot_demo.student;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping
    public ResponseEntity<StudentResponse> create(
            @Valid @RequestBody CreateStudentRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(studentService.create(request)));
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<StudentResponse>> createMany(
            @RequestBody @Valid List<@Valid CreateStudentRequest> requests) {
        List<StudentResponse> students = requests.stream()
                .map(studentService::create)
                .map(this::toResponse)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(students);
    }

    @GetMapping
    public List<StudentResponse> findAll(
            @RequestParam(required = false) String keyword) {
        return studentService.findAll(keyword).stream()
                .map(this::toResponse)
                .toList();
    }

    @PutMapping("/{id}")
    public StudentResponse update(
            @PathVariable Long id,
            @Valid @RequestBody CreateStudentRequest request) {
        return toResponse(studentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        studentService.delete(id);
    }

    private StudentResponse toResponse(Student student) {
        return new StudentResponse(
                student.getId(),
                student.getName(),
                student.getEmail()
        );
    }
}