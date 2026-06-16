
package com.example.ai_client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;

class ResilienceTest {

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
    @DisplayName("The system returns fallback when the Python service is unavailable")
    void testPredict_fallbackWhenServiceDown() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        String result = aiService.predict("test");

        assertEquals("The AI service is unavailable. Please try again later.", result);
    }

    @Test
    @DisplayName("The system returns fallback on timeout")
    void testPredict_fallbackWhenTimeout() throws Exception {
        mockWebServer.shutdown(); // simulate that the service has completely failed

        String result = aiService.predict("test");

        assertEquals("The AI service is unavailable. Please try again later.", result);
    }
}

