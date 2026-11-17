#!/usr/bin/env python3
"""Reproduce the step-010 run evaluation with Python 3.9+ and JDK 21+."""

import argparse
import base64
from collections import defaultdict, deque
import csv
import hashlib
import io
import json
from pathlib import Path, PurePosixPath
import runpy
import subprocess
import tarfile
import tempfile


REFERENCE = "693cc1e8b57f176f037bac0ab54c9b01de50851c"
GENERATED = "7a0222bd130b7f6fbb94b7c102fb976599b31f7e"
BRANCH = "eval-v2-step-010-run-01"
ALIAS = {"DueDates": "TaskDueDates"}
# Reviewed correspondences between data rows in the pinned normalized CSVs.
# These align implementation roles; they do not certify behavior equivalence.
REVIEWED = [
    (58, 51, "Date selection callback: service delegation replaces direct model update/refresh; both save tasks."),
    (62, 52, "Overdue badge creation; variable and display capitalization differ."),
    (63, 53, "Attach overdue badge stylesheet class; selector renamed."),
    (64, 54, "Control overdue badge visibility; variable renamed."),
    (65, 55, "Control overdue badge layout participation; variable renamed."),
    (68, 66, "Compose due-date UI: constructor arguments become addAll. Separate clear-date controls remain unmatched."),
    (76, 78, "Existing task serialization loop retains PersistTags coverage; date serialization representation differs."),
    (86, 89, "Parse optional persisted date; field order and null representation differ. Runtime behavior not verified."),
    (121, 133, "Overdue badge font-weight property; selector renamed."),
    (122, 132, "Overdue badge text-color property; selector and visual value differ."),
    (123, 131, "Overdue badge background-color property; selector and visual value differ."),
    (124, 134, "Overdue badge padding property; selector and visual value differ."),
    (125, 135, "Overdue badge corner-radius property; selector and visual value differ."),
]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    repo = args.repo.resolve()
    output = (args.output or repo / "evaluation").resolve()

    def git(*arguments):
        return subprocess.check_output(["git", "-c", "safe.directory=" + repo.as_posix(),
                                        "-C", str(repo), *arguments])

    def read(ref, path):
        return git("show", ref + ":" + path)

    def rows(content):
        return list(csv.reader(io.StringIO(content.decode("utf-8"))))[1:]

    def save(path, fields, records):
        target = output / path
        target.parent.mkdir(parents=True, exist_ok=True)
        with target.open("w", encoding="utf-8", newline="") as stream:
            writer = csv.writer(stream, lineterminator="\n")
            writer.writerow(fields)
            writer.writerows(records)

    reference_bytes = {name: read(REFERENCE, "evaluation/ground_truth/step-010/" + name)
                       for name in ("file_mappings.csv", "fragments.csv")}
    reference_fragments = [tuple(row) for row in rows(reference_bytes["fragments.csv"])]
    reference_mappings = {tuple(row) for row in rows(reference_bytes["file_mappings.csv"])}
    reference_model = read(REFERENCE, "benchmark/steps/step-010/ground-truth/.feature-model").decode()
    reference_source = git("rev-parse", "v010^{commit}").decode().strip()
    generated_commit = git("rev-parse", GENERATED + "^{commit}").decode().strip()
    archive = git("archive", generated_commit, "project")
    with tarfile.open(fileobj=io.BytesIO(archive)) as bundle:
        files = {member.name.removeprefix("project/"):
                 bundle.extractfile(member).read().decode("utf-8").replace("\r\n", "\n")
                 for member in bundle.getmembers() if member.isfile()
                 and (member.name.startswith("project/src/") or member.name == "project/.feature-model")}
    source = {path: content for path, content in files.items() if path.endswith((".java", ".css"))}
    generated_model = files[".feature-model"]
    generated_features = set(generated_model.split())
    generated_mappings, generated_folders = set(), set()
    for path, content in files.items():
        name = PurePosixPath(path).name
        if name not in (".feature-to-file", ".feature-to-folder"):
            continue
        lines = [line.strip() for line in content.splitlines() if line.strip()]
        if len(lines) % 2:
            raise ValueError("Incomplete mapping: " + path)
        for target, labels in zip(lines[::2], lines[1::2]):
            target = str(PurePosixPath(path).parent / target)
            for feature in map(str.strip, labels.split(",")):
                if feature not in generated_features:
                    raise ValueError("Unknown mapped feature: " + feature)
                if name == ".feature-to-file":
                    if target not in source:
                        raise ValueError("Missing mapped source: " + target)
                    generated_mappings.add((target, feature))
                else:
                    generated_folders.add((target, feature))
                    generated_mappings.update((p, feature) for p in source if p.startswith(target + "/"))
    notes = []
    with tempfile.TemporaryDirectory(prefix="evaluate-step010-") as temp:
        temp = Path(temp)
        normalizer = temp / "normalize_ground_truth.py"
        normalizer.write_bytes(read(REFERENCE, "scripts/normalize_ground_truth.py"))
        module = runpy.run_path(str(normalizer))
        helper = temp / "ExtractStatements.java"
        helper.write_text(module["JAVA_PARSER"], encoding="utf-8")
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(temp), str(helper)], check=True)
        generated_fragments = module["java_fragments"](source, generated_features, temp, notes)
        generated_fragments.extend(module["css_fragments"](source, generated_features, notes))
    if len(reference_fragments) != 125 or len(generated_fragments) != 138:
        raise ValueError("Reviewed alignment applies only to the pinned 125/138-row inputs")

    for name, content in reference_bytes.items():
        target = output / "ground_truth" / "step-010" / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(content)
    save("generated/step-010/file_mappings.csv", ["file", "feature"], sorted(generated_mappings))
    save("generated/step-010/fragments.csv", ["file", "context", "statement", "feature"], generated_fragments)
    if generated_folders:
        save("generated/step-010/folder_mappings.csv", ["folder", "feature"], sorted(generated_folders))

    comparisons, metrics = [], []

    def metric(name, tp, fp, fn, status="diagnostic", note=""):
        precision = tp / (tp + fp) if tp is not None and tp + fp else None
        recall = tp / (tp + fn) if tp is not None and tp + fn else None
        f1 = 2 * tp / (2 * tp + fp + fn) if tp is not None and 2 * tp + fp + fn else None
        metrics.append((name, tp, fp, fn, precision, recall, f1, status, note))

    def compare_sets(name, expected, actual, status="evaluated", note=""):
        metric(name, len(expected & actual), len(actual - expected), len(expected - actual), status, note)
        for classification, records in (("TP", expected & actual), ("FP", actual - expected), ("FN", expected - actual)):
            for record in sorted(records):
                comparisons.append((name, classification, "", "", json.dumps(record), "", "Set comparison"))

    def model_pairs(model, align=False):
        present, hierarchy, stack = set(), set(), []
        for line in model.splitlines():
            if not line.strip():
                continue
            depth = (len(line) - len(line.lstrip())) // 4
            feature = line.strip()
            if align:
                feature = ALIAS.get(feature, feature)
            if depth > len(stack):
                raise ValueError("Invalid feature hierarchy indentation")
            stack = stack[:depth]
            hierarchy.update((ancestor, feature) for ancestor in stack)
            stack.append(feature)
            present.add(feature)
        return present, hierarchy

    for aligned in (False, True):
        expected_features, expected_hierarchy = model_pairs(reference_model, aligned)
        actual_features, actual_hierarchy = model_pairs(generated_model, aligned)
        suffix = "aligned" if aligned else "raw_names"
        compare_sets("features_" + suffix, expected_features, actual_features)
        compare_sets("hierarchy_" + suffix, expected_hierarchy, actual_hierarchy)
        normalize_feature = lambda feature: ALIAS.get(feature, feature) if aligned else feature
        expected_files = reference_mappings | {(row[0], row[3]) for row in reference_fragments}
        actual_files = generated_mappings | {(row[0], row[3]) for row in generated_fragments}
        compare_sets("file_presence_" + suffix,
                     {(path, normalize_feature(feature)) for path, feature in expected_files},
                     {(path, normalize_feature(feature)) for path, feature in actual_files},
                     "partial_reference", "Reference explicit file mappings are empty; FP means unsupported by supplied reference, not proven wrong.")
    compare_sets("fragment_file_presence_aligned",
                 {(row[0], ALIAS.get(row[3], row[3])) for row in reference_fragments},
                 {(row[0], ALIAS.get(row[3], row[3])) for row in generated_fragments},
                 "partial_reference", "Projects only inline fragments to files; reference may omit legitimate relationships.")
    metric("explicit_file_mappings", None, None, None, "not_evaluable",
           f"Reference has 0 explicit mappings, generated has {len(generated_mappings)}; empty oracle is unresolved, not treated as proof every generated mapping is wrong.")
    metric("folder_mappings", None, None, None, "not_applicable", "Neither side supplies folder mappings.")

    def fragment_comparison(name, aligned, semantic=False, due_only=False):
        def key(row):
            return (*row[:3], ALIAS.get(row[3], row[3]) if aligned else row[3])
        expected = {i: row for i, row in enumerate(reference_fragments, 1)
                    if not due_only or row[3] == "TaskDueDates"}
        actual = {i: row for i, row in enumerate(generated_fragments, 1)
                  if not due_only or ALIAS.get(row[3], row[3]) == "TaskDueDates"}
        lookup = defaultdict(deque)
        for index, row in actual.items():
            lookup[key(row)].append(index)
        matched = []
        for reference_id, row in expected.items():
            if lookup[key(row)]:
                matched.append((reference_id, lookup[key(row)].popleft(), "Exact normalized statement/context match"))
        used_reference = {r for r, _, _ in matched}
        used_generated = {g for _, g, _ in matched}
        if semantic:
            for reference_id, generated_id, rationale in REVIEWED:
                if due_only and reference_id not in expected:
                    continue
                if reference_id in used_reference or generated_id in used_generated:
                    raise ValueError("Reviewed correspondence conflicts with exact match")
                if key(expected[reference_id])[-1] != key(actual[generated_id])[-1]:
                    raise ValueError("Reviewed correspondence has inconsistent feature identity")
                matched.append((reference_id, generated_id, "Reviewed role alignment: " + rationale))
                used_reference.add(reference_id)
                used_generated.add(generated_id)
        for reference_id, generated_id, rationale in matched:
            comparisons.append((name, "TP", f"R{reference_id:03}", f"G{generated_id:03}",
                                json.dumps(expected[reference_id]), json.dumps(actual[generated_id]), rationale))
        for reference_id in expected.keys() - used_reference:
            comparisons.append((name, "FN", f"R{reference_id:03}", "", json.dumps(expected[reference_id]), "",
                                "No aligned generated counterpart; may reflect implementation difference, not missing annotation."))
        for generated_id in actual.keys() - used_generated:
            comparisons.append((name, "FP", "", f"G{generated_id:03}", "", json.dumps(actual[generated_id]),
                                "No aligned reference counterpart; may be a legitimate additional implementation/annotation."))
        metric(name, len(matched), len(actual) - len(matched), len(expected) - len(matched),
               "reviewed_role_alignment" if semantic else "lexical_diagnostic",
               "Closed-reference overlap; unaligned implementation changes are counted FP/FN. Not pure annotation accuracy on a shared implementation.")

    fragment_comparison("fragments_raw_names", False)
    fragment_comparison("fragments_feature_aligned_exact", True)
    fragment_comparison("fragments_reviewed_role_alignment", True, semantic=True)
    fragment_comparison("due_dates_fragments_reviewed_role_alignment", True, semantic=True, due_only=True)
    save("results/step-010/metrics.csv", ["metric", "tp", "fp", "fn", "precision", "recall", "f1", "status", "notes"], metrics)
    save("results/step-010/comparison.csv", ["metric", "classification", "reference_id", "generated_id", "reference_record", "generated_record", "reason"], comparisons)

    report = [
        "# Step-010 evaluation: eval-v2-step-010-run-01", "",
        f"Generated branch: {BRANCH}; evaluated source commit: {generated_commit}.",
        f"Reference CSVs and normalizer: {REFERENCE}; reference source v010: {reference_source}.", "",
        "Generated implementation was normalized using the same Java/CSS extractor as ground truth. "
        "There are 10 explicit generated file mappings and 138 generated fragment associations, "
        "versus 0 explicit reference file mappings and 125 reference fragments. No folder mappings exist.", "",
        "## Results", "", "| Metric | TP | FP | FN | Precision | Recall | F1 |", "| --- | ---: | ---: | ---: | ---: | ---: | ---: |",
    ]
    for name, tp, fp, fn, precision, recall, f1, status, note in metrics:
        pct = lambda value: "N/A" if value is None else f"{100 * value:.2f}%"
        report.append(f"| {name} | {tp if tp is not None else 'N/A'} | {fp if fp is not None else 'N/A'} | "
                      f"{fn if fn is not None else 'N/A'} | {pct(precision)} | {pct(recall)} | {pct(f1)} |")
    report.extend([
        "", "## Interpretation and limits", "",
        "- Precision = TP/(TP+FP), recall = TP/(TP+FN), F1 = 2TP/(2TP+FP+FN). Undefined scores are blank in CSV and N/A here; no fabricated 0% or 100%.",
        "- Raw-name scores preserve generated names. Aligned scores use the explicit correspondence DueDates -> TaskDueDates. Both models place this feature below TaskManagement and its implementation covers task dates, overdue display, persistence, and ordering. No other name/hierarchy correspondence is introduced.",
        "- File presence unions explicit file mappings, expanded folder mappings, and fragment-to-file projections. Comparison is on distinct (file, feature) pairs; annotation syntax/granularity differences do not cause mismatches at this level.",
        "- Step-010's reference explicit mapping CSV is empty although v010 source has mappings. It remains unchanged. Explicit mapping correctness is not evaluable until this oracle gap is resolved. File-presence scores are partial-reference diagnostics: unmatched mappings can be valid relationships missing from the reference.",
        "- Fragment exact scores use (file, context, normalized statement, feature) and preserve repeated associations with one-to-one matching. Formatting is normalized; code rewrites are not automatically equated by fuzzy string similarity.",
        "- Reviewed role alignment adds 13 manually inspected one-to-one correspondences after 109 exact feature-aligned matches. These cover callback refactoring, renamed overdue UI variables/selectors, UI composition, serialization/parsing roles, and CSS property roles. Every pair and rationale is in comparison.csv. This is an inspectable role-level judgment, not a proof of behavioral equivalence.",
        "- The resulting 16 unmatched generated associations and 3 unmatched reference associations are counted FP/FN only under the supplied closed-reference overlap metric. These are not all demonstrated annotation errors: generated code includes additional dates/ordering/service annotations, whereas reference getters/setters and ordering implementation lack inline due-date coverage. Implementation differences and reference incompleteness affect these scores.",
        "- The three remaining reference rows R059-R061 describe a dedicated clear-date button absent from the generated implementation. The prompt does not explicitly require that button. A missing reference-specific control is not by itself proof of incorrect feature traceability.",
        "- Due-date-only results are separate because unchanged inherited annotations inflate the all-features result. Generated overdue logic additionally excludes completed tasks, and date persistence uses a different field order/null representation. These behavioral differences have not been validated by runtime tests.",
        "- CSS alignments compare the roles of properties on an overdue badge; colors, dimensions, selector names, and exact visual appearance differ.",
        "- Feature presence and ancestor-descendant hierarchy scores assess feature identity/structure only. No mandatory/optional/cross-tree constraint or independent domain-validity score is claimed.",
        "", "## Files and reproduction", "",
        "- ../../ground_truth/step-010/: exact reference CSV copies from the pinned main commit.",
        "- ../../generated/step-010/: generated file_mappings.csv and fragments.csv.",
        "- metrics.csv: scores as ratios in [0,1], counts, applicability, and limits.",
        "- comparison.csv: TP/FP/FN evidence, including reviewed statement correspondences. R/G IDs are 1-based CSV data-row numbers, not source line numbers.", "",
        "From the evaluation worktree root: python scripts/evaluate_step_010.py", "",
        "This script reads pinned Git objects and writes only evaluation outputs. It does not change source, stage files, commit, or push. "
        "Uncommitted local agent settings are outside evaluation scope.", "",
        "## Input hashes", "",
    ])
    for name, content in reference_bytes.items():
        report.append(f"- Reference {name} SHA-256: {hashlib.sha256(content).hexdigest()}")
    if notes:
        report.extend(["", "## Extractor notes", "", *["- " + note for note in notes]])
    target = output / "results" / "step-010" / "README.md"
    target.write_text("\n".join(report) + "\n", encoding="utf-8")
    print(json.dumps({"source_commit": generated_commit, "output": str(output), "metrics": metrics}, indent=2))


if __name__ == "__main__":
    main()
