
package com.example.ai_client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
class RestVsOnnxBenchmarkTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OnnxAnomalyService onnxAnomalyService;

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

    private long benchmarkRest(int count) {
        for (int i = 0; i < count; i++) {
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"sentiment\":\"POSITIVE\",\"confidence\":0.87}")
                    .addHeader("Content-Type", "application/json"));
        }
        long start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            aiService.predict("test " + i);
        }
        return System.currentTimeMillis() - start;
    }

    private long benchmarkOnnx(int count) throws Exception {
        long start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            onnxAnomalyService.predict(52, 11, 0.02, 5);
        }
        return System.currentTimeMillis() - start;
    }

    @Test
    @DisplayName("Benchmark 10 requests: REST vs ONNX")
    void benchmark_10() throws Exception {
        long rest = benchmarkRest(10);
        long onnx = benchmarkOnnx(10);
        double restAvg = (double) rest / 10;
        double onnxAvg = (double) onnx / 10;
        System.out.printf("10 requests%n");
        System.out.printf("REST total: %dms | avg: %.2fms%n", rest, restAvg);
        System.out.printf("ONNX total: %dms | avg: %.2fms%n", onnx, onnxAvg);
        System.out.printf("ONNX is faster by %.1fx%n%n", restAvg / onnxAvg);
        assertTrue(onnx <= rest + 100);
    }

    @Test
    @DisplayName("Benchmark 100 requests: REST vs ONNX")
    void benchmark_100() throws Exception {
        long rest = benchmarkRest(100);
        long onnx = benchmarkOnnx(100);
        double restAvg = (double) rest / 100;
        double onnxAvg = (double) onnx / 100;
        System.out.printf("100 requests%n");
        System.out.printf("REST total: %dms | avg: %.2fms%n", rest, restAvg);
        System.out.printf("ONNX total: %dms | avg: %.2fms%n", onnx, onnxAvg);
        System.out.printf("ONNX is faster by %.1fx%n%n", restAvg / onnxAvg);
        assertTrue(onnx <= rest + 100);
    }

    @Test
    @DisplayName("Benchmark 1000 requests: REST vs ONNX")
    void benchmark_1000() throws Exception {
        long rest = benchmarkRest(1000);
        long onnx = benchmarkOnnx(1000);
        double restAvg = (double) rest / 1000;
        double onnxAvg = (double) onnx / 1000;
        System.out.printf("1000 requests%n");
        System.out.printf("REST total: %dms | avg: %.2fms%n", rest, restAvg);
        System.out.printf("ONNX total: %dms | avg: %.2fms%n", onnx, onnxAvg);
        System.out.printf("ONNX is faster by %.1fx%n%n", restAvg / onnxAvg);
        assertTrue(onnx <= rest + 500);
    }
}

