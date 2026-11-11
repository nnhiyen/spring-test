package com.example.springboot_demo.student;

public record StudentResponse(
        Long id,
        String name,
        String email
) {
}