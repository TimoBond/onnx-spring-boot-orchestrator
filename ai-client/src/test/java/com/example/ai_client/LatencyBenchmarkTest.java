package com.example.ai_client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@SpringBootTest
class LatencyBenchmarkTest {

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

    private double[] measureRest(int count) {
        long[] times = new long[count];
        for (int i = 0; i < count; i++) {
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"sentiment\":\"ПОЗИТИВНИЙ\",\"confidence\":0.87}")
                    .addHeader("Content-Type", "application/json"));
        }
        for (int i = 0; i < count; i++) {
            long start = System.nanoTime();
            aiService.predict("тест");
            times[i] = System.nanoTime() - start;
        }
        return calcStats(times);
    }

    private double[] measureOnnx(int count) throws Exception {
        long[] times = new long[count];
        for (int i = 0; i < count; i++) {
            long start = System.nanoTime();
            onnxAnomalyService.predict(52, 11, 0.02, 5);
            times[i] = System.nanoTime() - start;
        }
        return calcStats(times);
    }

    // [min, max, avg, p95, p99]
    private double[] calcStats(long[] times) {
        Arrays.sort(times);
        double min = times[0] / 1_000_000.0;
        double max = times[times.length - 1] / 1_000_000.0;
        double avg = Arrays.stream(times).average().orElse(0) / 1_000_000.0;
        double p95 = times[(int)(times.length * 0.95)] / 1_000_000.0;
        double p99 = times[(int)(times.length * 0.99)] / 1_000_000.0;
        return new double[]{min, max, avg, p95, p99};
    }

    @Test
    @DisplayName("Latency Benchmark: REST vs ONNX")
    void latencyBenchmark() throws Exception {
        int[] counts = {10, 100, 500, 1000};

        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║           LATENCY BENCHMARK: REST vs ONNX (мс)              ║");
        System.out.println("╠══════╦═══════╦════════╦════════╦════════╦════════╦═══════════╣");
        System.out.println("║      ║       ║  MIN   ║  AVG   ║  MAX   ║  P95   ║   P99     ║");
        System.out.println("╠══════╬═══════╬════════╬════════╬════════╬════════╬═══════════╣");

        for (int count : counts) {
            double[] rest = measureRest(count);
            double[] onnx = measureOnnx(count);

            System.out.printf("║ %-4d ║ REST  ║ %6.2f ║ %6.2f ║ %6.2f ║ %6.2f ║ %9.2f ║%n",
                    count, rest[0], rest[2], rest[1], rest[3], rest[4]);
            System.out.printf("║      ║ ONNX  ║ %6.2f ║ %6.2f ║ %6.2f ║ %6.2f ║ %9.2f ║%n",
                    onnx[0], onnx[2], onnx[1], onnx[3], onnx[4]);
            System.out.println("╠══════╬═══════╬════════╬════════╬════════╬════════╬═══════════╣");
        }
        System.out.println("╚══════╩═══════╩════════╩════════╩════════╩════════╩═══════════╝");
    }
}