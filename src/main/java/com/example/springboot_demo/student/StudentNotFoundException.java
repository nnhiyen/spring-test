package com.example.springboot_demo.student;

public class StudentNotFoundException extends RuntimeException {

    public StudentNotFoundException(Long id) {
        super("Student not found: " + id);
    }
}
