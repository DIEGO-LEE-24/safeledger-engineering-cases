"""Check this repository's public file boundary and local Markdown links.

Only reads this directory. No network, product access, or file writes.
This is a repository hygiene tool, not product implementation evidence.
"""

import json
import re
from pathlib import Path
from urllib.parse import unquote, urlsplit


ROOT = Path(__file__).resolve().parents[1]
ALLOWED_SUFFIXES = {".md", ".py", ".java", ".mjs"}
ALLOWED_NAMES = {".gitignore"}
PUBLIC_FILES = {
    ".gitignore", "README.md", "cases/a-safeledger-llm.md",
    "cases/c-safeledger-agent.md",
    "docs/publication-scope.md", "evidence/README.md",
    "tools/check_repository.py",
    "examples/llm-gateway/README.md", "examples/llm-gateway/run.py",
    "examples/llm-gateway/src/LlmGateway.java", "examples/llm-gateway/src/Demo.java",
    "examples/llm-gateway/tests/LlmGatewayTest.java",
    "examples/agent-guardrails/README.md", "examples/agent-guardrails/guard.mjs",
    "examples/agent-guardrails/demo.mjs", "examples/agent-guardrails/guard.test.mjs",
    "evidence/test-results.md",
}
BLOCKED_PARTS = {
    "private", "reports", "primary", "raw", "data", "credentials",
    "node_modules", "__pycache__", ".venv",
}
SUSPICIOUS = {
    "private-key": re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"),
    "github-token": re.compile(r"\b(?:gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,})"),
    "api-token": re.compile(r"\bsk-[A-Za-z0-9_-]{20,}"),
    "aws-key": re.compile(r"\b(?:AKIA|ASIA)[A-Z0-9]{16}\b"),
    "credential-assignment": re.compile(
        r"(?im)^\s*(?:api[_-]?key|token|password|secret)\s*[:=]\s*[\"']?[^\s\"']{8,}"
    ),
    "absolute-user-path": re.compile(r"(?i)(?:[A-Z]:[/\\]Users[/\\]|/(?:home|Users)/)"),
    "ipv4-address": re.compile(r"\b(?:\d{1,3}\.){3}\d{1,3}\b"),
    "unnecessary-generation-label": re.compile(r"(?i)AI[- ]generated|Generated\s+by\s+AI"),
}


def main():
    failures = []
    checked_files = 0
    checked_links = 0
    mermaid_blocks = 0
    failures = []
    checked_files = 0
    checked_links = 0
    mermaid_blocks = 0
    for path in sorted(ROOT.rglob("*")):
        relative = path.relative_to(ROOT)
        if relative.parts[0] == ".git":
            continue
        if path.is_symlink() or getattr(path, "is_junction", lambda: False)():
            failures.append({"file": relative.as_posix(), "rule": "symlink"})
            continue
        if any(part in BLOCKED_PARTS or part.startswith(".env") for part in relative.parts):
            failures.append({"file": relative.as_posix(), "rule": "excluded-material"})
            continue
        if path.is_dir():
            if ".git" in relative.parts:
                failures.append({"file": relative.as_posix(), "rule": "nested-git-directory"})
            continue
        if relative.as_posix() not in PUBLIC_FILES:
            failures.append({"file": relative.as_posix(), "rule": "unreviewed-file"})
            continue
        if path.suffix not in ALLOWED_SUFFIXES and path.name not in ALLOWED_NAMES:
            failures.append({"file": relative.as_posix(), "rule": "unapproved-file-type"})
            continue
        checked_files += 1
        try:
            content = path.read_text(encoding="utf-8")
        except (UnicodeError, OSError):
            failures.append({"file": relative.as_posix(), "rule": "unreadable-text"})
            continue
        for name, pattern in SUSPICIOUS.items():
            if pattern.search(content):
                # Do not echo matched content, which could be sensitive.
                failures.append({"file": relative.as_posix(), "rule": name})
        if path.suffix != ".md":
            continue
        fence = None
        prose = []
        for line in content.splitlines():
            stripped = line.lstrip()
            match = re.match(r"^(`{3,}|~{3,})(.*)$", stripped)
            if match:
                marker, language = match.groups()
                if fence is None:
                    fence = marker
                    if language.strip() == "mermaid":
                        mermaid_blocks += 1
                elif marker[0] == fence[0] and len(marker) >= len(fence):
                    fence = None
                continue
            if fence is None:
                prose.append(line)
        if fence is not None:
            failures.append({"file": relative.as_posix(), "rule": "unclosed-code-fence"})
        for target in re.findall(r"!?\[[^\]]*\]\(([^\s)]+)\)", "\n".join(prose)):
            checked_links += 1
            parsed = urlsplit(target.strip("<>"))
            if parsed.scheme or parsed.netloc:
                failures.append({"file": relative.as_posix(), "rule": "external-link-review"})
                continue
            if parsed.fragment:
                failures.append({"file": relative.as_posix(), "rule": "anchor-review"})
                continue
            resolved = (path.parent / unquote(parsed.path)).resolve()
            try:
                resolved.relative_to(ROOT)
            except ValueError:
                failures.append({"file": relative.as_posix(), "rule": "link-outside-repository"})
                continue
            if not parsed.path or not resolved.is_file():
                failures.append({"file": relative.as_posix(), "rule": "missing-link-target"})
    for expected in sorted(PUBLIC_FILES):
        if not (ROOT / expected).is_file():
            failures.append({"file": expected, "rule": "missing-reviewed-file"})
    print(json.dumps({
        "checked_files": checked_files,
        "checked_local_links": checked_links,
        "mermaid_blocks": mermaid_blocks,
        "failures": failures,
        "scope": "static files and local links; not rights or product verification",
    }, ensure_ascii=False, indent=2))
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
