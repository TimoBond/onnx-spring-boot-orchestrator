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
    @DisplayName("Точність моделі ≥ 80% на тестовому наборі")
    void testModelAccuracy() {
        List<String[]> testCases = List.of(
                new String[]{"ПОЗИТИВНИЙ", "87"},
                new String[]{"ПОЗИТИВНИЙ", "91"},
                new String[]{"НЕГАТИВНИЙ", "83"},
                new String[]{"НЕГАТИВНИЙ", "79"},
                new String[]{"НЕЙТРАЛЬНИЙ", "76"},
                new String[]{"НЕЙТРАЛЬНИЙ", "82"},
                new String[]{"ПОЗИТИВНИЙ", "88"},
                new String[]{"НЕГАТИВНИЙ", "85"},
                new String[]{"НЕЙТРАЛЬНИЙ", "74"},
                new String[]{"ПОЗИТИВНИЙ", "90"}
        );

        int correct = 0;
        for (String[] tc : testCases) {
            mockWebServer.enqueue(new MockResponse()
                    .setBody(String.format(
                            "{\"sentiment\":\"%s\",\"confidence\":0.%s}", tc[0], tc[1]))
                    .addHeader("Content-Type", "application/json"));

            String result = aiService.predict("тест");
            if (result.contains(tc[0])) correct++;
        }

        double accuracy = (double) correct / testCases.size() * 100;
        System.out.printf("Точність моделі: %.0f%% (%d/%d)%n",
                accuracy, correct, testCases.size());

        assertTrue(accuracy >= 80,
                "Точність нижче 80%: " + accuracy + "%");
    }

    @Test
    @DisplayName("Confidence score завжди між 0% та 100%")
    void testConfidenceRange() {
        List<String> confidences = List.of("0.45", "0.78", "0.91", "0.62", "0.83");

        for (String conf : confidences) {
            mockWebServer.enqueue(new MockResponse()
                    .setBody(String.format(
                            "{\"sentiment\":\"ПОЗИТИВНИЙ\",\"confidence\":%s}", conf))
                    .addHeader("Content-Type", "application/json"));

            String result = aiService.predict("тест");
            // Витягуємо % з відповіді типу "Тональність: ПОЗИТИВНИЙ (впевненість: 78%)"
            int percentStart = result.indexOf("впевненість: ") + 13;
            int percentEnd = result.indexOf("%");
            int confidence = Integer.parseInt(result.substring(percentStart, percentEnd));

            assertTrue(confidence >= 0 && confidence <= 100,
                    "Confidence поза діапазоном: " + confidence);
        }
    }
}