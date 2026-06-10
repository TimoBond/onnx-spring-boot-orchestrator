package com.example.ai_client;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/anomaly")
public class OnnxAnomalyController {

    private final OnnxAnomalyService onnxAnomalyService;

    public OnnxAnomalyController(OnnxAnomalyService onnxAnomalyService) {
        this.onnxAnomalyService = onnxAnomalyService;
    }

    @PostMapping("/detect")
    public ResponseEntity<Map<String, Object>> detect(
            @RequestBody Map<String, Double> body) throws Exception {

        double latency   = body.get("latency");
        double cpu       = body.get("cpu_usage");
        double errorRate = body.get("error_rate");
        double queueSize = body.get("queue_size");

        Map<String, Object> result = onnxAnomalyService.predict(
                latency, cpu, errorRate, queueSize);

        return ResponseEntity.ok(result);
    }
}