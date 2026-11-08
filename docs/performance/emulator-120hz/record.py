"""Capture FrameTimeline on the dedicated Week3 AVD. Uses Python 3 and adb."""
import argparse
import gzip
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import time
import unicodedata
import xml.etree.ElementTree as ET


HERE = Path(__file__).resolve().parent
APP = "com.example.week3assignment"
REMOTE = "/data/local/tmp/week3-fps"


def normal(text):
    return "".join(c for c in unicodedata.normalize("NFKD", text.lower().replace("ı", "i"))
                   if not unicodedata.combining(c))


def point(node):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node["bounds"]))
    return [str((x1 + x2) // 2), str((y1 + y2) // 2)]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--config", type=Path, default=HERE / "frametimeline.pbtxt")
    parser.add_argument("--continuous-drag", action="store_true",
                        help="Use the separate Week3 touch-driver APK for a single uninterrupted scroll.")
    parser.add_argument("--seconds", type=int, choices=range(1, 61), default=30, metavar="1..60")
    parser.add_argument("--rounds", type=int, default=3)
    parser.add_argument("--resume", action="store_true")
    parser.add_argument("--scenarios", nargs="+", choices=["scroll", "search", "toggle"],
                        default=["scroll", "search", "toggle"])
    args = parser.parse_args()
    if args.continuous_drag and args.scenarios != ["scroll"]:
        parser.error("--continuous-drag requires --scenarios scroll")
    args.out.mkdir(parents=True, exist_ok=True)
    manifest = args.out / "captures.json"
    if manifest.exists() and not args.resume:
        raise SystemExit("Choose a new output directory or use --resume.")

    def adb(*command, timeout=120, stdin=None):
        result = subprocess.run(["adb", "-s", args.serial, *command], capture_output=True,
                                encoding="utf-8", errors="replace", input=stdin, timeout=timeout)
        if result.returncode:
            raise RuntimeError(f"{command}: {result.stdout} {result.stderr}")
        return result.stdout.strip()

    def shell(*command):
        return adb("shell", *command)

    def layout():
        shell("uiautomator", "dump", REMOTE + "/layout.xml")
        root = ET.fromstring(adb("exec-out", "cat", REMOTE + "/layout.xml"))
        nodes = []
        for n in root.iter("node"):
            if n.get("package") != APP:
                continue
            data = dict(n.attrib)
            if n.get("checked") == "true":
                data["checked_label"] = next((s.get("text") for s in n.iter() if s.get("text")), "")
            nodes.append(data)
        if not nodes:
            raise RuntimeError("The benchmark activity is not visible.")
        return nodes

    def require_text(nodes, text):
        if not any(normal(n["text"]) == normal(text) for n in nodes):
            raise RuntimeError(f"Expected UI text {text!r}. Found {[n['text'] for n in nodes if n['text']]}")

    if shell("getprop", "ro.boot.qemu.avd_name") != "Week3_Pixel9_120Hz":
        raise SystemExit("Select the dedicated Week3_Pixel9_120Hz emulator.")
    display = shell("dumpsys", "display")
    mode = next(line.strip() for line in display.splitlines() if "mActiveSfDisplayMode=" in line)
    if not re.search(r"vsyncRate=120[.,}]", mode) or "width=1080, height=2424" not in mode:
        raise SystemExit("Expected the active 1080 x 2424, 120 Hz mode: " + mode)
    render = re.search(r'DisplayDeviceInfo\{"Built-in Screen"[^\n]*?renderFrameRate ([\d.]+)', display)
    period = int(shell("dumpsys", "SurfaceFlinger", "--latency").splitlines()[0])
    if not render or abs(float(render[1]) - 120) > 0.1 or abs(period - 8_333_333) > 1000:
        raise SystemExit("Set min_refresh_rate and peak_refresh_rate to 120.0 on this test AVD.")
    sf_dump = shell("dumpsys", "SurfaceFlinger")
    gpu = next(line.strip() for line in sf_dump.splitlines() if "GLES:" in line)
    record = {"active_display": mode, "render_hz": float(render[1]), "sf_period_ns": period,
              "gpu": gpu, "sdk": shell("getprop", "ro.build.version.sdk"),
              "scenarios": [], "requested_seconds": args.seconds}
    if args.config.resolve() != (HERE / "frametimeline.pbtxt").resolve():
        record["custom_config_sha256"] = hashlib.sha256(args.config.read_bytes()).hexdigest()
    if args.continuous_drag:
        record["scroll_input"] = "continuous drag, 600 px per 1.1 s leg, up to 240 input samples/s"
    if manifest.exists():
        previous = json.loads(manifest.read_text(encoding="utf-8"))
        if {**previous, "scenarios": []} != record:
            raise SystemExit("Capture conditions changed. Choose a new output directory.")
        record = previous
    completed = {(r["scenario"], r["trial"]) for r in record["scenarios"]}
    shell("mkdir", "-p", REMOTE)
    with tempfile.TemporaryDirectory() as temp:
        temp = Path(temp)
        local = temp / "workload.sh"
        local.write_bytes((HERE / "workload.sh").read_bytes().replace(b"\r\n", b"\n"))
        adb("push", str(local), REMOTE + "/workload.sh")
        config = args.config.read_text(encoding="utf-8")
        for repeat in range(1, args.rounds + 1):
            for scenario in args.scenarios:
                if (scenario, repeat) in completed:
                    continue
                name = f"{scenario}-{repeat}"
                out_name = name + ".perfetto-trace.gz"
                if (args.out / out_name).exists():
                    raise SystemExit(f"Preserve or move the unindexed trace {out_name} before retrying.")
                shell("am", "force-stop", APP)
                shell("am", "start", "-W", "-n", APP + "/.MainActivity")
                nodes = layout()
                require_text(nodes, "106,0 W")
                lamp = point(next(n for n in nodes if normal(n["content-desc"]).startswith("kose lambasi")))
                edit = point(next(n for n in nodes if n["class"] == "android.widget.EditText"))
                if scenario == "search":
                    shell("input", "tap", *edit)
                    nodes = layout()
                    edit = point(next(n for n in nodes if n["class"] == "android.widget.EditText"))
                chips = [p for label in ("tumu", "bahce", "salon")
                         for p in point(next(n for n in nodes if normal(n["text"]) == label))]
                coordinates = edit + chips + lamp
                if scenario == "scroll":
                    # Move into the middle of the list before alternating short swipes.
                    shell("input", "swipe", "540", "2000", "540", "1050", "850")
                shell("sh", REMOTE + "/workload.sh", scenario, "4", *coordinates)
                if scenario == "toggle":
                    require_text(layout(), "91,0 W")
                    shell("input", "tap", *lamp)
                    require_text(layout(), "106,0 W")
                elif scenario == "search":
                    require_text(layout(), "Cihaz bulunamadi")

                trace = "/data/misc/perfetto-traces/week3-fps.pftrace"
                started = adb("shell", "-T", "perfetto", "--background-wait", "--txt", "-c", "-",
                              "-o", trace, stdin=config)
                pid = re.search(r"(?m)^\s*(\d+)\s*$", started)
                if not pid:
                    raise RuntimeError("Perfetto did not report a session PID: " + started)
                try:
                    input_info = {}
                    if args.continuous_drag:
                        response = shell("am", "instrument", "-w", "-r", "-e", "seconds", str(args.seconds),
                                         "org.week3.measure/.ContinuousDrag")
                        if "INSTRUMENTATION_CODE: -1" not in response:
                            raise RuntimeError(response)
                        fields = dict(re.findall(r"INSTRUMENTATION_RESULT: (\w+)=(\d+)", response))
                        begin, end = int(fields["begin_ns"]), int(fields["end_ns"])
                        markers = {"BEGIN": begin, "END": end, "swipe 0 begin": begin, "swipe 0 end": end}
                        count = 1
                        input_info["injected_moves"] = int(fields["moves"])
                    else:
                        output = shell("sh", REMOTE + "/workload.sh", scenario, str(args.seconds),
                                       *coordinates).splitlines()
                        count = int(output[-1])
                        markers = {}
                        for line in output[:-1]:
                            stamp, label = line.split(" ", 1)
                            markers[label] = int(stamp) * 10_000_000
                finally:
                    shell("kill", "-TERM", pid.group(1))
                for _ in range(50):
                    if not shell("ls", "-d", "/proc/" + pid.group(1), "2>/dev/null", "||", "true"):
                        break
                    time.sleep(0.1)
                else:
                    raise RuntimeError("The recording did not finish flushing.")
                raw = temp / "current.pftrace"
                adb("pull", trace, str(raw))
                with raw.open("rb") as src, gzip.open(args.out / out_name, "wb") as dst:
                    shutil.copyfileobj(src, dst)
                nodes = layout()
                final_state = {"activity_visible": True}
                valid = True
                if scenario == "toggle":
                    valid = any(n["text"] == "91,0 W" for n in nodes)
                    final_state["expected_watts_seen"] = valid
                if scenario == "search":
                    values = {normal(n["text"]) for n in nodes}
                    query = next((n["text"] for n in nodes if n["class"] == "android.widget.EditText"), "")
                    salon = any(n.get("checked_label") == "Salon" for n in nodes)
                    final_state.update(query=query, salon_selected=salon, empty="cihaz bulunamadi" in values,
                                       expected_watts_seen="106,0 w" in values)
                    valid = query == "kamera" and salon and final_state["empty"] and final_state["expected_watts_seen"]
                gfx = shell("dumpsys", "gfxinfo", APP, "framestats").splitlines()
                header = next(line for line in gfx if line.startswith("Flags,")).split(",")
                last_frame = next(line.split(",") for line in reversed(gfx) if line.startswith("0,"))
                app_period = int(last_frame[header.index("FrameInterval")])
                if abs(app_period - period) > 1000:
                    raise RuntimeError(f"Unexpected app FrameInterval {app_period}")
                record["scenarios"].append({"scenario": scenario, "trial": repeat, **input_info,
                                            "final_state": final_state, "functional_ok": valid,
                                            "guest_boot_markers_ns": markers, "app_frame_interval_ns": app_period,
                                            "actions_or_cycles": count, "trace": out_name})
                manifest.write_text(json.dumps(record, indent=2, ensure_ascii=False), encoding="utf-8")
                print(f"{name}: {count} actions/cycles, {raw.stat().st_size} trace bytes, UI={valid} {ascii(final_state)}", flush=True)


if __name__ == "__main__":
    main()
