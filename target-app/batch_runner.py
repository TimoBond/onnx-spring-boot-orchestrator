import argparse
import subprocess
import sys
import time
from pathlib import Path

TARGET_APP_LABEL = "app=target-app"
PORT_FORWARD_9090 = ["kubectl", "port-forward", "svc/target-app", "9090:8080"]
PORT_FORWARD_9091 = ["kubectl", "port-forward", "svc/target-app", "9091:8080"]


def run(cmd, **kwargs):
    return subprocess.run(cmd, capture_output=True, text=True, **kwargs)


def reset_pods():
    print("  Resetting target-app pods...")
    run(["kubectl", "delete", "pod", "-l", TARGET_APP_LABEL, "--wait=true", "--timeout=60s"])
    # wait until both replicas are Ready
    for _ in range(60):
        result = run(["kubectl", "get", "pods", "-l", TARGET_APP_LABEL,
                       "-o", "jsonpath={.items[*].status.containerStatuses[0].ready}"])
        readiness = result.stdout.strip().split()
        if len(readiness) == 2 and all(r == "true" for r in readiness):
            print("  Pods ready.")
            return True
        time.sleep(2)
    print("  WARNING: pods did not become ready within timeout.")
    return False


def clear_chaos_resources():
    for kind in ["networkchaos", "stresschaos", "podchaos"]:
        run(["kubectl", "delete", kind, "--all", "--ignore-not-found=true"])


def start_port_forwards():
    print("  Starting port-forwards (9090, 9091)...")
    p9090 = subprocess.Popen(PORT_FORWARD_9090, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    p9091 = subprocess.Popen(PORT_FORWARD_9091, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    time.sleep(5)  # give tunnels time to establish
    return p9090, p9091


def stop_port_forwards(procs):
    for p in procs:
        p.terminate()
    time.sleep(2)


def run_single_experiment(run_id: int, duration: int, workdir: Path):
    load_out = workdir / f"run_full_{run_id}.csv"
    monitor_out = workdir / f"monitor_full_{run_id}.csv"
    injector_log = workdir / f"injector_full_{run_id}.log"

    print(f"  Launching load_gen, monitor_loop, fault_injector (run {run_id})...")

    with open(injector_log, "w") as inj_log:
        p_load = subprocess.Popen(
            [sys.executable, "load_gen.py", str(duration), str(load_out)],
            cwd=workdir, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        p_monitor = subprocess.Popen(
            [sys.executable, "monitor_loop_extended.py", str(duration), str(monitor_out)],
            cwd=workdir, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        time.sleep(2)
        p_injector = subprocess.Popen(
            [sys.executable, "fault_injector_full_run.py"],
            cwd=workdir, stdout=inj_log, stderr=subprocess.STDOUT)

        p_injector.wait()
        p_monitor.wait()
        p_load.wait()

    print(f"  Run {run_id} complete -> {load_out.name}, {monitor_out.name}, {injector_log.name}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--runs", type=int, default=15)
    parser.add_argument("--duration", type=int, default=950)
    parser.add_argument("--workdir", default=".")
    parser.add_argument("--pause-between-runs", type=int, default=30,
                         help="Seconds to pause between runs, letting the system settle.")
    args = parser.parse_args()

    workdir = Path(args.workdir).resolve()
    print(f"Batch runner: {args.runs} runs x {args.duration}s in {workdir}")

    for i in range(1, args.runs + 1):
        print(f"\n=== Run {i}/{args.runs} ===")
        clear_chaos_resources()
        reset_pods()
        pf_procs = start_port_forwards()
        try:
            run_single_experiment(i, args.duration, workdir)
        finally:
            stop_port_forwards(pf_procs)
        clear_chaos_resources()
        if i < args.runs:
            print(f"  Pausing {args.pause_between_runs}s before next run...")
            time.sleep(args.pause_between_runs)

    print(f"\nBatch complete: {args.runs} runs finished.")


if __name__ == "__main__":
    main()
