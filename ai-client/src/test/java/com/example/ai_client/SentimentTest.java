package com.example.ai_client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;

class SentimentTest {

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
    @DisplayName("Позитивний текст → ПОЗИТИВНИЙ")
    void testPositiveSentiment() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"text\":\"чудово\",\"sentiment\":\"ПОЗИТИВНИЙ\",\"confidence\":0.85}")
                .addHeader("Content-Type", "application/json"));

        String result = aiService.predict("чудово відмінно супер");
        assertTrue(result.contains("ПОЗИТИВНИЙ"));
    }

    @Test
    @DisplayName("Негативний текст → НЕГАТИВНИЙ")
    void testNegativeSentiment() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"text\":\"жахливо\",\"sentiment\":\"НЕГАТИВНИЙ\",\"confidence\":0.82}")
                .addHeader("Content-Type", "application/json"));

        String result = aiService.predict("жахливо погано провал");
        assertTrue(result.contains("НЕГАТИВНИЙ"));
    }

    @Test
    @DisplayName("Нейтральний текст → НЕЙТРАЛЬНИЙ")
    void testNeutralSentiment() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"text\":\"звичайний день\",\"sentiment\":\"НЕЙТРАЛЬНИЙ\",\"confidence\":0.78}")
                .addHeader("Content-Type", "application/json"));

        String result = aiService.predict("звичайний день нічого особливого");
        assertTrue(result.contains("НЕЙТРАЛЬНИЙ"));
    }
}