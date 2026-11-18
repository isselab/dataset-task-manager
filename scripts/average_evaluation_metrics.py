#!/usr/bin/env python3
"""Average the six requested branch evaluations without staging or committing.

First run: python scripts/average_evaluation_metrics.py --refresh-sources
Reproduce from the saved input snapshot: python scripts/average_evaluation_metrics.py
"""

import argparse
from collections import defaultdict
import csv
import hashlib
import io
import math
from pathlib import Path
import re
import statistics
import subprocess


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "evaluation" / "averages"
RUNS = [
    ("eval-v2-step-001-run-01", "step-001"),
    ("eval-v2-step-004-run-01", "step-004"),
    ("eval-v2-step-009-run-02", "step-009"),
    ("eval-v2-step-003-run-03", "step-003"),
    ("eval-v2-step-007-run-01", "step-007"),
    ("eval-v2-step-013-run-01", "step-013"),
]
METRIC_FIELDS = ["metric", "tp", "fp", "fn", "precision", "recall", "f1", "status", "notes"]
SOURCE_FIELDS = ["branch", "step", "source_head_commit", "evaluated_source_commit",
                 "source_metrics_sha256", *METRIC_FIELDS]
AVERAGE_FIELDS = ["metric", "averaging", "runs_total", "runs_with_metric", "runs_with_counts",
                  "precision_runs", "recall_runs", "f1_runs", "tp", "fp", "fn",
                  "precision", "recall", "f1", "status", "notes"]
PRIMARY = ["features_aligned", "hierarchy_aligned", "file_presence_aligned",
           "fragments_feature_aligned_exact", "fragments_reviewed_role_alignment",
           "focus_fragments_reviewed_role_alignment"]


def git(repo, *args):
    return subprocess.check_output(["git", "-c", "safe.directory=" + repo.as_posix(),
                                    "-C", str(repo), *args], text=True, encoding="utf-8").strip()


def read_csv(path):
    with path.open("r", encoding="utf-8", newline="") as stream:
        return list(csv.DictReader(stream))


def csv_bytes(fields, records):
    stream = io.StringIO(newline="")
    writer = csv.DictWriter(stream, fieldnames=fields, lineterminator="\n")
    writer.writeheader()
    writer.writerows(records)
    return stream.getvalue().encode("utf-8")


def refresh_sources():
    worktrees = {}
    for record in git(ROOT, "worktree", "list", "--porcelain").split("\n\n"):
        fields = dict(line.split(" ", 1) for line in record.splitlines() if " " in line)
        if "branch" in fields:
            worktrees[fields["branch"].removeprefix("refs/heads/")] = Path(fields["worktree"])
    records = []
    for branch, step in RUNS:
        if branch not in worktrees:
            raise ValueError("Missing branch worktree: " + branch)
        worktree = worktrees[branch]
        if git(worktree, "branch", "--show-current") != branch:
            raise ValueError("Worktree branch changed: " + branch)
        source_path = worktree / "evaluation" / "results" / step / "metrics.csv"
        content = source_path.read_bytes()
        source_rows = list(csv.DictReader(io.StringIO(content.decode("utf-8-sig"))))
        if not source_rows or set(source_rows[0]) != set(METRIC_FIELDS):
            raise ValueError("Unexpected metrics format: " + str(source_path))
        report = source_path.with_name("README.md").read_text(encoding="utf-8")
        match = re.search(r"Generated commit: ([0-9a-f]{40})", report)
        if not match:
            raise ValueError("Missing evaluated source commit: " + str(source_path))
        provenance = dict(branch=branch, step=step,
                          source_head_commit=git(worktree, "rev-parse", "HEAD"),
                          evaluated_source_commit=match[1],
                          source_metrics_sha256=hashlib.sha256(content).hexdigest())
        records.extend({**provenance, **row} for row in source_rows)
    return records


def ratio(numerator, denominator):
    return numerator / denominator if denominator else None


def validate(records):
    seen = set()
    expected_runs = set(RUNS)
    for row in records:
        run = (row["branch"], row["step"])
        if run not in expected_runs:
            raise ValueError("Unexpected source branch/step: " + str(run))
        identity = (row["branch"], row["metric"])
        if identity in seen:
            raise ValueError("Duplicate branch/metric: " + str(identity))
        seen.add(identity)
        counts = [row[key] for key in ("tp", "fp", "fn")]
        if any(counts) and not all(counts):
            raise ValueError("Partially missing TP/FP/FN: " + str(identity))
        if all(counts):
            tp, fp, fn = map(int, counts)
            if min(tp, fp, fn) < 0:
                raise ValueError("Negative association count")
            expected = dict(precision=ratio(tp, tp + fp), recall=ratio(tp, tp + fn),
                            f1=ratio(2 * tp, 2 * tp + fp + fn))
        else:
            expected = dict(precision=None, recall=None, f1=None)
        for key, value in expected.items():
            actual = float(row[key]) if row[key] != "" else None
            if actual is not None and (not math.isfinite(actual) or not 0 <= actual <= 1):
                raise ValueError("Invalid score: " + str(identity))
            if (actual is None) != (value is None) or (actual is not None and abs(actual - value) > 1e-12):
                raise ValueError("Source score disagrees with counts: " + str(identity))
    if {(row["branch"], row["step"]) for row in records} != expected_runs:
        raise ValueError("Source snapshot does not contain all six requested branches")


def average(records):
    grouped = defaultdict(list)
    for row in records:
        grouped[row["metric"]].append(row)
    results = []
    for metric, rows in grouped.items():
        with_counts = [row for row in rows if row["tp"] != ""]
        totals = {key: sum(int(row[key]) for row in with_counts) if with_counts else None
                  for key in ("tp", "fp", "fn")}
        values = {key: [float(row[key]) for row in rows if row[key] != ""]
                  for key in ("precision", "recall", "f1")}
        statuses = sorted({row["status"] for row in rows})
        note = "Input statuses: " + ", ".join(statuses) + ". "
        if metric.startswith("focus_"):
            note += "Focus features vary by scenario; step-013's empty removal scope is undefined and excluded from macro means. "
        if metric == "removed_feature_references":
            note += "A count check, not a precision/recall/F1 score; present only for step-013. "
        if metric == "folder_mappings":
            note += "No applicable folder mappings in any run. "
        for averaging in ("macro", "micro"):
            result = dict(metric=metric, averaging=averaging, runs_total=len(RUNS),
                          runs_with_metric=len(rows), runs_with_counts=len(with_counts),
                          precision_runs=len(values["precision"]), recall_runs=len(values["recall"]),
                          f1_runs=len(values["f1"]), **totals)
            if averaging == "macro":
                result.update({key: statistics.fmean(scores) if scores else None
                               for key, scores in values.items()})
                explanation = "Independent arithmetic means of defined branch precision, recall, and F1; F1 is not recomputed from mean precision/recall."
            elif with_counts:
                tp, fp, fn = (totals[key] for key in ("tp", "fp", "fn"))
                result.update(precision=ratio(tp, tp + fp), recall=ratio(tp, tp + fn),
                              f1=ratio(2 * tp, 2 * tp + fp + fn))
                explanation = "Compute precision/recall/F1 after pooling branch TP/FP/FN within this metric."
            else:
                result.update(precision=None, recall=None, f1=None)
                explanation = "No numeric association counts to pool."
            result["status"] = "averaged" if any(result[key] is not None for key in values) else "not_aggregatable"
            result["notes"] = note + explanation
            results.append(result)
    return results


def report(records, results):
    lines = ["# Average evaluation metrics", "",
             "Scope: the six branches requested together. Step-010 and all other runs are excluded. "
             "Different evaluation dimensions are kept separate; there is no combined overall accuracy score.", "",
             "## Macro averages (equal weight per branch)", "",
             "| Metric | Precision | Recall | F1 | Contributing branches |",
             "| --- | ---: | ---: | ---: | ---: |"]
    percent = lambda value: "N/A" if value is None else f"{100 * value:.2f}%"
    for row in results:
        if row["averaging"] == "macro" and row["metric"] in PRIMARY:
            lines.append(f"| {row['metric']} | {percent(row['precision'])} | {percent(row['recall'])} | "
                         f"{percent(row['f1'])} | {row['f1_runs']}/{len(RUNS)} |")
    lines.extend(["", "## Micro averages (pooled association counts)", "",
                  "| Metric | TP | FP | FN | Precision | Recall | F1 |",
                  "| --- | ---: | ---: | ---: | ---: | ---: | ---: |"])
    for row in results:
        if row["averaging"] == "micro" and row["metric"] in PRIMARY:
            lines.append(f"| {row['metric']} | {row['tp']} | {row['fp']} | {row['fn']} | "
                         f"{percent(row['precision'])} | {percent(row['recall'])} | {percent(row['f1'])} |")
    lines.extend(["", "## Calculation and interpretation", "",
        "- Macro precision, recall, and F1 are separate arithmetic means of the unrounded source values. Each applicable run receives equal weight. The macro F1 is the mean of branch F1 scores, not the harmonic mean of macro precision and macro recall.",
        "- Micro precision = sum(TP)/(sum(TP)+sum(FP)); micro recall = sum(TP)/(sum(TP)+sum(FN)); micro F1 = 2sum(TP)/(2sum(TP)+sum(FP)+sum(FN)). Larger association sets contribute more weight.",
        "- Numeric zero scores remain included. Undefined/blank scores are excluded rather than replaced by zero or 100%. The CSV records the number of contributing runs for each score and how many runs supply association counts.",
        "- Step-013's archive-removal focus has TP=FP=FN=0, so its focus scores are undefined. Focus macro averages use five branches; its zero counts do not change the pooled totals. Focus features differ across scenarios, so focus averages summarize each run's chosen scenario scope rather than one common feature.",
        "- Folder mapping scores are undefined in all six runs and remain N/A. Removed-feature reference counts appear only for step-013 and are not averaged as accuracy scores. All other common metrics use six branches.",
        "- Raw-name and aligned-name metrics, exact and reviewed fragment metrics, explicit file mappings and combined file presence, and feature/hierarchy metrics remain separate. No score is averaged across these different dimensions.",
        "- These averages inherit the source evaluations' limitations: sparse reference annotations, implementation/file-layout differences, and manually reviewed role correspondences. They describe overlap with the supplied references, not independently demonstrated annotation correctness.",
        "- The tp/fp/fn columns contain pooled totals in both macro and micro rows for audit purposes. Only the micro scores are calculated from those totals. CSV scores are ratios in [0,1]; tables display percentages rounded to two decimals.",
        "", "## Input runs and provenance", "",
        "Each input came from evaluation/results/step-NNN/metrics.csv in the corresponding branch worktree. "
        "The working-file metrics were read without modifying the branch. source_head_commit identifies the branch "
        "containing the saved evaluation; evaluated_source_commit identifies the implementation that was scored.", "",
        "| Branch | Step | Evaluated implementation commit | Branch HEAD at collection | Metrics SHA-256 |",
        "| --- | --- | --- | --- | --- |"])
    for branch, step in RUNS:
        row = next(row for row in records if row["branch"] == branch)
        lines.append(f"| {branch} | {step} | {row['evaluated_source_commit']} | "
                     f"{row['source_head_commit']} | {row['source_metrics_sha256']} |")
    lines.extend(["", "## Files and reproduction", "",
        "- [averages.csv](averages.csv): macro/micro results for every metric, applicability, sample sizes, and pooled counts.",
        "- [source_metrics.csv](source_metrics.csv): source metric rows with branch identity and provenance; sufficient to reproduce averages without the other worktrees.",
        "", "From the main worktree root:", "",
        "```powershell", "python scripts/average_evaluation_metrics.py", "```", "",
        "To deliberately refresh the input snapshot from the same six branch worktrees:", "",
        "```powershell", "python scripts/average_evaluation_metrics.py --refresh-sources", "```", "",
        "The script validates source counts and formulas, writes only main's average artifacts, and never stages or commits.", ""])
    return "\n".join(lines).encode("utf-8")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--refresh-sources", action="store_true", help="Read current metrics from the six branch worktrees")
    args = parser.parse_args()
    if git(ROOT, "branch", "--show-current") != "main":
        parser.error("Run this script in the main branch worktree")
    source_path = OUTPUT / "source_metrics.csv"
    records = refresh_sources() if args.refresh_sources or not source_path.exists() else read_csv(source_path)
    validate(records)
    results = average(records)
    artifacts = {"source_metrics.csv": csv_bytes(SOURCE_FIELDS, records),
                 "averages.csv": csv_bytes(AVERAGE_FIELDS, results),
                 "README.md": report(records, results)}
    OUTPUT.mkdir(parents=True, exist_ok=True)
    for name, content in artifacts.items():
        (OUTPUT / name).write_bytes(content)
    print(f"Saved {len(results)} aggregate rows from {len(records)} source rows across {len(RUNS)} branches to {OUTPUT}")
    for row in results:
        if row["metric"] in ("file_presence_aligned", "fragments_reviewed_role_alignment"):
            print(row["metric"], row["averaging"],
                  "precision={:.4%}, recall={:.4%}, F1={:.4%}".format(row["precision"], row["recall"], row["f1"]))


if __name__ == "__main__":
    main()
