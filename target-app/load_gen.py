import time, csv, sys, urllib.request, json

URL = "http://localhost:9090/work"
RPS = 10
DURATION = int(sys.argv[1]) if len(sys.argv) > 1 else 300
SLA_MS = 200
OUT = sys.argv[2] if len(sys.argv) > 2 else "run_log.csv"

print(f"Load gen: {RPS} RPS, {DURATION}s, SLA={SLA_MS}ms -> {OUT}")

with open(OUT, "w", newline="") as f:
    w = csv.writer(f)
    w.writerow(["ts", "elapsed_s", "latency_ms", "ok", "sla_violated"])
    start = time.time()
    total = 0
    violations = 0
    while time.time() - start < DURATION:
        t0 = time.perf_counter()
        try:
            req = urllib.request.urlopen(URL, timeout=5)
            ok = req.getcode() == 200
        except:
            ok = False
        lat = (time.perf_counter() - t0) * 1000
        elapsed = time.time() - start
        violated = (not ok) or (lat > SLA_MS)
        total += 1
        if violated:
            violations += 1
        w.writerow([f"{time.time():.3f}", f"{elapsed:.1f}", f"{lat:.1f}", ok, violated])
        if total % 50 == 0:
            print(f"  T+{elapsed:.0f}s: {total} reqs, {violations} violations ({100*violations/total:.1f}%)")
        time.sleep(max(0, 1/RPS - (time.perf_counter() - t0 - lat/1000)))

print(f"Done: {total} requests, {violations} SLA violations ({100*violations/total:.1f}%)")
