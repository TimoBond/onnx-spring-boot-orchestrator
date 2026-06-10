from flask import Flask, request, jsonify
import time, random

app = Flask(__name__)
stress_ms = 50
error_rate = 0.0

@app.route("/work")
def work():
    global stress_ms, error_rate
    if random.random() < error_rate:
        return jsonify(status="error"), 500
    time.sleep(stress_ms / 1000.0)
    return jsonify(status="ok", latency_ms=stress_ms)

@app.route("/stress", methods=["POST"])
def stress():
    global stress_ms
    stress_ms = int(request.args.get("ms", 50))
    return jsonify(stress_ms=stress_ms)

@app.route("/errors", methods=["POST"])
def errors():
    global error_rate
    error_rate = float(request.args.get("rate", 0.0))
    return jsonify(error_rate=error_rate)

@app.route("/health")
def health():
    return "ok"

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8080)
