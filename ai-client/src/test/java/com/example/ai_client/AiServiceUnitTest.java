
package com.example.ai_client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;

class AiServiceUnitTest {

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
    @DisplayName("AiService returns sentiment from the Python service")
    void testPredict_returnsResultFromPythonService() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"sentiment\":\"POSITIVE\",\"confidence\":0.87}")
                .addHeader("Content-Type", "application/json"));

        String result = aiService.predict("test");

        assertTrue(result.contains("POSITIVE"));
        assertTrue(result.contains("87%"));
    }
}

