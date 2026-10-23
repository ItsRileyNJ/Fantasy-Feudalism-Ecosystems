#!/usr/bin/env python3
"""Compare current compiled trajectory code with a Git revision, without modifying the checkout."""
import argparse
import os
from pathlib import Path
import shutil
import subprocess
import tempfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--baseline", default="HEAD", help="Git revision containing the reference solver")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    manifest = root / "build/moddev/gameTestServerLegacyClasspath.txt"
    if not manifest.is_file():
        parser.error("Run the development build and prepareGameTestServerRun first")
    java_home = Path(os.environ.get("JAVA_HOME", "/workspace/toolchains/jdk"))
    java = str(java_home / "bin/java") if (java_home / "bin/java").is_file() else shutil.which("java")
    javac = str(java_home / "bin/javac") if (java_home / "bin/javac").is_file() else shutil.which("javac")
    if not java or not javac:
        parser.error("A full Java 21 JDK is required")
    classpath = os.pathsep.join([str(root / "build/classes/java/main"),
                                 str(root / "build/moddev/artifacts/neoforge-21.1.262.jar"),
                                 *manifest.read_text().splitlines()])
    source_path = "src/main/java/net/itsrileynj/fantasyfeudalismmobs/SkeletonBallistics.java"
    source = subprocess.run(["git", "show", f"{args.baseline}:{source_path}"], cwd=root,
                            check=True, capture_output=True, text=True).stdout
    with tempfile.TemporaryDirectory(prefix="ballistics-") as directory:
        work = Path(directory)
        source_file = work / "SkeletonBallistics.java"
        source_file.write_text(source)
        baseline = work / "baseline"
        baseline.mkdir()
        subprocess.run([javac, "-proc:none", "-cp", classpath, "-d", str(baseline), str(source_file)], check=True)
        subprocess.run([javac, "-proc:none", "-cp", classpath, "-d", str(work),
                        str(root / "tools/BallisticsBenchmark.java")], check=True)
        subprocess.run([java, "-cp", str(work) + os.pathsep + classpath, "BallisticsBenchmark",
                        str(baseline / "net/itsrileynj/fantasyfeudalismmobs/SkeletonBallistics.class")], check=True)


if __name__ == "__main__":
    main()
