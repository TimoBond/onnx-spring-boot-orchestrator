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
    @DisplayName("Система повертає fallback коли Python сервіс недоступний")
    void testPredict_fallbackWhenServiceDown() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        String result = aiService.predict("тест");

        assertEquals("AI сервіс недоступний. Спробуйте пізніше.", result);
    }

    @Test
    @DisplayName("Система повертає fallback при таймауті")
    void testPredict_fallbackWhenTimeout() throws Exception {
        mockWebServer.shutdown(); // симулюємо що сервіс повністю впав

        String result = aiService.predict("тест");

        assertEquals("AI сервіс недоступний. Спробуйте пізніше.", result);
    }
}