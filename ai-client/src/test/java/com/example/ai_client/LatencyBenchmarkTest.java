
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
                    .setBody("{\"sentiment\":\"POSITIVE\",\"confidence\":0.87}")
                    .addHeader("Content-Type", "application/json"));
        }

        for (int i = 0; i < count; i++) {
            long start = System.nanoTime();
            aiService.predict("test");
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

    // Returns statistics in the following order: [min, max, avg, p95, p99]
    private double[] calcStats(long[] times) {
        Arrays.sort(times);

        double min = times[0] / 1_000_000.0;
        double max = times[times.length - 1] / 1_000_000.0;
        double avg = Arrays.stream(times).average().orElse(0) / 1_000_000.0;
        double p95 = times[percentileIndex(times.length, 0.95)] / 1_000_000.0;
        double p99 = times[percentileIndex(times.length, 0.99)] / 1_000_000.0;

        return new double[]{min, max, avg, p95, p99};
    }

    private int percentileIndex(int length, double percentile) {
        return Math.min((int) Math.ceil(length * percentile) - 1, length - 1);
    }

    @Test
    @DisplayName("Latency benchmark: REST vs ONNX")
    void latencyBenchmark() throws Exception {
        int[] counts = {10, 100, 500, 1000};

        System.out.println();
        System.out.println("Latency benchmark: REST vs ONNX");
        System.out.println("All values are reported in milliseconds.");
        System.out.printf("%-8s %-8s %10s %10s %10s %10s %10s%n",
                "N", "Type", "MIN", "AVG", "MAX", "P95", "P99");

        for (int count : counts) {
            double[] rest = measureRest(count);
            double[] onnx = measureOnnx(count);

            System.out.printf(Locale.US, "%-8d %-8s %10.2f %10.2f %10.2f %10.2f %10.2f%n",
                    count, "REST", rest[0], rest[2], rest[1], rest[3], rest[4]);

            System.out.printf(Locale.US, "%-8d %-8s %10.2f %10.2f %10.2f %10.2f %10.2f%n",
                    count, "ONNX", onnx[0], onnx[2], onnx[1], onnx[3], onnx[4]);
        }
    }
}

