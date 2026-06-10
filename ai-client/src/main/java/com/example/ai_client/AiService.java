package com.example.ai_client;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

@Service
public class AiService {

    private final WebClient aiWebClient;

    public AiService(WebClient aiWebClient) {
        this.aiWebClient = aiWebClient;
    }

    public String predict(String text) {
        try {
            return aiWebClient.post()
                    .uri("/predict")
                    .bodyValue(Map.of("text", text))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .map(r -> String.format(
                            "Тональність: %s (впевненість: %s%%)",
                            r.get("sentiment"),
                            Math.round(Double.parseDouble(r.get("confidence").toString()) * 100)
                    ))
                    .block();
        } catch (Exception e) {
            return "AI сервіс недоступний. Спробуйте пізніше.";
        }
    }
}