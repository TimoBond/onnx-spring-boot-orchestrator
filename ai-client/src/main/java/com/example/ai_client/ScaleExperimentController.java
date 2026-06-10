package com.example.ai_client;


import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/experiment")
public class ScaleExperimentController {

    private final OnnxAnomalyService onnxAnomalyService;
    private final KubernetesScaleExecutor scaleExecutor;

    public ScaleExperimentController(OnnxAnomalyService onnxAnomalyService,
                                     KubernetesScaleExecutor scaleExecutor) {
        this.onnxAnomalyService = onnxAnomalyService;
        this.scaleExecutor = scaleExecutor;
    }

    @PostMapping("/full-cycle")
    public Map<String, Object> runFullCycle(
            @RequestParam double latency,
            @RequestParam double cpu,
            @RequestParam double errorRate,
            @RequestParam double queueSize) throws Exception {

        long t0 = System.nanoTime();

        // 1. ONNX inference (IsolationForest)
        long t1 = System.nanoTime();
        Map<String, Object> anomaly = onnxAnomalyService.predict(
                latency, cpu, errorRate, queueSize);
        long inferenceMs = (System.nanoTime() - t1) / 1_000_000;

        boolean isAnomaly = (boolean) anomaly.get("is_anomaly");

        // 2. Decision (та сама логіка що в RuntimeAnomalyDetector)
        String action = "NONE";
        if (isAnomaly) {
            if (cpu > 90 || errorRate > 0.5)   action = "FALLBACK";
            else if (latency > 200)             action = "SCALE_UP";
            else if (errorRate > 0.1)           action = "RETRY";
        }

        // 3. K8s action
        ScaleResult scaleResult = scaleExecutor.execute(action, 1);

        long totalMs = (System.nanoTime() - t0) / 1_000_000;

        return Map.of(
                "anomaly", isAnomaly,
                "action", action,
                "inferenceMs", inferenceMs,
                "scaleResult", scaleResult,
                "totalCycleMs", totalMs
        );
    }
}