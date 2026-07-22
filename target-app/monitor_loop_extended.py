import time, csv, sys, urllib.request, json, datetime

TARGET = "http://localhost:9091/work"
ORCH = "http://localhost:8080/api/experiment/full-cycle"
INTERVAL = 5
DURATION = int(sys.argv[1]) if len(sys.argv) > 1 else 300
PROBES = 5
OUT = sys.argv[2] if len(sys.argv) > 2 else "monitor_log.csv"

print(f"Monitor: every {INTERVAL}s, {PROBES} probes, {DURATION}s total -> {OUT}")

with open(OUT, "w", newline="") as f:
    writer = csv.writer(f)
    # Розширено порівняно з оригіналом: додано timestamp (ISO) та action
    # для подальшого ground-truth аналізу (detection delay, false-action rate).
    writer.writerow(["cycle", "timestamp_iso", "elapsed_s", "avg_latency_ms",
                      "error_rate", "is_anomaly", "action", "inference_ms",
                      "total_cycle_ms"])

    start = time.time()
    cycle = 0
    while time.time() - start < DURATION:
        cycle += 1
        lats = []
        errors = 0
        for _ in range(PROBES):
            t0 = time.perf_counter()
            try:
                r = urllib.request.urlopen(TARGET, timeout=5)
                ok = r.getcode() == 200
            except:
                ok = False
            lat = (time.perf_counter() - t0) * 1000
            lats.append(lat)
            if not ok:
                errors += 1

        avg_lat = sum(lats) / len(lats)
        err_rate = errors / PROBES

        params = f"latency={avg_lat:.0f}&cpu=50&errorRate={err_rate}&queueSize=0"
        is_anomaly = None
        action = "?"
        inference_ms = None
        total_cycle_ms = None
        try:
            req = urllib.request.urlopen(
                urllib.request.Request(f"{ORCH}?{params}", method="POST"), timeout=10)
            resp = json.loads(req.read())
            action = resp.get("action", "?")
            is_anomaly = resp.get("anomaly")
            inference_ms = resp.get("inferenceMs")
            total_cycle_ms = resp.get("totalCycleMs")
        except Exception as e:
            action = f"ERR:{e}"

        elapsed = time.time() - start
        ts_iso = datetime.datetime.utcnow().isoformat()

        writer.writerow([cycle, ts_iso, f"{elapsed:.1f}", f"{avg_lat:.1f}",
                          f"{err_rate:.2f}", is_anomaly, action,
                          inference_ms, total_cycle_ms])
        f.flush()

        print(f"  [{cycle}] T+{elapsed:.0f}s lat={avg_lat:.0f}ms err={err_rate:.0%} -> {action}")
        time.sleep(max(0, INTERVAL - (time.time() - start - elapsed)))

print("Monitor done")
