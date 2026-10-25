#!/usr/bin/env python3
"""Run both mods' unmodified GameTests against the supported Mercenaries JAR."""
import argparse
import hashlib
from pathlib import Path
import re
import os
import signal
import subprocess
import sys
import tempfile
import time


def run_suite(command, root, log):
    """Bound both total runtime and a diagnosed pre-start chunk-readiness stall."""
    with log.open("w") as stream, log.open() as reader:
        process = subprocess.Popen(command, cwd=root, stdout=stream, stderr=subprocess.STDOUT, start_new_session=True)
        started = time.monotonic()
        stalled_name = None
        stalled_since = started
        reason = None
        while process.poll() is None:
            time.sleep(1)
            for line in reader.read().splitlines():
                if "Running test batch" in line:
                    stalled_name = None
                match = re.search(r"progress diagnostic: name=([^,]+), started=false,.*testTick=0,", line)
                if match and match[1] != stalled_name:
                    stalled_name = match[1]
                    stalled_since = time.monotonic()
            now = time.monotonic()
            if stalled_name and now - stalled_since > 30:
                reason = f"test never started after repeated chunk-readiness diagnostics: {stalled_name}"
            elif now - started > 900:
                reason = "suite exceeded fifteen minutes"
            if reason:
                os.killpg(process.pid, signal.SIGTERM)
                try:
                    process.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL)
                    process.wait()
                print(f"FAIL: {reason}; pending tests are unrun. See {log}", file=sys.stderr)
                return 1
        return process.returncode


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("jar", type=Path, help="User-supplied Mercenaries 1.15.7 JAR")
    parser.add_argument("--suite", choices=("all", "ecosystems", "mercenaries"), default="all")
    args = parser.parse_args()
    jar = args.jar.resolve(strict=True)
    expected = "32fba122dde2819abdd64a46ee350dc3e6f2533e34c8b7360700e2a8239d0a65"
    if hashlib.sha256(jar.read_bytes()).hexdigest() != expected:
        parser.error("JAR does not match the supported Mercenaries 1.15.7 binary; audit a new version before changing this hash")

    root = Path(__file__).resolve().parents[1]
    helper = Path("/workspace/toolchains/gradle.py")
    gradle = [sys.executable, str(helper)] if helper.is_file() else [str(root / "gradlew"), "--max-workers=2"]
    (root / "run").mkdir(exist_ok=True)
    output = Path(tempfile.mkdtemp(prefix="compatibility-", dir=root / "run"))
    print(f"Mercenaries SHA-256: {expected}\nFresh worlds and logs: {output}", flush=True)
    suites = {"ecosystems": ("fantasy_feudalism_mobs", 203), "mercenaries": ("villagerscontract", 338)}
    for name, (namespace, minimum) in suites.items():
        if args.suite not in ("all", name):
            continue
        log = output / f"{name}.log"
        # A single-use daemon keeps the game JVM inside this invocation's process group,
        # so aborting a stalled run also stops its descendants rather than leaking a server.
        command = gradle + ["--no-daemon", "runGameTestServer", "build", "multiplayerTestJar",
                            f"-PcompatibilityMod={jar}", f"-PgameTestNamespaces={namespace}",
                            f"-PgameTestDirectory={output / name}"]
        print(f"Running {name}; log: {log}", flush=True)
        returncode = run_suite(command, root, log)
        text = log.read_text()
        summaries = re.findall(r"All (\d+) required tests passed", text)
        if returncode or "BUILD SUCCESSFUL" not in text or not summaries or int(summaries[-1]) < minimum:
            print("\n".join(text.splitlines()[-60:]), file=sys.stderr)
            print(f"FAIL: {name}; expected at least {minimum} passing required tests. See {log}", file=sys.stderr)
            return 1
        print(f"PASS: {name}: {summaries[-1]} required tests; build and test-harness JAR succeeded", flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
