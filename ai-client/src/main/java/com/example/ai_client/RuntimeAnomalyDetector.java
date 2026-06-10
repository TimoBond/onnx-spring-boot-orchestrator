package com.example.ai_client;

import com.sun.management.OperatingSystemMXBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RuntimeAnomalyDetector {

    private final LstmAnomalyService lstmAnomalyService;
    private final OperatingSystemMXBean osMxBean;
    private final MemoryMXBean memoryMxBean;
    private final ThreadMXBean threadMxBean;

    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong errorRequests  = new AtomicLong(0);
    private final AtomicLong totalLatencyMs = new AtomicLong(0);
    private volatile int cycleCount = 0;

    public RuntimeAnomalyDetector(LstmAnomalyService lstmAnomalyService) {
        this.lstmAnomalyService = lstmAnomalyService;
        this.osMxBean    = (OperatingSystemMXBean)
                ManagementFactory.getOperatingSystemMXBean();
        this.memoryMxBean = ManagementFactory.getMemoryMXBean();
        this.threadMxBean = ManagementFactory.getThreadMXBean();
    }

    public void recordRequest(long latencyMs, boolean isError) {
        totalRequests.incrementAndGet();
        totalLatencyMs.addAndGet(latencyMs);
        if (isError) errorRequests.incrementAndGet();
    }

    @Scheduled(fixedDelay = 5000)
    public void detectAnomalies() {
        try {
            cycleCount++;

            // Реальні JVM метрики
            double cpuLoad = osMxBean.getProcessCpuLoad();
            double cpu = (cpuLoad < 0) ? 5.0 : cpuLoad * 100;

            long heapUsed = memoryMxBean.getHeapMemoryUsage().getUsed();
            long heapMax  = memoryMxBean.getHeapMemoryUsage().getMax();
            double heapPct = (heapMax > 0) ? (heapUsed * 100.0 / heapMax) : 0;

            int threadCount = threadMxBean.getThreadCount();

            long reqCount = totalRequests.getAndSet(0);
            long latSum   = totalLatencyMs.getAndSet(0);
            long errCount = errorRequests.getAndSet(0);

            double avgLatency = (reqCount > 0) ? (double) latSum / reqCount : 50.0;
            double errorRate  = (reqCount > 0) ? (double) errCount / reqCount : 0.01;

            // Додаємо у ковзне вікно LSTM
            lstmAnomalyService.addMetric(
                    avgLatency, cpu, errorRate, threadCount);

            if (!lstmAnomalyService.isReady()) {
                System.out.printf(
                        " [Цикл %d] Збір даних: %d/10%n",
                        cycleCount, cycleCount);
                return;
            }

            // LSTM інференс
            Map<String, Object> result = lstmAnomalyService.detect();

            boolean isAnomaly = (boolean) result.get("is_anomaly");
            String  status    = (String)  result.get("status");
            String  action    = (String)  result.get("action");
            double  error     = (double)  result.get("reconstruction_error");
            double  thr       = (double)  result.get("threshold");

            String icon = isAnomaly ? "!" : "+";

            System.out.printf(
                    "%s [Цикл %d] %s | " +
                            "latency=%.0fms cpu=%.1f%% heap=%.1f%% threads=%d err=%.2f | " +
                            "MSE=%.6f/%.6f%s%n",
                    icon, cycleCount, status,
                    avgLatency, cpu, heapPct, threadCount, errorRate,
                    error, thr,
                    isAnomaly ? " | Дія: " + action : ""
            );

            if (isAnomaly) executeAction(action);

        } catch (Exception e) {
            System.err.println("! Помилка: " + e.getMessage());
        }
    }

    private void executeAction(String action) {
        switch (action) {
            case "SCALE_UP" -> System.out.println(
                    "   SCALE_UP  → збільшення кількості подів");
            case "RETRY"    -> System.out.println(
                    "  RETRY     → повторний запит");
            case "FALLBACK" -> System.out.println(
                    "  FALLBACK  → деградований режим");
            default         -> {}
        }
    }
}