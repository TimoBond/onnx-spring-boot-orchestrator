
package com.example.ai_client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;

class PerformanceTest {

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

    private long runRequests(int count) {
        for (int i = 0; i < count; i++) {
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"sentiment\":\"POSITIVE\",\"confidence\":0.87}")
                    .addHeader("Content-Type", "application/json"));
        }

        long start = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            String result = aiService.predict("request " + i);

            assertNotNull(result);
            assertTrue(result.contains("POSITIVE"));
            assertTrue(result.contains("87%"));
        }

        return System.currentTimeMillis() - start;
    }

    @Test
    @DisplayName("Latency - 10 requests")
    void test_10_requests() {
        long total = runRequests(10);
        double avg = (double) total / 10;

        System.out.printf(
                "10 requests | Total: %d ms | Average latency: %.2f ms%n",
                total,
                avg
        );

        assertTrue(avg < 500);
    }

    @Test
    @DisplayName("Latency - 100 requests")
    void test_100_requests() {
        long total = runRequests(100);
        double avg = (double) total / 100;

        System.out.printf(
                "100 requests | Total: %d ms | Average latency: %.2f ms%n",
                total,
                avg
        );

        assertTrue(avg < 500);
    }

    @Test
    @DisplayName("Latency - 500 requests")
    void test_500_requests() {
        long total = runRequests(500);
        double avg = (double) total / 500;

        System.out.printf(
                "500 requests | Total: %d ms | Average latency: %.2f ms%n",
                total,
                avg
        );

        assertTrue(avg < 500);
    }

    @Test
    @DisplayName("Latency - 1000 requests")
    void test_1000_requests() {
        long total = runRequests(1000);
        double avg = (double) total / 1000;

        System.out.printf(
                "1000 requests | Total: %d ms | Average latency: %.2f ms%n",
                total,
                avg
        );

        assertTrue(avg < 500);
    }
}


}
