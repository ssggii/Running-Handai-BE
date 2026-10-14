"""코스 생성 부하를 주면서 조회 API 응답 시간과 커넥션 풀 상태를 잰다.

앱은 local,measure,measure-stub 프로필로 떠 있어야 한다 (외부 API는 지연 스텁).

  python3 -I load_test.py <라벨> [--create-rate 2.4] [--duration 60] [--warmup 10]
                              [--read-rate 5] [--read-path /api/courses/1] [--member-id 1] [--points 1156]

동작
  - 코스 생성: 초당 create-rate건씩 일정한 간격으로 보낸다 (리틀의 법칙의 도착률)
  - 조회: 초당 read-rate건씩 일정한 간격으로 보낸다 (생성 부하와 상관없이 들어오는 다른 사용자 요청)
  - 커넥션 풀: 0.2초마다 /metrics/hikaricp.connections.{active,pending}을 읽는다
  - 처음 warmup초는 집계에서 뺀다. 요청은 duration초 동안 보내고, 보낸 요청이 모두 끝날 때까지 기다린다
결과 한 줄을 results.tsv에 쌓는다.
"""
import argparse
import datetime
import json
import os
import statistics
import threading
import time
import urllib.error
import urllib.request

APP = os.environ.get("APP", "http://localhost:8080")
DIR = os.path.dirname(os.path.abspath(__file__))


def request(method, path, timeout):
    start = time.monotonic()
    try:
        req = urllib.request.Request(APP + path, method=method)
        with urllib.request.urlopen(req, timeout=timeout) as res:
            res.read()
            status = res.status
    except urllib.error.HTTPError as e:
        status = e.code
    except Exception:
        status = -1  # 타임아웃, 연결 실패
    return start, (time.monotonic() - start) * 1000, status


def metric(name):
    try:
        with urllib.request.urlopen(f"{APP}/metrics/hikaricp.connections.{name}", timeout=2) as res:
            return json.load(res)["measurements"][0]["value"]
    except Exception:
        return None


def pct(values, p):
    if not values:
        return float("nan")
    values = sorted(values)
    return values[min(len(values) - 1, int(round(p / 100 * (len(values) - 1))))]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("label")
    ap.add_argument("--create-rate", type=float, default=2.4)
    ap.add_argument("--duration", type=float, default=60)
    ap.add_argument("--warmup", type=float, default=10)
    ap.add_argument("--read-rate", type=float, default=5)
    ap.add_argument("--read-path", default="/api/courses/1")
    ap.add_argument("--member-id", type=int, default=1)
    ap.add_argument("--points", type=int, default=1156)
    a = ap.parse_args()

    creates, reads, samples = [], [], []
    lock = threading.Lock()
    threads = []
    t0 = time.monotonic()
    stop = threading.Event()

    def fire(kind):
        if kind == "create":
            r = request("POST", f"/measure/courses?memberId={a.member_id}&points={a.points}", timeout=120)
            target = creates
        else:
            r = request("GET", a.read_path, timeout=60)
            target = reads
        with lock:
            target.append(r)

    def schedule(kind, rate):
        if rate <= 0:
            return
        interval, n = 1 / rate, 0
        while time.monotonic() - t0 < a.duration:
            next_at = t0 + n * interval
            time.sleep(max(0, next_at - time.monotonic()))
            th = threading.Thread(target=fire, args=(kind,))
            th.start()
            threads.append(th)
            n += 1

    def sample():
        while not stop.is_set():
            samples.append((time.monotonic(), metric("active"), metric("pending")))
            time.sleep(0.2)

    sampler = threading.Thread(target=sample)
    sampler.start()
    schedulers = [threading.Thread(target=schedule, args=("create", a.create_rate)),
                  threading.Thread(target=schedule, args=("read", a.read_rate))]
    for s in schedulers:
        s.start()
    for s in schedulers:
        s.join()
    for th in list(threads):
        th.join()
    stop.set()
    sampler.join()

    after = t0 + a.warmup
    c = [r for r in creates if r[0] >= after]
    r = [x for x in reads if x[0] >= after]
    c_ok = [x[1] for x in c if x[2] == 200]
    r_ok = [x[1] for x in r if x[2] == 200]
    s = [x for x in samples if x[0] >= after and x[2] is not None]
    pending = [x[2] for x in s]
    active = [x[1] for x in s if x[1] is not None]

    row = {
        "time": datetime.datetime.now().strftime("%Y-%m-%d %H:%M"),
        "label": a.label,
        "create_rate": a.create_rate,
        "duration_s": a.duration,
        "creates": len(c),
        "create_fail": len(c) - len(c_ok),
        "create_p50_ms": f"{pct(c_ok, 50):.0f}",
        "create_p95_ms": f"{pct(c_ok, 95):.0f}",
        "reads": len(r),
        "read_fail": len(r) - len(r_ok),
        "read_p50_ms": f"{pct(r_ok, 50):.0f}",
        "read_p95_ms": f"{pct(r_ok, 95):.0f}",
        "read_p99_ms": f"{pct(r_ok, 99):.0f}",
        "read_max_ms": f"{max(r_ok) if r_ok else float('nan'):.0f}",
        "active_max": f"{max(active) if active else float('nan'):.0f}",
        "pending_max": f"{max(pending) if pending else float('nan'):.0f}",
        "pending_mean": f"{statistics.mean(pending) if pending else float('nan'):.1f}",
    }
    path = os.path.join(DIR, "results.tsv")
    new = not os.path.exists(path)
    with open(path, "a") as f:
        if new:
            f.write("\t".join(row) + "\n")
        f.write("\t".join(str(v) for v in row.values()) + "\n")
    for k, v in row.items():
        print(f"{k:>14}: {v}")


if __name__ == "__main__":
    main()
