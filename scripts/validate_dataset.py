#!/usr/bin/env python3
"""Validate the dependency-free schema-v2 dataset contract."""

from __future__ import annotations

import datetime as dt
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OPERATIONS = {
    "add",
    "modify",
    "remove",
    "merge",
    "split",
    "rename",
    "relocate",
    "interact",
}
MANIFEST_REQUIRED = {
    "schema_version",
    "name",
    "title",
    "description",
    "repository",
    "license",
    "created",
    "authors",
    "provenance",
    "benchmark",
    "contents",
}
GROUND_TRUTH_FILES = {
    ".feature-model",
    ".feature-to-file",
    ".feature-to-folder",
}


def fail(message: str) -> None:
    print(f"ERROR: {message}", file=sys.stderr)
    raise SystemExit(1)


def load_object(path: Path, label: str) -> dict:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        fail(f"cannot read {label} at {path}: {exc}")
    if not isinstance(value, dict):
        fail(f"{label} must be a JSON object")
    return value


def nonempty(value: object) -> bool:
    return isinstance(value, str) and bool(value.strip())


def keys_exact(value: dict, required: set[str], optional: set[str], label: str) -> None:
    missing = sorted(required - value.keys())
    unknown = sorted(value.keys() - required - optional)
    if missing:
        fail(f"{label} is missing: {', '.join(missing)}")
    if unknown:
        fail(f"{label} has unsupported fields: {', '.join(unknown)}")


def contained_path(base: Path, relative: object, label: str) -> Path:
    if not nonempty(relative):
        fail(f"{label} must be a non-empty relative path")
    path = Path(str(relative))
    if path.is_absolute():
        fail(f"{label} must be relative")
    candidate = (base / path).resolve()
    base_resolved = base.resolve()
    if candidate != base_resolved and base_resolved not in candidate.parents:
        fail(f"{label} escapes {base}")
    if not candidate.exists():
        fail(f"{label} does not exist: {relative}")
    return candidate


def version_number(value: object, label: str) -> int:
    match = re.fullmatch(r"v([0-9]{3,})", str(value))
    if not match:
        fail(f"{label} must match vNNN")
    return int(match.group(1))


def validate_manifest() -> tuple[dict, Path]:
    manifest = load_object(ROOT / "dataset.json", "dataset manifest")
    keys_exact(manifest, MANIFEST_REQUIRED, {"$schema"}, "dataset.json")
    if manifest["schema_version"] != "2.0":
        fail("schema_version must be '2.0'")
    if not re.fullmatch(r"[A-Za-z0-9._-]+", str(manifest["name"])):
        fail("name may contain only letters, digits, '.', '_' and '-'")
    for key in ("title", "description", "license"):
        if not nonempty(manifest[key]):
            fail(f"{key} must be a non-empty string")
    if not re.fullmatch(r"https://github\.com/[^/]+/[^/]+/?", str(manifest["repository"])):
        fail("repository must be a GitHub repository HTTPS URL")
    try:
        dt.date.fromisoformat(str(manifest["created"]))
    except ValueError:
        fail("created must be an ISO date (YYYY-MM-DD)")

    authors = manifest["authors"]
    if not isinstance(authors, list) or not authors:
        fail("authors must be a non-empty array")
    for index, author in enumerate(authors):
        if not isinstance(author, dict):
            fail(f"authors[{index}] must be an object")
        keys_exact(author, {"name"}, {"orcid"}, f"authors[{index}]")
        if not nonempty(author["name"]):
            fail(f"authors[{index}].name must be non-empty")

    provenance = manifest["provenance"]
    if not isinstance(provenance, dict):
        fail("provenance must be an object")
    keys_exact(provenance, {"collection_method", "source"}, set(), "provenance")
    if any(not nonempty(provenance[key]) for key in provenance):
        fail("provenance values must be non-empty")

    benchmark = manifest["benchmark"]
    if not isinstance(benchmark, dict):
        fail("benchmark must be an object")
    keys_exact(
        benchmark,
        {"execution_model", "initial_version", "ground_truth_policy"},
        set(),
        "benchmark",
    )
    if benchmark["execution_model"] != "sequential":
        fail("benchmark.execution_model must be 'sequential'")
    version_number(benchmark["initial_version"], "benchmark.initial_version")
    if not nonempty(benchmark["ground_truth_policy"]):
        fail("benchmark.ground_truth_policy must be non-empty")

    contents = manifest["contents"]
    if not isinstance(contents, dict):
        fail("contents must be an object")
    keys_exact(contents, {"subject", "steps", "documentation"}, set(), "contents")
    subject = contained_path(ROOT, contents["subject"], "contents.subject")
    if not subject.is_dir():
        fail("contents.subject must be a directory")
    steps = contained_path(ROOT, contents["steps"], "contents.steps")
    if not steps.is_file():
        fail("contents.steps must be a file")
    documentation = contained_path(ROOT, contents["documentation"], "contents.documentation")
    if not documentation.is_dir():
        fail("contents.documentation must be a directory")
    return manifest, steps


def validate_change(change: object, label: str) -> str:
    if not isinstance(change, dict):
        fail(f"{label} must be an object")
    keys_exact(
        change,
        {"id", "operation", "features_before", "features_after", "description"},
        set(),
        label,
    )
    if not nonempty(change["id"]) or not nonempty(change["description"]):
        fail(f"{label}.id and description must be non-empty")
    operation = change["operation"]
    if operation not in OPERATIONS:
        fail(f"{label}.operation is unsupported: {operation!r}")
    before = change["features_before"]
    after = change["features_after"]
    for field, values in (("features_before", before), ("features_after", after)):
        if not isinstance(values, list) or any(not nonempty(item) for item in values):
            fail(f"{label}.{field} must be an array of non-empty feature names")
        if len(values) != len(set(values)):
            fail(f"{label}.{field} contains duplicate feature names")

    valid_arity = {
        "add": len(before) == 0 and len(after) >= 1,
        "remove": len(before) >= 1 and len(after) == 0,
        "merge": len(before) >= 2 and len(after) >= 1,
        "split": len(before) >= 1 and len(after) >= 2,
        "rename": len(before) == 1 and len(after) == 1,
        "relocate": len(before) == 1 and len(after) == 1,
        "modify": len(before) >= 1 and len(after) >= 1,
        "interact": len(set(before + after)) >= 2,
    }[operation]
    if not valid_arity:
        fail(f"{label} has invalid before/after feature counts for {operation!r}")
    return str(operation)


    keys_exact(validation, {"status", "reviewers", "notes"}, set(), f"{label}.validation")
    if validation["status"] not in {"draft", "reviewed", "adjudicated"}:
        fail(f"{label}.validation.status is unsupported")
    if not isinstance(validation["notes"], str):
        fail(f"{label}.validation.notes must be a string")
    reviewers = validation["reviewers"]
    if not isinstance(reviewers, list):
        fail(f"{label}.validation.reviewers must be an array")
    for index, reviewer in enumerate(reviewers):
        reviewer_label = f"{label}.validation.reviewers[{index}]"
        if not isinstance(reviewer, dict):
            fail(f"{reviewer_label} must be an object")
        keys_exact(reviewer, {"name"}, {"date"}, reviewer_label)
        if not nonempty(reviewer["name"]):
            fail(f"{reviewer_label}.name must be non-empty")
        if "date" in reviewer:
            try:
                dt.date.fromisoformat(str(reviewer["date"]))
            except ValueError:
                fail(f"{reviewer_label}.date must be an ISO date")
    if validation["status"] != "draft" and not reviewers:
        fail(f"{label}: reviewed/adjudicated ground truth requires at least one reviewer")


def validate_step(path: Path, index_id: str, expected_sequence: int) -> tuple[str, str]:
    label = f"step {index_id}"
    step = load_object(path, label)
    required = {
        "id",
        "sequence",
        "before_version",
        "after_version",
        "prompts",
        "scenario_types",
        "expected_changes",
        "ground_truth",
    }
    keys_exact(step, required, {"$schema"}, label)
    if step["id"] != index_id:
        fail(f"{label}: id does not match steps.json")
    if step["sequence"] != expected_sequence:
        fail(f"{label}: sequence must be {expected_sequence}")
    before_number = version_number(step["before_version"], f"{label}.before_version")
    after_number = version_number(step["after_version"], f"{label}.after_version")
    if after_number != before_number + 1:
        fail(f"{label}: after_version must immediately follow before_version")

    prompts = step["prompts"]
    if not isinstance(prompts, list) or not prompts:
        fail(f"{label}.prompts must be a non-empty array")
    for prompt_index, prompt in enumerate(prompts, 1):
        prompt_label = f"{label}.prompts[{prompt_index - 1}]"
        if not isinstance(prompt, dict):
            fail(f"{prompt_label} must be an object")
        keys_exact(prompt, {"order", "path"}, set(), prompt_label)
        if prompt["order"] != prompt_index:
            fail(f"{prompt_label}.order must be {prompt_index}")
        prompt_path = contained_path(path.parent, prompt["path"], f"{prompt_label}.path")
        if not prompt_path.is_file() or not prompt_path.read_text(encoding="utf-8").strip():
            fail(f"{prompt_label}.path must be a non-empty file")
        response_path = prompt_path.parent / "agent_response.md"
        if not response_path.is_file() or not response_path.read_text(encoding="utf-8").strip():
            fail(f"{prompt_label}: missing non-empty agent_response.md beside prompt")

    scenario_types = step["scenario_types"]
    if (
        not isinstance(scenario_types, list)
        or not scenario_types
        or any(item not in OPERATIONS for item in scenario_types)
        or len(scenario_types) != len(set(scenario_types))
    ):
        fail(f"{label}.scenario_types must contain unique supported operations")

    changes = step["expected_changes"]
    if not isinstance(changes, list) or not changes:
        fail(f"{label}.expected_changes must be a non-empty array")
    change_ids: set[str] = set()
    change_operations: set[str] = set()
    for index, change in enumerate(changes):
        change_label = f"{label}.expected_changes[{index}]"
        operation = validate_change(change, change_label)
        if change["id"] in change_ids:
            fail(f"{label}: duplicate change id {change['id']!r}")
        change_ids.add(change["id"])
        change_operations.add(operation)
    if not change_operations.issubset(set(scenario_types)):
        fail(f"{label}: scenario_types must include every expected-change operation")

    truth_path = contained_path(path.parent, step["ground_truth"], f"{label}.ground_truth")
    if not truth_path.is_dir():
        fail(f"{label}.ground_truth must be a directory")
    missing = sorted(name for name in GROUND_TRUTH_FILES if not (truth_path / name).is_file())
    if missing:
        fail(f"{label}.ground_truth is missing: {', '.join(missing)}")

    return str(step["before_version"]), str(step["after_version"])


def validate_steps(manifest: dict, steps_path: Path) -> int:
    index = load_object(steps_path, "benchmark steps index")
    keys_exact(index, {"steps"}, {"$schema"}, "benchmark/steps.json")
    entries = index["steps"]
    if not isinstance(entries, list) or not entries:
        fail("benchmark/steps.json must contain at least one step")

    ids: set[str] = set()
    expected_before = str(manifest["benchmark"]["initial_version"])
    for sequence, entry in enumerate(entries, 1):
        label = f"benchmark/steps.json.steps[{sequence - 1}]"
        if not isinstance(entry, dict):
            fail(f"{label} must be an object")
        keys_exact(entry, {"id", "path"}, set(), label)
        step_id = entry["id"]
        if not isinstance(step_id, str) or not re.fullmatch(r"step-[0-9]{3,}", step_id):
            fail(f"{label}.id must match step-NNN")
        if step_id in ids:
            fail(f"duplicate step id: {step_id}")
        ids.add(step_id)
        step_path = contained_path(steps_path.parent, entry["path"], f"{label}.path")
        if not step_path.is_file():
            fail(f"{label}.path must be a file")
        before, after = validate_step(step_path, step_id, sequence)
        if before != expected_before:
            fail(f"{step_id}: before_version must be {expected_before}")
        expected_before = after
    return len(entries)


def main() -> None:
    manifest, steps_path = validate_manifest()
    count = validate_steps(manifest, steps_path)
    print(f"Dataset contract valid: {manifest['name']} ({count} evolution step(s))")


if __name__ == "__main__":
    main()
