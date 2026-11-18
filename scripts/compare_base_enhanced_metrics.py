#!/usr/bin/env python3
"""Save base averages and a base/enhanced comparison over the five shared steps.

python scripts/compare_base_enhanced_metrics.py --refresh-base-sources
python scripts/compare_base_enhanced_metrics.py  # reproduce from saved snapshots
Existing enhanced six-run averages are read only and are never overwritten.
"""

import argparse
import csv
import hashlib
import io
from pathlib import Path
import re
import runpy
import subprocess


ROOT = Path(__file__).resolve().parents[1]
BASE_OUTPUT = ROOT / "evaluation" / "averages_base"
COMPARISON_OUTPUT = ROOT / "evaluation" / "base_vs_enhanced"
ENHANCED_OUTPUT = ROOT / "evaluation" / "averages"
BASE_REFERENCE = "02cc7c313aee0e2a5607ead65b4366e726ef82cd"
ENHANCED_REFERENCE = "693cc1e8b57f176f037bac0ab54c9b01de50851c"
BASE_RUNS = [(f"eval-step-{step}-run-01", f"step-{step}") for step in ("001", "003", "004", "009", "013")]
PRIMARY = ["features_aligned", "hierarchy_aligned", "file_presence_aligned",
           "fragments_feature_aligned_exact", "fragments_reviewed_role_alignment",
           "focus_fragments_reviewed_role_alignment"]


def git(repo, *args):
    return subprocess.check_output(["git", "-c", "safe.directory=" + repo.as_posix(),
                                    "-C", str(repo), *args])


def read_csv(path):
    with path.open(encoding="utf-8", newline="") as stream:
        return list(csv.DictReader(stream))


def csv_bytes(fields, records):
    stream = io.StringIO(newline="")
    writer = csv.DictWriter(stream, fieldnames=fields, lineterminator="\n")
    writer.writeheader()
    writer.writerows(records)
    return stream.getvalue().encode("utf-8")


def save(path, content):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(content)


def aggregate(records, runs):
    # Reuse the exact macro/micro arithmetic and undefined-score policy used for
    # enhanced averages, while supplying the cohort being aggregated.
    module = runpy.run_path(str(ROOT / "scripts" / "average_evaluation_metrics.py"))
    module["average"].__globals__["RUNS"] = runs
    module["validate"](records)
    return module["average"](records), module


def collect_base():
    worktrees = {}
    for record in git(ROOT, "worktree", "list", "--porcelain").decode().strip().split("\n\n"):
        fields = dict(line.split(" ", 1) for line in record.splitlines() if " " in line)
        if "branch" in fields:
            worktrees[fields["branch"].removeprefix("refs/heads/")] = Path(fields["worktree"])
    records = []
    for branch, step in BASE_RUNS:
        worktree = worktrees[branch]
        if git(worktree, "branch", "--show-current").decode().strip() != branch:
            raise ValueError("Worktree branch changed: " + branch)
        source = worktree / "evaluation" / "results" / step / "metrics.csv"
        content = source.read_bytes()
        report = source.with_name("README.md").read_text(encoding="utf-8")
        match = re.search(r"Generated commit: ([0-9a-f]{40})", report)
        if not match:
            raise ValueError("Missing evaluated implementation commit: " + branch)
        metadata = dict(branch=branch, step=step,
                        source_head_commit=git(worktree, "rev-parse", "HEAD").decode().strip(),
                        evaluated_source_commit=match[1], source_metrics_sha256=hashlib.sha256(content).hexdigest())
        records.extend({**metadata, **row} for row in csv.DictReader(io.StringIO(content.decode("utf-8"))))
    return records


def number(value):
    return None if value in (None, "") else float(value)


def delta(base, enhanced):
    a, b = number(base), number(enhanced)
    return None if a is None or b is None else 100 * (b - a)


def percent(value):
    value = number(value)
    return "N/A" if value is None else f"{100 * value:.2f}%"


def cohort_report(records, averages):
    lines = ["# Base-agent average evaluation metrics", "",
             "Five base runs: steps 001, 003, 004, 009, and 013, each run-01. "
             "The original enhanced six-run averages in ../averages/ are unchanged. "
             "Use ../base_vs_enhanced/ for comparison over the same five steps.", ""]
    for averaging, title in (("macro", "Macro averages: equal weight per applicable run"),
                             ("micro", "Micro averages: pooled TP/FP/FN counts")):
        lines.extend(["## " + title, "", "| Metric | Precision | Recall | F1 | Defined input F1 scores |",
                      "| --- | ---: | ---: | ---: | ---: |"])
        for row in averages:
            if row["averaging"] == averaging and row["metric"] in PRIMARY:
                lines.append(f"| {row['metric']} | {percent(row['precision'])} | {percent(row['recall'])} | "
                             f"{percent(row['f1'])} | {row['f1_runs']}/5 |")
        lines.append("")
    lines.extend(["## Calculation and limits", "",
        "- Macro precision, recall, and F1 are independent arithmetic means of unrounded branch values. Macro F1 is not recomputed from macro precision/recall.",
        "- Micro scores are recomputed after pooling TP/FP/FN separately within each evaluation dimension. Score units in CSV are ratios, not percentages.",
        "- Numeric zeros are included. Undefined scores are excluded rather than filled with zero or 100%. Folder mappings remain N/A. Step-013 archive-removal focus has empty expected/generated associations, so focus macro averages use four defined runs.",
        "- Raw/aligned features and hierarchy, file presence/explicit mappings, exact/reviewed fragments, and scenario focus remain separate. No overall accuracy is invented by averaging different dimensions together.",
        "- The source evaluations use the same normalization and matching policies as the enhanced runs. They measure overlap against a potentially sparse reference, with inspectable role-level judgments. File layout, code-unit splitting, and implementation differences affect results independently of annotation quality.",
        "- In particular, base step-001 relocates classes into taskmanager packages, producing zero literal-path file overlap. Step-009 does not introduce a separate TaskOrdering feature. Neither difference is silently normalized away.",
        "", "## Sources", "", "| Base branch | Step | Evaluated implementation commit | Metrics SHA-256 |",
        "| --- | --- | --- | --- |"])
    for branch, step in BASE_RUNS:
        row = next(row for row in records if row["branch"] == branch)
        lines.append(f"| {branch} | {step} | {row['evaluated_source_commit']} | {row['source_metrics_sha256']} |")
    lines.extend(["", "## Files and reproduction", "",
        "- [averages.csv](averages.csv): macro/micro results for every metric, applicability, sample sizes, and pooled counts.",
        "- [source_metrics.csv](source_metrics.csv): all base source rows plus branch/commit/hash provenance.",
        "- [Matched comparison](../base_vs_enhanced/README.md): base versus enhanced results on the same five steps.", "",
        "Run from main: python scripts/compare_base_enhanced_metrics.py", "",
        "Refresh base inputs explicitly: python scripts/compare_base_enhanced_metrics.py --refresh-base-sources", ""])
    return ("\n".join(lines)).encode("utf-8")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--refresh-base-sources", action="store_true")
    args = parser.parse_args()
    if git(ROOT, "branch", "--show-current").decode().strip() != "main":
        parser.error("Run this script in the main worktree")
    protected = {path: hashlib.sha256(path.read_bytes()).hexdigest()
                 for path in ENHANCED_OUTPUT.rglob("*") if path.is_file()}
    source_path = BASE_OUTPUT / "source_metrics.csv"
    base = collect_base() if args.refresh_base_sources or not source_path.exists() else read_csv(source_path)
    base_averages, average_module = aggregate(base, BASE_RUNS)
    enhanced_all = read_csv(ENHANCED_OUTPUT / "source_metrics.csv")
    original_module = runpy.run_path(str(ROOT / "scripts" / "average_evaluation_metrics.py"))
    original_module["validate"](enhanced_all)
    shared_steps = {step for _, step in BASE_RUNS}
    enhanced = [row for row in enhanced_all if row["step"] in shared_steps]
    enhanced_runs = list(dict.fromkeys((row["branch"], row["step"]) for row in enhanced))
    if len(enhanced_runs) != 5 or {step for _, step in enhanced_runs} != shared_steps:
        raise ValueError("Expected one enhanced run for each of the five base steps")
    enhanced_averages, _ = aggregate(enhanced, enhanced_runs)
    # Verify that pinning current-main ground truth did not change the reference
    # relative to the already saved enhanced evaluations.
    for step in shared_steps:
        for name in ("file_mappings.csv", "fragments.csv"):
            path = f"evaluation/ground_truth/{step}/{name}"
            if git(ROOT, "show", BASE_REFERENCE + ":" + path) != git(ROOT, "show", ENHANCED_REFERENCE + ":" + path):
                raise ValueError("Base and enhanced ground-truth snapshots differ: " + path)
    normalizer = "scripts/normalize_ground_truth.py"
    if git(ROOT, "show", BASE_REFERENCE + ":" + normalizer) != git(ROOT, "show", ENHANCED_REFERENCE + ":" + normalizer):
        raise ValueError("Normalizer version differs between cohorts")
    expected = {(row["metric"], row["averaging"]): row for row in base_averages}
    actual = {(row["metric"], row["averaging"]): row for row in enhanced_averages}
    if expected.keys() != actual.keys():
        raise ValueError("Metric sets differ between cohorts")
    comparison = []
    for identity, a in expected.items():
        b = actual[identity]
        row = dict(metric=identity[0], averaging=identity[1], base_runs=5, enhanced_runs=5,
                   scope="steps 001,003,004,009,013")
        for score in ("precision", "recall", "f1"):
            row.update({"base_" + score: a[score], "enhanced_" + score: b[score],
                        score + "_delta_pp": delta(a[score], b[score]),
                        "base_" + score + "_runs": a[score + "_runs"],
                        "enhanced_" + score + "_runs": b[score + "_runs"]})
        for count in ("tp", "fp", "fn"):
            row["base_" + count] = a[count]
            row["enhanced_" + count] = b[count]
        comparison.append(row)
    base_rows = {(row["step"], row["metric"]): row for row in base}
    enhanced_rows = {(row["step"], row["metric"]): row for row in enhanced}
    if base_rows.keys() != enhanced_rows.keys():
        raise ValueError("Paired step/metric sets differ")
    paired = []
    for (step, metric), a in base_rows.items():
        b = enhanced_rows[(step, metric)]
        if metric.startswith("focus_") and a["notes"].split("Focus: ")[-1] != b["notes"].split("Focus: ")[-1]:
            raise ValueError("Focus feature definitions differ: " + step)
        row = dict(step=step, metric=metric, base_branch=a["branch"], enhanced_branch=b["branch"])
        for score in ("precision", "recall", "f1"):
            row.update({"base_" + score: number(a[score]), "enhanced_" + score: number(b[score]),
                        score + "_delta_pp": delta(a[score], b[score])})
        paired.append(row)

    save(BASE_OUTPUT / "source_metrics.csv", csv_bytes(average_module["SOURCE_FIELDS"], base))
    save(BASE_OUTPUT / "averages.csv", csv_bytes(average_module["AVERAGE_FIELDS"], base_averages))
    save(BASE_OUTPUT / "README.md", cohort_report(base, base_averages))
    save(COMPARISON_OUTPUT / "enhanced_matched_averages.csv", csv_bytes(average_module["AVERAGE_FIELDS"], enhanced_averages))
    save(COMPARISON_OUTPUT / "comparison.csv", csv_bytes(list(comparison[0]), comparison))
    save(COMPARISON_OUTPUT / "paired_step_metrics.csv", csv_bytes(list(paired[0]), paired))
    lines = ["# Base versus enhanced agent: matched-step comparison", "",
             "Comparison uses the same five steps: 001, 003, 004, 009, and 013. "
             "Base runs are run-01. Enhanced runs are the already selected v2 runs: "
             "001/run-01, 003/run-03, 004/run-01, 009/run-02, and 013/run-01.", "",
             "The [original enhanced averages](../averages/README.md) include step-007 (six runs) and are preserved unchanged. "
             "They should not be directly subtracted from the five-run base mean. "
             "Here enhanced averages are recalculated over the five shared steps.", ""]
    for averaging in ("macro", "micro"):
        lines.extend([f"## {averaging.capitalize()} comparison: five shared steps", "",
                      "| Metric | Base P | Base R | Base F1 | Enhanced P | Enhanced R | Enhanced F1 | F1 change (pp) |",
                      "| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |"])
        for row in comparison:
            if row["averaging"] == averaging and row["metric"] in PRIMARY:
                change = "N/A" if row["f1_delta_pp"] is None else f"{row['f1_delta_pp']:+.2f}"
                lines.append(f"| {row['metric']} | {percent(row['base_precision'])} | {percent(row['base_recall'])} | "
                             f"{percent(row['base_f1'])} | {percent(row['enhanced_precision'])} | {percent(row['enhanced_recall'])} | "
                             f"{percent(row['enhanced_f1'])} | {change} |")
        lines.append("")
    lines.extend(["## Interpretation", "",
        "- Delta is enhanced minus base, in percentage points. Positive means a higher enhanced score; it is not a relative percentage change.",
        "- Macro P/R/F1 are separate means of branch scores; micro P/R/F1 pool TP/FP/FN. No mixed overall score is calculated across dimensions.",
        "- Focus definitions are checked to be identical for paired steps. Step-013 removal focus is undefined in both variants; focus macro averages therefore have four contributors. Folder scores remain N/A and deletion-reference checks are not averaged as accuracy.",
        "- The two ground-truth CSVs for every shared step and the normalization script were verified byte-identical between main references "
        + BASE_REFERENCE + " (base) and " + ENHANCED_REFERENCE + " (enhanced). Naming aliases, code-role correspondences, and source IDs remain documented in each branch report.",
        "- These are descriptive comparisons of one selected implementation per variant per step. Different selected run numbers and agent nondeterminism are not controlled by this calculation; no significance, causality, or general improvement claim is established.",
        "- File metrics preserve physical paths. Base step-001 relocates classes, while base step-013 renames/refactors the details class. File overlap can change because of implementation layout rather than traceability defects.",
        "- Reviewed fragment alignment is an inspectable one-to-one role judgment, not proof of behavioral equivalence. Unit splitting/merging, inherited annotations, extra valid features, and sparse reference coverage affect both variants. Exact and reviewed results are provided together so this sensitivity remains visible.",
        "- The reviewed fragment macro change is "
        + f"{next(row['f1_delta_pp'] for row in comparison if row['metric'] == 'fragments_reviewed_role_alignment' and row['averaging'] == 'macro'):+.2f} pp; "
        + "the file-presence macro change is "
        + f"{next(row['f1_delta_pp'] for row in comparison if row['metric'] == 'file_presence_aligned' and row['averaging'] == 'macro'):+.2f} pp. "
        + "These overlap differences should not be presented as independently demonstrated changes in annotation quality.",
        "", "## Files", "",
        "- [comparison.csv](comparison.csv): paired-cohort macro/micro precision, recall, F1, pooled counts, contributors, and changes in percentage points.",
        "- [paired_step_metrics.csv](paired_step_metrics.csv): unaveraged base/enhanced scores and changes for each shared step/metric.",
        "- [enhanced_matched_averages.csv](enhanced_matched_averages.csv): enhanced means using five steps instead of the original six.",
        "- [Base averages and source snapshot](../averages_base/README.md). Original enhanced inputs remain at ../averages/source_metrics.csv.", "",
        "## Reproduction", "", "Run from main: python scripts/compare_base_enhanced_metrics.py", "",
        "The default uses saved source snapshots, so branch worktrees are not required. "
        "Use --refresh-base-sources to reread base metrics from the five branch worktrees. "
        "The script does not stage, commit, change branches, or overwrite the original enhanced averages.", "",
        "Enhanced input snapshot SHA-256: " + hashlib.sha256((ENHANCED_OUTPUT / "source_metrics.csv").read_bytes()).hexdigest(), ""])
    save(COMPARISON_OUTPUT / "README.md", ("\n".join(lines)).encode("utf-8"))
    for path, checksum in protected.items():
        if hashlib.sha256(path.read_bytes()).hexdigest() != checksum:
            raise RuntimeError("Original enhanced average artifact changed: " + str(path))
    print(f"Saved base averages for five runs to {BASE_OUTPUT}")
    print(f"Saved matched-step comparison to {COMPARISON_OUTPUT}; original enhanced averages unchanged")
    for row in comparison:
        if row["averaging"] == "macro" and row["metric"] in ("file_presence_aligned", "fragments_reviewed_role_alignment"):
            print(row["metric"], "base F1", percent(row["base_f1"]), "enhanced F1", percent(row["enhanced_f1"]),
                  "delta pp", f"{row['f1_delta_pp']:+.2f}")


if __name__ == "__main__":
    main()
