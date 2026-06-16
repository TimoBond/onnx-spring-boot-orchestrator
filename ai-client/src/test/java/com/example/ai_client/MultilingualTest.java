
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
    @DisplayName("Ukrainian language - positive sentiment")
    void testUkrainianPositive() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"text\":\"чудово\",\"sentiment\":\"POSITIVE\",\"confidence\":0.85}")
                .addHeader("Content-Type", "application/json"));

        String result = aiService.predict("чудово відмінно");

        assertTrue(
                result.contains("POSITIVE"),
                "The Ukrainian positive phrase was not recognized"
        );
    }

    @Test
    @DisplayName("English language - positive sentiment")
    void testEnglishPositive() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"text\":\"excellent\",\"sentiment\":\"POSITIVE\",\"confidence\":0.88}")
                .addHeader("Content-Type", "application/json"));

        String result = aiService.predict("excellent amazing wonderful");

        assertTrue(
                result.contains("POSITIVE"),
                "The English positive phrase was not recognized"
        );
    }

    @Test
    @DisplayName("Ukrainian language - negative sentiment")
    void testUkrainianNegative() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"text\":\"жахливо\",\"sentiment\":\"NEGATIVE\",\"confidence\":0.83}")
                .addHeader("Content-Type", "application/json"));

        String result = aiService.predict("жахливо погано");

        assertTrue(
                result.contains("NEGATIVE"),
                "The Ukrainian negative phrase was not recognized"
        );
    }

    @Test
    @DisplayName("English language - negative sentiment")
    void testEnglishNegative() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"text\":\"terrible\",\"sentiment\":\"NEGATIVE\",\"confidence\":0.86}")
                .addHeader("Content-Type", "application/json"));

        String result = aiService.predict("terrible horrible awful");

        assertTrue(
                result.contains("NEGATIVE"),
                "The English negative phrase was not recognized"
        );
    }
}


