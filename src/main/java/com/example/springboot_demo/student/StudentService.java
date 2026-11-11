package com.example.springboot_demo.student;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class StudentService {

    private final StudentRepository repository;

    public StudentService(StudentRepository repository) {
        this.repository = repository;
    }

    public Student create(CreateStudentRequest request) {

        if (request.name() == null
                || request.name().isBlank()) {
            throw new IllegalArgumentException(
                    "Name is required"
            );
        }

        if (repository.existsByEmail(request.email())) {
            throw new IllegalStateException(
                    "Email already exists"
            );
        }

        Student student = new Student(
                request.name(),
                request.email()
        );

        return repository.save(student);
    }

    public List<Student> findAll(String keyword) {
        String normalizedKeyword = keyword == null
                ? ""
                : keyword.trim().toLowerCase(Locale.ROOT);

        return repository.findAll().stream()
                .filter(student -> normalizedKeyword.isBlank()
                        || student.getName().toLowerCase(Locale.ROOT)
                        .contains(normalizedKeyword)
                        || student.getEmail().toLowerCase(Locale.ROOT)
                        .contains(normalizedKeyword))
                .toList();
    }

    public Student update(Long id, CreateStudentRequest request) {
        validateRequest(request);
        Student existing = repository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException(id));

        if (!existing.getEmail().equalsIgnoreCase(request.email())
                && repository.existsByEmail(request.email())) {
            throw new IllegalStateException("Email already exists");
        }

        return repository.save(new Student(id, request.name(), request.email()));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new StudentNotFoundException(id);
        }
        repository.deleteById(id);
    }

    private void validateRequest(CreateStudentRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
    }
}