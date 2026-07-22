package com.example.ai_client;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class KubernetesScaleExecutor {

    private final KubernetesClient kubernetesClient;
    private static final Logger log = LoggerFactory.getLogger(KubernetesScaleExecutor.class);

   
    private static final String TARGET_DEPLOYMENT = "target-app";  
    private static final String NAMESPACE = "default";

    public KubernetesScaleExecutor() {
        this.kubernetesClient = new KubernetesClientBuilder().build();
    }

    public ScaleResult execute(String action, int currentReplicas) {
        long startTime = System.nanoTime();

        if ("SCALE_UP".equals(action)) {
            int desired = currentReplicas + 1;
            kubernetesClient.apps().deployments()
                    .inNamespace(NAMESPACE)
                    .withName(TARGET_DEPLOYMENT)
                    .scale(desired);

            boolean ready = waitForReplicas(desired, 60);
            long elapsed = (System.nanoTime() - startTime) / 1_000_000;

            log.info("SCALE_UP: {} → {} replicas, ready={}, took {} ms",
                    currentReplicas, desired, ready, elapsed);

            return new ScaleResult(action, currentReplicas, desired, ready, elapsed);
        }

        return new ScaleResult("NONE", currentReplicas, currentReplicas, true, 0);
    }

    private boolean waitForReplicas(int desired, int timeoutSeconds) {
        for (int i = 0; i < timeoutSeconds; i++) {
            var deployment = kubernetesClient.apps().deployments()
                    .inNamespace(NAMESPACE)
                    .withName(TARGET_DEPLOYMENT)
                    .get();
            Integer readyReplicas = deployment.getStatus().getReadyReplicas();
            if (readyReplicas != null && readyReplicas >= desired) {
                return true;
            }
            try { Thread.sleep(1000); } catch (InterruptedException e) { break; }
        }
        return false;
    }
}
