package com.example.ai_client;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class PredictController {

    private final AiService aiService;
    private final RuntimeAnomalyDetector detector;

    public PredictController(AiService aiService, RuntimeAnomalyDetector detector) {
        this.aiService = aiService;
        this.detector  = detector;
    }

    @PostMapping("/api/predict")
    public ResponseEntity<String> predict(@RequestBody Map<String, String> body) {
        long start = System.currentTimeMillis();
        boolean isError = false;
        try {
            String result = aiService.predict(body.get("text"));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            isError = true;
            return ResponseEntity.status(500).body("Error");
        } finally {
            detector.recordRequest(System.currentTimeMillis() - start, isError);
        }
    }
}