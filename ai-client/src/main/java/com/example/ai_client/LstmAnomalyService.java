package com.example.ai_client;

import ai.onnxruntime.*;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

@Service
public class LstmAnomalyService {

    private OrtEnvironment env;
    private OrtSession session;
    private float threshold;

    private final Deque<float[]> window = new ArrayDeque<>();
    private static final int WINDOW_SIZE = 10;
    private static final int FEATURES    = 3;

   
    private static final float[] FEATURE_MIN = {0f,   0f,  0f};
    private static final float[] FEATURE_MAX = {600f, 100f, 1f};

    @PostConstruct
    public void init() throws Exception {
        env = OrtEnvironment.getEnvironment();

        InputStream modelStream = getClass()
                .getResourceAsStream("/lstm_autoencoder.onnx");
        byte[] modelBytes = modelStream.readAllBytes();
        session = env.createSession(modelBytes,
                new OrtSession.SessionOptions());

        InputStream thresholdStream = getClass()
                .getResourceAsStream("/lstm_threshold.txt");
        String thresholdStr = new String(
                thresholdStream.readAllBytes(),
                StandardCharsets.UTF_8).trim();
        threshold = Float.parseFloat(thresholdStr);

        System.out.printf(
                "LSTM Autoencoder loaded successfully (threshold=%.6f)%n",
                threshold);
    }

    private float[] normalize(double latency, double cpu,
                              double errorRate) {
        float[] raw  = {(float) latency, (float) cpu, (float) errorRate};
        float[] norm = new float[FEATURES];
        for (int i = 0; i < FEATURES; i++) {
            norm[i] = (raw[i] - FEATURE_MIN[i]) /
                    (FEATURE_MAX[i] - FEATURE_MIN[i]);
        }
        return norm;
    }

    public void addMetric(double latency, double cpu,
                          double errorRate, double queueSize) {
        window.addLast(normalize(latency, cpu, errorRate));
        if (window.size() > WINDOW_SIZE) window.pollFirst();
    }

    public boolean isReady() {
        return window.size() == WINDOW_SIZE;
    }

    public Map<String, Object> detect() throws Exception {
        if (!isReady()) {
            return Map.of(
                    "status",              "INSUFFICIENT_DATA",
                    "is_anomaly",          false,
                    "action",              "NONE",
                    "reconstruction_error", 0.0
            );
        }

      
        float[][][] input = new float[1][WINDOW_SIZE][FEATURES];
        int i = 0;
        for (float[] vec : window) {
            input[0][i++] = vec;
        }

        OnnxTensor tensor = OnnxTensor.createTensor(env, input);
        OrtSession.Result result = session.run(
                Map.of("input", tensor));

        float[][][] output = (float[][][]) result.get(0).getValue();

        // Reconstruction error (MSE)
        double mse = 0;
        for (int t = 0; t < WINDOW_SIZE; t++) {
            for (int f = 0; f < FEATURES; f++) {
                double diff = input[0][t][f] - output[0][t][f];
                mse += diff * diff;
            }
        }
        mse /= (WINDOW_SIZE * FEATURES);

        boolean isAnomaly = mse > threshold;
        String  action    = isAnomaly
                ? decideAction(window.peekLast())
                : "NONE";

        return Map.of(
                "is_anomaly",           isAnomaly,
                "status",               isAnomaly ? "АНОМАЛІЯ" : "НОРМА",
                "reconstruction_error", Math.round(mse * 1_000_000.0) / 1_000_000.0,
                "threshold",            Math.round((double) threshold * 1_000_000.0) / 1_000_000.0,
                "action",               action
        );
    }

    private String decideAction(float[] last) {
        if (last == null) return "NONE";
      
        float latency   = last[0] * FEATURE_MAX[0];
        float cpu       = last[1] * FEATURE_MAX[1];
        float errorRate = last[2] * FEATURE_MAX[2];

        if (cpu > 90 || errorRate > 0.5f)  return "FALLBACK";
        if (latency > 200)                  return "SCALE_UP";
        if (errorRate > 0.1f)               return "RETRY";
        return "NONE";
    }
}
