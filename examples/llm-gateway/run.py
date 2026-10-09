"""Compile the independent Java example into a disposable temporary directory."""

import argparse
from pathlib import Path
import subprocess
import tempfile


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--demo", action="store_true")
    options = parser.parse_args()
    root = Path(__file__).resolve().parent
    sources = sorted(root.glob("src/*.java")) + sorted(root.glob("tests/*.java"))
    with tempfile.TemporaryDirectory(prefix="gateway-example-") as build:
        # The context owns this newly created directory, not any product path.
        if Path(build).resolve().parent != Path(tempfile.gettempdir()).resolve():
            raise RuntimeError("Unexpected build directory")
        compiled = subprocess.run(["javac", "-encoding", "UTF-8", "-d", build, *map(str, sources)])
        if compiled.returncode:
            return compiled.returncode
        entry = "Demo" if options.demo else "LlmGatewayTest"
        return subprocess.run(["java", "-cp", build, entry]).returncode


if __name__ == "__main__":
    raise SystemExit(main())
