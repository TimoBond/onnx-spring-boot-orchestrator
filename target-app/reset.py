import urllib.request, subprocess, time

print("Resetting...")
for _ in range(5):
    try: urllib.request.urlopen(urllib.request.Request("http://localhost:9090/stress?ms=50", method="POST"), timeout=2)
    except: pass
    try: urllib.request.urlopen(urllib.request.Request("http://localhost:9090/errors?rate=0", method="POST"), timeout=2)
    except: pass

subprocess.run(["kubectl", "scale", "deployment", "target-app", "--replicas=2"], capture_output=True)
print("Waiting 15s for pods...")
time.sleep(15)
print("Ready!")
