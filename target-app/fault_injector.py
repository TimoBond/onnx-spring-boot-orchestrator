import time, urllib.request, subprocess, sys

PORT = 9090
EVENTS = [
    (60,  "stress", 500),
    (120, "stress", 50),
    (150, "errors", 0.4),
    (210, "errors", 0.0),
    (240, "stress", 800),
    (270, "kill",   None),
    (285, "stress", 50),
]

print("Fault injector started")
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
    elif kind == "kill":
        subprocess.run(["kubectl", "delete", "pod", "-l", "app=target-app", "--field-selector=status.phase=Running", "--wait=false"], capture_output=True)
        print(f"  T+{elapsed:.0f}s: KILLED one pod")

print("Fault injection complete")
