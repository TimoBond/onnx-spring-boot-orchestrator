import time, urllib.request, subprocess, sys

PORT = 9090

# Full 900s timeline combining original self-inflicted events with all three
# confirmed Chaos Mesh scenarios. "kill" is omitted because pod deletion
# breaks any active kubectl port-forward tunnel used for local measurement.

EVENTS = [
    # Original self-inflicted events (unchanged from paper's Section 3.8)
    (60,  "stress", 500),
    (120, "stress", 50),
    (150, "errors", 0.4),
    (210, "errors", 0.0),
    (240, "stress", 800),
    (270, "stress", 50),

    # New Chaos Mesh scenarios (confirmed working individually)
    (330, "chaos_apply",  "01-slow-rate-latency.yaml"),
    (500, "chaos_delete", "01-slow-rate-latency.yaml"),

    (560, "chaos_apply",  "02-resource-exhaustion.yaml"),
    (680, "chaos_delete", "02-resource-exhaustion.yaml"),   # скорочено з 720 -> більше буфера на відновлення

    (740, "chaos_apply",  "03-dependency-failure.yaml"),
    (830, "chaos_delete", "03-dependency-failure.yaml"),
    # Останні ~120-170с (830-950/1000s) залишені як буфер відновлення перед кінцем вимірювання
]

print("Fault injector started (FULL RUN - 900s)")
start = time.time()
for t, kind, arg in EVENTS:
    wait = start + t - time.time()
    if wait > 0:
        time.sleep(wait)
    elapsed = time.time() - start

    if kind == "stress":
        for _ in range(5):
            try: urllib.request.urlopen(urllib.request.Request(f"http://localhost:{PORT}/stress?ms={arg}", method="POST"), timeout=2)
            except: pass
        print(f"  T+{elapsed:.0f}s: STRESS -> {arg}ms")

    elif kind == "errors":
        for _ in range(5):
            try: urllib.request.urlopen(urllib.request.Request(f"http://localhost:{PORT}/errors?rate={arg}", method="POST"), timeout=2)
            except: pass
        print(f"  T+{elapsed:.0f}s: ERRORS -> {arg}")

    elif kind == "chaos_apply":
        result = subprocess.run(["kubectl", "apply", "-f", arg], capture_output=True, text=True)
        status = "OK" if result.returncode == 0 else f"FAIL: {result.stderr.strip()}"
        print(f"  T+{elapsed:.0f}s: CHAOS APPLY {arg} -> {status}")

    elif kind == "chaos_delete":
        result = subprocess.run(["kubectl", "delete", "-f", arg], capture_output=True, text=True)
        status = "OK" if result.returncode == 0 else f"FAIL: {result.stderr.strip()}"
        print(f"  T+{elapsed:.0f}s: CHAOS DELETE {arg} -> {status}")

    else:
        print(f"  T+{elapsed:.0f}s: UNKNOWN EVENT KIND {kind}")

print("Fault injection complete (full run)")
