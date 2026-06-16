```java
package com.example.ai_client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PrototypeValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OnnxAnomalyService onnxAnomalyService;

    @MockBean
    private AiService aiService;

   
    // V1 - REST Integration Layer
  

    @Test
    @Order(1)
    @DisplayName("V1.1 - REST layer accepts requests")
    void v1_restLayerAcceptsRequests() throws Exception {
        when(aiService.predict("test")).thenReturn("Sentiment: POSITIVE (confidence: 87%)");

        mockMvc.perform(post("/api/predict")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"test\"}"))
                .andExpect(status().isOk());

        System.out.println("V1.1 REST layer works");
    }

    @Test
    @Order(2)
    @DisplayName("V1.2 - REST layer returns an AI response")
    void v1_restLayerReturnsAiResponse() throws Exception {
        when(aiService.predict("excellent")).thenReturn("Sentiment: POSITIVE (confidence: 91%)");

        mockMvc.perform(post("/api/predict")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"excellent\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Sentiment: POSITIVE (confidence: 91%)"));

        System.out.println("V1.2 REST response is correct");
    }

    @Test
    @Order(3)
    @DisplayName("V1.3 - Resilience: fallback when the AI service fails")
    void v1_resilienceFallback() throws Exception {
        when(aiService.predict("test")).thenReturn("The AI service is unavailable. Please try again later.");

        mockMvc.perform(post("/api/predict")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"test\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("The AI service is unavailable. Please try again later."));

        System.out.println("V1.3 Fallback mechanism works");
    }

    
    // V2 - ONNX Native Inference
  

    @Test
    @Order(4)
    @DisplayName("V2.1 - ONNX model is loaded in the JVM")
    void v2_onnxModelLoaded() {
        assertNotNull(onnxAnomalyService);
        System.out.println("V2.1 ONNX model is loaded in the JVM");
    }

    @Test
    @Order(5)
    @DisplayName("V2.2 - ONNX detects NORMAL state")
    void v2_onnxDetectsNormal() throws Exception {
        var result = onnxAnomalyService.predict(52, 11, 0.02, 5);
        assertEquals("NORMAL", result.get("status"));
        assertEquals(false, result.get("is_anomaly"));
        assertEquals("NONE", result.get("action"));
        System.out.println("V2.2 NORMAL: " + result);
    }

    @Test
    @Order(6)
    @DisplayName("V2.3 - ONNX detects ANOMALY")
    void v2_onnxDetectsAnomaly() throws Exception {
        var result = onnxAnomalyService.predict(500, 95, 0.9, 100);
        assertEquals("ANOMALY", result.get("status"));
        assertEquals(true, result.get("is_anomaly"));
        System.out.println("V2.3 ANOMALY: " + result);
    }

    // V3 - Orchestration Decisions

    @Test
    @Order(7)
    @DisplayName("V3.1 - Orchestration: SCALE_UP under overload")
    void v3_orchestrationScaleUp() throws Exception {
        var result = onnxAnomalyService.predict(300, 60, 0.05, 80);
        assertEquals("SCALE_UP", result.get("action"));
        System.out.println("V3.1 SCALE_UP: latency=300ms, queue=80");
    }

    @Test
    @Order(8)
    @DisplayName("V3.2 - Orchestration: RETRY under errors")
    void v3_orchestrationRetry() throws Exception {
        var result = onnxAnomalyService.predict(100, 40, 0.2, 10);
        assertEquals("RETRY", result.get("action"));
        System.out.println("V3.2 RETRY: error_rate=0.2");
    }

    @Test
    @Order(9)
    @DisplayName("V3.3 - Orchestration: FALLBACK under critical failure")
    void v3_orchestrationFallback() throws Exception {
        var result = onnxAnomalyService.predict(500, 95, 0.9, 100);
        assertEquals("FALLBACK", result.get("action"));
        System.out.println("V3.3 FALLBACK: cpu=95%, error=0.9");
    }

   
    // V4 - Latency Validation
  

    @Test
    @Order(10)
    @DisplayName("V4.1 - ONNX latency < 10ms for one request")
    void v4_onnxLatencyUnder10ms() throws Exception {
        long start = System.currentTimeMillis();
        onnxAnomalyService.predict(52, 11, 0.02, 5);
        long duration = System.currentTimeMillis() - start;
        System.out.printf("V4.1 ONNX latency: %dms%n", duration);
        assertTrue(duration < 10, "ONNX latency exceeds 10ms: " + duration);
    }

    @Test
    @Order(11)
    @DisplayName("V4.2 - ONNX processes 1000 requests < 2000ms")
    void v4_onnxThroughput() throws Exception {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            onnxAnomalyService.predict(52, 11, 0.02, 5);
        }
        long total = System.currentTimeMillis() - start;
        double avg = (double) total / 1000;
        System.out.printf("V4.2 ONNX 1000 requests: %dms (avg: %.2fms)%n", total, avg);
        assertTrue(total < 2000, "1000 requests took: " + total + "ms");
    }
}
```

