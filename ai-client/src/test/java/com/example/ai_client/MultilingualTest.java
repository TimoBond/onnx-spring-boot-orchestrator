package com.example.ai_client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;

class MultilingualTest {

    private MockWebServer mockWebServer;
    private AiService aiService;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();
        aiService = new AiService(webClient);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Українська мова — позитивний")
    void testUkrainianPositive() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"text\":\"чудово\",\"sentiment\":\"ПОЗИТИВНИЙ\",\"confidence\":0.85}")
                .addHeader("Content-Type", "application/json"));

        String result = aiService.predict("чудово відмінно");
        assertTrue(result.contains("ПОЗИТИВНИЙ"), "Українська позитивна фраза не розпізнана");
    }

    @Test
    @DisplayName("Англійська мова — позитивний")
    void testEnglishPositive() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"text\":\"excellent\",\"sentiment\":\"ПОЗИТИВНИЙ\",\"confidence\":0.88}")
                .addHeader("Content-Type", "application/json"));

        String result = aiService.predict("excellent amazing wonderful");
        assertTrue(result.contains("ПОЗИТИВНИЙ"), "Англійська позитивна фраза не розпізнана");
    }

    @Test
    @DisplayName("Українська мова — негативний")
    void testUkrainianNegative() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"text\":\"жахливо\",\"sentiment\":\"НЕГАТИВНИЙ\",\"confidence\":0.83}")
                .addHeader("Content-Type", "application/json"));

        String result = aiService.predict("жахливо погано");
        assertTrue(result.contains("НЕГАТИВНИЙ"), "Українська негативна фраза не розпізнана");
    }

    @Test
    @DisplayName("Англійська мова — негативний")
    void testEnglishNegative() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"text\":\"terrible\",\"sentiment\":\"НЕГАТИВНИЙ\",\"confidence\":0.86}")
                .addHeader("Content-Type", "application/json"));

        String result = aiService.predict("terrible horrible awful");
        assertTrue(result.contains("НЕГАТИВНИЙ"), "Англійська негативна фраза не розпізнана");
    }
}
