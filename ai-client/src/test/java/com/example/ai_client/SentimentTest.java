
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
    @DisplayName("Positive text -> POSITIVE")
    void testPositiveSentiment() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"text\":\"excellent\",\"sentiment\":\"POSITIVE\",\"confidence\":0.85}")
                .addHeader("Content-Type", "application/json"));

        String result = aiService.predict("excellent great super");
        assertTrue(result.contains("POSITIVE"));
    }

    @Test
    @DisplayName("Negative text -> NEGATIVE")
    void testNegativeSentiment() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"text\":\"terrible\",\"sentiment\":\"NEGATIVE\",\"confidence\":0.82}")
                .addHeader("Content-Type", "application/json"));

        String result = aiService.predict("terrible bad failure");
        assertTrue(result.contains("NEGATIVE"));
    }

    @Test
    @DisplayName("Neutral text -> NEUTRAL")
    void testNeutralSentiment() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"text\":\"ordinary day\",\"sentiment\":\"NEUTRAL\",\"confidence\":0.78}")
                .addHeader("Content-Type", "application/json"));

        String result = aiService.predict("ordinary day nothing special");
        assertTrue(result.contains("NEUTRAL"));
    }
}

