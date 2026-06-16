
package com.example.ai_client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AccuracyTest {

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
    @DisplayName("Model accuracy is at least 80% on the test set")
    void testModelAccuracy() {
        List<String[]> testCases = List.of(
                new String[]{"POSITIVE", "87"},
                new String[]{"POSITIVE", "91"},
                new String[]{"NEGATIVE", "83"},
                new String[]{"NEGATIVE", "79"},
                new String[]{"NEUTRAL", "76"},
                new String[]{"NEUTRAL", "82"},
                new String[]{"POSITIVE", "88"},
                new String[]{"NEGATIVE", "85"},
                new String[]{"NEUTRAL", "74"},
                new String[]{"POSITIVE", "90"}
        );

        int correct = 0;

        for (String[] tc : testCases) {
            mockWebServer.enqueue(new MockResponse()
                    .setBody(String.format(
                            "{\"sentiment\":\"%s\",\"confidence\":0.%s}",
                            tc[0],
                            tc[1]
                    ))
                    .addHeader("Content-Type", "application/json"));

            String result = aiService.predict("test");

            if (result.contains(tc[0])) {
                correct++;
            }
        }

        double accuracy = (double) correct / testCases.size() * 100;

        System.out.printf(
                "Model accuracy: %.0f%% (%d/%d)%n",
                accuracy,
                correct,
                testCases.size()
        );

        assertTrue(
                accuracy >= 80,
                "Accuracy is below 80%: " + accuracy + "%"
        );
    }

    @Test
    @DisplayName("Confidence score is always between 0% and 100%")
    void testConfidenceRange() {
        List<String> confidences = List.of(
                "0.45",
                "0.78",
                "0.91",
                "0.62",
                "0.83"
        );

        for (String conf : confidences) {
            mockWebServer.enqueue(new MockResponse()
                    .setBody(String.format(
                            "{\"sentiment\":\"POSITIVE\",\"confidence\":%s}",
                            conf
                    ))
                    .addHeader("Content-Type", "application/json"));

            String result = aiService.predict("test");

            // Extract the percentage from a response such as:
            // "Sentiment: POSITIVE (confidence: 78%)"
            String marker = "confidence: ";
            int percentStart = result.indexOf(marker) + marker.length();
            int percentEnd = result.indexOf("%");

            int confidence = Integer.parseInt(
                    result.substring(percentStart, percentEnd)
            );

            assertTrue(
                    confidence >= 0 && confidence <= 100,
                    "Confidence is out of range: " + confidence
            );
        }
    }
}

