"""Calculate presentation rates and pacing from the captured Android traces."""
import argparse
from collections import Counter
import csv
import io
import json
import math
from pathlib import Path
import subprocess


HERE = Path(__file__).resolve().parent
PRESENTED = {"Early Present", "On-time Present", "Late Present"}


def query(exe, trace, sql):
    p = subprocess.run([str(exe), "query", "--format", "csv", "--max-rows", "0",
                        "--max-bytes", "0", str(trace)], input=sql, capture_output=True,
                       encoding="utf-8", errors="replace")
    if p.returncode:
        raise RuntimeError(p.stderr)
    return list(csv.DictReader(io.StringIO(p.stdout)))


def percentile(values, percent):
    ordered = sorted(values)
    return round(ordered[math.ceil(len(ordered) * percent / 100) - 1], 3)


def read_run(exe, trace_dir, run):
    trace = trace_dir / run["trace"]
    integrity = query(exe, trace, """
      SELECT (SELECT int_value FROM metadata WHERE name='trace_time_clock_id') AS clock_id,
             (SELECT int_value FROM metadata WHERE name='tracing_started_ns') AS started,
             (SELECT COALESCE(SUM(value), 0) FROM stats WHERE severity='data_loss' AND value>0) AS loss
    """)[0]
    marks = run["guest_boot_markers_ns"]
    begin, end = marks["BEGIN"], marks["END"]
    # Clock 6 is CLOCK_BOOTTIME. The sidecar timestamps have 10 ms resolution.
    if (integrity["clock_id"] != "6" or integrity["loss"] != "0" or
            int(integrity["started"]) > begin + 10_000_000 or end <= begin):
        raise ValueError(f"Invalid trace clock, bounds, or lost data in {trace}: {integrity}")
    rows = query(exe, trace, (HERE / "frames.sql").read_text(encoding="utf-8"))
    apps = [row for row in rows if begin <= int(row["ts"]) < end and int(row["dur"]) >= 0]
    presented = {int(row["display_frame_token"]): int(row["present_ns"]) for row in rows
                 if row["present_type"] in PRESENTED and row["present_ns"] != "[NULL]"}
    times = sorted(time for time in presented.values() if begin <= time < end)
    if not apps or len(times) < 2:
        raise ValueError(f"No measurable MainActivity buffers in {trace}")
    # The discrete-input scenarios include intentional pauses. Their active
    # rate is left blank; evaluate those frames using deadline/jank data.
    segments = []
    if run["scenario"] == "scroll":
        for i in range(run["actions_or_cycles"]):
            lo, hi = marks[f"swipe {i} begin"], marks[f"swipe {i} end"]
            segments.append((hi - lo, [t for t in times if lo <= t < hi]))
    return {
        "trial": run["trial"], "scenario": run["scenario"], "ops": run["actions_or_cycles"],
        "ui_ok": run.get("functional_ok", run["scenario"] == "scroll"),
        "window_ns": end - begin, "presented": len(times), "apps": len(apps),
        "dropped": sum(r["present_type"] == "Dropped Frame" for r in apps),
        "unmatched": sum(r["present_type"] in PRESENTED and r["present_ns"] == "[NULL]" for r in apps),
        "jank": sum(r["jank_type"] not in ("None", "[NULL]", "Unspecified") for r in apps),
        "app_late": sum(r["on_time_finish"] == "0" for r in apps),
        "work_ms": [int(r["dur"]) / 1e6 for r in apps],
        "active_ns": sum(duration for duration, _ in segments),
        "active_frames": sum(len(t) for _, t in segments),
        "gap_ms": [(b - a) / 1e6 for _, t in segments for a, b in zip(t, t[1:])],
        "labels": Counter(r["jank_type"] for r in apps),
    }


def summarize(runs, trial):
    def total(key):
        return sum(r[key] for r in runs)
    gaps = [x for r in runs for x in r["gap_ms"]]
    work = [x for r in runs for x in r["work_ms"]]
    active = total("active_ns")
    return {
        "scenario": runs[0]["scenario"], "trial": trial, "ui_passed": total("ui_ok"),
        "trials": len(runs), "actions_or_cycles": total("ops"),
        "window_s": round(total("window_ns") / 1e9, 3),
        "presented": total("presented"), "presented_per_window_second": round(total("presented") * 1e9 / total("window_ns"), 2),
        "swipe_s": round(active / 1e9, 3) if active else "", "swipe_frames": total("active_frames") if active else "",
        "swipe_fps": round(total("active_frames") * 1e9 / active, 2) if active else "",
        **{f"swipe_gap_p{q}_ms": percentile(gaps, q) if gaps else "" for q in (50, 95, 99)},
        "swipe_gap_max_ms": round(max(gaps), 3) if gaps else "",
        "app_frames": total("apps"), "dropped_app_frames": total("dropped"), "unmatched_frames": total("unmatched"),
        "jank_frames": total("jank"), "jank_pct": round(100 * total("jank") / total("apps"), 2),
        "app_finished_late": total("app_late"), "app_work_p95_ms": percentile(work, 95),
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--processor", type=Path, required=True)
    parser.add_argument("--traces", type=Path, default=HERE / "traces")
    parser.add_argument("--out", type=Path, default=HERE / "results.csv")
    opts = parser.parse_args()
    captures = json.loads((opts.traces / "captures.json").read_text(encoding="utf-8"))
    runs = [read_run(opts.processor, opts.traces, run) for run in captures["scenarios"]]
    summary = [summarize([run], run["trial"]) for run in runs]
    for scenario in dict.fromkeys(run["scenario"] for run in runs):
        group = [run for run in runs if run["scenario"] == scenario]
        combined = summarize(group, "ALL")
        summary.append(combined)
        print(json.dumps(combined, ensure_ascii=True))
        print("FrameTimeline:", dict(sum((r["labels"] for r in group), Counter())))
    with opts.out.open("w", newline="", encoding="utf-8") as out:
        writer = csv.DictWriter(out, fieldnames=summary[0])
        writer.writeheader()
        writer.writerows(summary)


if __name__ == "__main__":
    main()
