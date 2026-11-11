package com.example.springboot_demo.student;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StudentE2eTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    @Test
    void createStudent_shouldExposeCreatedStudentOverHttp() throws Exception {
        // Mục tiêu: kiểm tra E2E từ HTTP client thật đến controller, service và database.
        // Bước 1 - ARRANGE: tạo POST request HTTP với payload hợp lệ.
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/students"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {"name":"E2E Student","email":"e2e@example.com"}
                        """))
                .build();

        // Bước 2 - ACT: gửi request qua cổng HTTP thật của ứng dụng.
        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        // Bước 3 - ASSERT: kiểm tra HTTP 201 và dữ liệu sinh viên trong response.
        assertEquals(201, response.statusCode());
        assertTrue(response.body().contains("\"name\":\"E2E Student\""));
        assertTrue(response.body().contains("\"email\":\"e2e@example.com\""));
    }

    @Test
    void createStudent_shouldReturn400_whenEmailIsInvalid() throws Exception {
        // Mục tiêu: kiểm tra E2E validation với email sai định dạng.
        // Bước 1 - ARRANGE: tạo request chứa email không hợp lệ.
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/students"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {"name":"Invalid Student","email":"invalid-email"}
                        """))
                .build();

        // Bước 2 - ACT: gửi request thật tới ứng dụng đang chạy.
        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        // Bước 3 - ASSERT: validation phải trả HTTP 400.
        assertEquals(400, response.statusCode());
    }

    @Test
    void studentManagement_shouldSupportBulkSearchUpdateAndDelete()
            throws Exception {
        HttpResponse<String> bulkResponse = send(
                "POST",
                "/api/students/bulk",
                """
                [
                  {"name":"E2E An","email":"e2e.an@example.com"},
                  {"name":"E2E Binh","email":"e2e.binh@example.com"}
                ]
                """
        );
        assertEquals(201, bulkResponse.statusCode());

        HttpResponse<String> searchResponse = send(
                "GET",
                "/api/students?keyword=E2E%20An",
                null
        );
        assertEquals(200, searchResponse.statusCode());
        assertTrue(searchResponse.body().contains("e2e.an@example.com"));

        String id = extractId(searchResponse.body());
        HttpResponse<String> updateResponse = send(
                "PUT",
                "/api/students/" + id,
                """
                {"name":"E2E An Updated","email":"e2e.updated@example.com"}
                """
        );
        assertEquals(200, updateResponse.statusCode());
        assertTrue(updateResponse.body().contains("E2E An Updated"));

        HttpResponse<String> deleteResponse = send(
                "DELETE",
                "/api/students/" + id,
                null
        );
        assertEquals(204, deleteResponse.statusCode());
    }

    private HttpResponse<String> send(
            String method,
            String path,
            String body
    ) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path));

        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json")
                    .method(
                            method,
                            HttpRequest.BodyPublishers.ofString(body)
                    );
        }

        return httpClient.send(
                builder.build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private String extractId(String responseBody) {
        int idStart = responseBody.indexOf("\"id\":") + 5;
        int idEnd = responseBody.indexOf(",", idStart);
        return responseBody.substring(idStart, idEnd);
    }
}
