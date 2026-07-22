package com.example.ai_client;

import ai.onnxruntime.*;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.Map;

@Service
public class OnnxAnomalyService {

    private OrtEnvironment env;
    private OrtSession session;

    private final double[] mean = {141.857, 33.286, 0.191, 26.786};
    private final double[] std  = {148.754, 34.792, 0.293, 34.720};


    @PostConstruct
    public void init() throws Exception {
        env = OrtEnvironment.getEnvironment();
        InputStream is = getClass().getResourceAsStream("/isolation_forest.onnx");
        byte[] modelBytes = is.readAllBytes();
        session = env.createSession(modelBytes, new OrtSession.SessionOptions());
        System.out.println("✅ ONNX модель завантажена в Spring Boot!");
    }

    private String decideAction(double latency, double cpu,
                                double errorRate, double queueSize) {
        
        if (cpu > 90 || errorRate > 0.5) {
            return "FALLBACK";
        }
       
        if (latency > 200 || queueSize > 50) {
            return "SCALE_UP";
        }
     
        if (errorRate > 0.1) {
            return "RETRY";
        }
        return "NONE";
    }

    public Map<String, Object> predict(double latency, double cpu,
                                       double errorRate, double queueSize) throws Exception {
        float[] input = {
                (float)((latency   - mean[0]) / std[0]),
                (float)((cpu       - mean[1]) / std[1]),
                (float)((errorRate - mean[2]) / std[2]),
                (float)((queueSize - mean[3]) / std[3])
        };

        float[][] inputData = {input};
        OnnxTensor tensor = OnnxTensor.createTensor(env, inputData);
        OrtSession.Result result = session.run(Map.of("float_input", tensor));
        long[][] labels = (long[][]) result.get(0).getValue();
        boolean isAnomaly = labels[0][0] == -1;

        String action = isAnomaly
                ? decideAction(latency, cpu, errorRate, queueSize)
                : "NONE";

        return Map.of(
                "is_anomaly",  isAnomaly,
                "status",      isAnomaly ? "ANOMALY" : "NORMAL",
                "action",      action,
                "latency_ms",  latency,
                "cpu_usage",   cpu,
                "error_rate",  errorRate,
                "queue_size",  queueSize
        );
    }
}
