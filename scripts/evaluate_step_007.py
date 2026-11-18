#!/usr/bin/env python3
"""Evaluate a pinned generated implementation against its normalized ground truth.

Requires Python 3.9+ and JDK 21+. Writes evaluation artifacts only; never stages,
commits, switches branches, or modifies project source.
"""
import argparse
from collections import defaultdict, deque
import csv
import hashlib
import io
import json
from pathlib import Path, PurePosixPath
import re
import runpy
import subprocess
import tarfile
import tempfile

REFERENCE = "693cc1e8b57f176f037bac0ab54c9b01de50851c"
CONFIG = {'aliases': {},
 'branch': 'eval-v2-step-007-run-01',
 'commit': 'b888d00190786c5290b12f7ec0cebd7b035767c0',
 'exact': 79,
 'focus_features': ['TaskQuery', 'KeywordSearch', 'StatusFilter'],
 'focus_reason': 'Expected merged TaskQuery and the two source features retained by the generated model.',
 'generated': 102,
 'generated_sha256': 'bbb143d0d918f363e4b8fddde4f532e901aa0a6050f08e4b7264d4aac0c781cc',
 'notes': ['KeywordSearch and StatusFilter remain children of TaskQuery in the generated model. The '
           'reference removes those identities through a merge. They are deliberately not aliased to '
           'TaskQuery, which would conceal the merge difference.',
           'Query behavior moves from a mapped TaskQuery.java class into annotated TaskService.createQuery. '
           'File-level path mismatch and fragment representation mismatch are implementation-dependent '
           'diagnostics.',
           'The generated model also retains the v006 hierarchy arrangement; ancestor-descendant scores '
           'expose those structural differences.',
           'Eight reviewed role pairs keep differing feature labels and therefore remain FP/FN; their code '
           'correspondence is documented without granting feature-association credit.'],
 'reference': 88,
 'reference_sha256': 'e018f1f5250d4349e2bd5b81ed3a5cccdf04e306c6e0d48b72776e719f9c29e6',
 'reviewed': [(14,
               14,
               'Apply combined query predicate: Runnable with TaskQuery constructor becomes listener using '
               'taskService.createQuery.'),
              (8,
               8,
               'Search text-field declaration; generated KeywordSearch differs from expected merged '
               'TaskQuery, so no TP credit.'),
              (9,
               9,
               'Search prompt; generated KeywordSearch differs from expected merged TaskQuery, so no TP '
               'credit.'),
              (10,
               10,
               'Filtered task-list declaration; generated KeywordSearch differs from expected merged '
               'TaskQuery, so no TP credit.'),
              (11,
               11,
               'Status selector declaration; generated StatusFilter differs from expected merged TaskQuery, '
               'so no TP credit.'),
              (12,
               12,
               'Status selector values; constants move to service and feature remains StatusFilter rather '
               'than TaskQuery.'),
              (13, 13, 'Select initial status value; StatusFilter remains distinct from expected TaskQuery.'),
              (36,
               38,
               'Compose query UI; generated feature StatusFilter differs from merged reference TaskQuery.'),
              (37,
               39,
               'Compose main view; generated KeywordSearch differs from merged reference TaskQuery.')],
 'step': '007'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    repo = args.repo.resolve()
    output = (args.output or repo / "evaluation").resolve()
    step = "step-" + CONFIG["step"]
    aliases = CONFIG["aliases"]

    def git(*arguments):
        return subprocess.check_output(["git", "-c", "safe.directory=" + repo.as_posix(),
                                        "-C", str(repo), *arguments])

    def read(ref, path):
        return git("show", ref + ":" + path)

    def canonical(feature):
        return aliases.get(feature, feature)

    def csv_rows(content):
        return [tuple(row) for row in list(csv.reader(io.StringIO(content.decode("utf-8"))))[1:]]

    def csv_bytes(fields, records):
        stream = io.StringIO(newline="")
        writer = csv.writer(stream, lineterminator="\n")
        writer.writerow(fields)
        writer.writerows(records)
        return stream.getvalue().encode("utf-8")

    def save(path, content):
        target = output / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(content)

    reference_bytes = {name: read(REFERENCE, f"evaluation/ground_truth/{step}/{name}")
                       for name in ("file_mappings.csv", "fragments.csv")}
    expected_fragments = csv_rows(reference_bytes["fragments.csv"])
    expected_mappings = set(csv_rows(reference_bytes["file_mappings.csv"]))
    model = read(REFERENCE, f"benchmark/steps/{step}/ground-truth/.feature-model").decode()
    metadata = json.loads(read(REFERENCE, f"benchmark/steps/{step}/step.json"))
    reference_source = git("rev-parse", metadata["after_version"] + "^{commit}").decode().strip()
    commit = git("rev-parse", CONFIG["commit"] + "^{commit}").decode().strip()
    archive = git("archive", commit, "project")
    with tarfile.open(fileobj=io.BytesIO(archive)) as bundle:
        files = {member.name.removeprefix("project/"):
                 bundle.extractfile(member).read().decode("utf-8").replace("\r\n", "\n")
                 for member in bundle.getmembers() if member.isfile()
                 and (member.name.startswith("project/src/") or member.name == "project/.feature-model")}
    generated_model = files[".feature-model"]
    features = set(generated_model.split())
    mappings, folders, notes = set(), set(), []
    for path, text in files.items():
        name = PurePosixPath(path).name
        if name not in (".feature-to-file", ".feature-to-folder"):
            continue
        lines = [line.strip() for line in text.splitlines() if line.strip()]
        if len(lines) % 2:
            raise ValueError("Incomplete mapping: " + path)
        for target, labels in zip(lines[::2], lines[1::2]):
            target = str(PurePosixPath(path).parent / target)
            for feature in map(str.strip, labels.split(",")):
                if feature not in features:
                    raise ValueError("Unknown mapped feature: " + feature)
                if name == ".feature-to-file":
                    if target not in files:
                        raise ValueError("Missing mapping target: " + target)
                    mappings.add((target, feature))
                else:
                    folders.add((target, feature))
                    mappings.update((p, feature) for p in files
                                    if p.startswith(target + "/") and p.endswith((".java", ".css")))
    with tempfile.TemporaryDirectory(prefix="evaluate-annotations-") as temp:
        temp = Path(temp)
        module_file = temp / "normalize_ground_truth.py"
        module_file.write_bytes(read(REFERENCE, "scripts/normalize_ground_truth.py"))
        module = runpy.run_path(str(module_file))

        def expand_shared(text, language):
            # Expand shared markers in comments only. Preserve physical lines and
            # close expanded feature scopes in reverse order to avoid false crossing.
            def replace_token(token):
                if not token[0].startswith(("/*", "//")):
                    return token[0]
                def replace_marker(marker):
                    kind, names = marker.groups()
                    names = [name.strip() for name in names.split(",")]
                    if len(names) != len(set(names)):
                        raise ValueError("Repeated feature in shared marker")
                    if kind == "end":
                        names.reverse()
                    return " ".join("&" + kind + "[" + name + "]" for name in names)
                return module["MARKER"].sub(replace_marker, token[0])
            return re.sub(module["comment_tokens"](language), replace_token, text)

        source = {path: expand_shared(text, Path(path).suffix[1:])
                  for path, text in files.items() if path.endswith((".java", ".css"))}
        helper = temp / "ExtractStatements.java"
        helper.write_text(module["JAVA_PARSER"], encoding="utf-8")
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(temp), str(helper)], check=True)
        generated = module["java_fragments"](source, features, temp, notes)
        generated.extend(module["css_fragments"](source, features, notes))
    generated_bytes = csv_bytes(["file", "context", "statement", "feature"], generated)
    if hashlib.sha256(reference_bytes["fragments.csv"]).hexdigest() != CONFIG["reference_sha256"]:
        raise ValueError("Reference rows changed: reviewed IDs are no longer valid")
    if hashlib.sha256(generated_bytes).hexdigest() != CONFIG["generated_sha256"]:
        raise ValueError("Generated rows changed: reviewed IDs are no longer valid")

    comparisons, metrics = [], []

    def metric(name, tp, fp, fn, status="evaluated", note=""):
        precision = tp / (tp + fp) if tp is not None and tp + fp else None
        recall = tp / (tp + fn) if tp is not None and tp + fn else None
        f1 = 2 * tp / (2 * tp + fp + fn) if tp is not None and 2 * tp + fp + fn else None
        metrics.append((name, tp, fp, fn, precision, recall, f1, status, note))

    def compare_sets(name, expected, actual, status="evaluated", note=""):
        metric(name, len(expected & actual), len(actual - expected), len(expected - actual), status, note)
        for classification, rows in (("TP", expected & actual), ("FP", actual - expected), ("FN", expected - actual)):
            for row in sorted(rows):
                comparisons.append((name, classification, "", "",
                                    json.dumps(row) if classification != "FP" else "",
                                    json.dumps(row) if classification != "FN" else "", "Set comparison"))

    def model_pairs(text, aligned):
        names, pairs, stack = set(), set(), []
        for line in text.splitlines():
            if not line.strip():
                continue
            depth = (len(line) - len(line.lstrip())) // 4
            name = canonical(line.strip()) if aligned else line.strip()
            if depth > len(stack):
                raise ValueError("Invalid feature hierarchy indentation")
            stack = stack[:depth]
            names.add(name)
            pairs.update((ancestor, name) for ancestor in stack)
            stack.append(name)
        return names, pairs

    for aligned in (False, True):
        suffix = "aligned" if aligned else "raw_names"
        transform = canonical if aligned else lambda feature: feature
        expected_names, expected_hierarchy = model_pairs(model, aligned)
        actual_names, actual_hierarchy = model_pairs(generated_model, aligned)
        compare_sets("features_" + suffix, expected_names, actual_names)
        compare_sets("hierarchy_" + suffix, expected_hierarchy, actual_hierarchy)
        compare_sets("file_presence_" + suffix,
                     {(p, transform(f)) for p, f in expected_mappings | {(r[0], r[3]) for r in expected_fragments}},
                     {(p, transform(f)) for p, f in mappings | {(r[0], r[3]) for r in generated}},
                     "reference_overlap", "Union of explicit/expanded mappings and fragment projections. Sparse reference and implementation/file layout differences affect this diagnostic.")
    compare_sets("explicit_file_mappings_aligned", {(p, canonical(f)) for p, f in expected_mappings},
                 {(p, canonical(f)) for p, f in mappings}, "reference_overlap", "Explicit mapping representation only; combined file-presence scores provide abstraction across annotation types.")
    compare_sets("fragment_file_presence_aligned", {(r[0], canonical(r[3])) for r in expected_fragments},
                 {(r[0], canonical(r[3])) for r in generated}, "reference_overlap", "Only fragment-to-file projections, excluding explicit file ownership mappings.")
    if folders:
        compare_sets("folder_mappings_aligned", set(), {(p, canonical(f)) for p, f in folders},
                     "reference_overlap", "Reference folder mappings are empty.")
    else:
        metric("folder_mappings", None, None, None, "not_applicable", "Neither side has folder mappings.")

    def fragments(name, aligned=True, reviewed=False, focus=False):
        def key(row):
            return (*row[:3], canonical(row[3]) if aligned else row[3])
        focus_features = set(CONFIG["focus_features"])
        expected = {i: row for i, row in enumerate(expected_fragments, 1)
                    if not focus or canonical(row[3]) in focus_features}
        actual = {i: row for i, row in enumerate(generated, 1)
                  if not focus or canonical(row[3]) in focus_features}
        lookup = defaultdict(deque)
        for i, row in actual.items():
            lookup[key(row)].append(i)
        matches = []
        for reference_id, row in expected.items():
            if lookup[key(row)]:
                matches.append((reference_id, lookup[key(row)].popleft(), "Exact normalized statement/context match"))
        used_reference = {r for r, _, _ in matches}
        used_generated = {g for _, g, _ in matches}
        mismatches = {}
        if reviewed:
            for reference_id, generated_id, reason in CONFIG["reviewed"]:
                if reference_id not in expected or generated_id not in actual:
                    continue
                if reference_id in used_reference or generated_id in used_generated:
                    raise ValueError("Reviewed alignment conflicts with an existing match")
                if canonical(expected[reference_id][3]) != canonical(actual[generated_id][3]):
                    mismatches[(reference_id, generated_id)] = reason
                    continue
                matches.append((reference_id, generated_id, "Reviewed role correspondence: " + reason))
                used_reference.add(reference_id)
                used_generated.add(generated_id)
        for reference_id, generated_id, reason in matches:
            comparisons.append((name, "TP", f"R{reference_id:03}", f"G{generated_id:03}",
                                json.dumps(expected[reference_id]), json.dumps(actual[generated_id]), reason))
        for reference_id in sorted(expected.keys() - used_reference):
            reason = "No aligned generated counterpart; implementation difference or omitted annotation may explain this."
            for (r, g), detail in mismatches.items():
                if r == reference_id:
                    reason = f"Reviewed role corresponds to G{g:03}, but feature labels differ: " + detail
            comparisons.append((name, "FN", f"R{reference_id:03}", "", json.dumps(expected[reference_id]), "", reason))
        for generated_id in sorted(actual.keys() - used_generated):
            reason = "No aligned reference counterpart; may be a valid additional implementation/annotation."
            for (r, g), detail in mismatches.items():
                if g == generated_id:
                    reason = f"Reviewed role corresponds to R{r:03}, but feature labels differ: " + detail
            comparisons.append((name, "FP", "", f"G{generated_id:03}", "", json.dumps(actual[generated_id]), reason))
        metric(name, len(matches), len(actual) - len(matches), len(expected) - len(matches),
               "reviewed_role_alignment" if reviewed else "lexical_diagnostic",
               "Closed-reference overlap; not pure annotation accuracy on a common implementation. "
               + ("Focus: " + ", ".join(CONFIG["focus_features"]) if focus else "All features."))

    fragments("fragments_raw_names", aligned=False)
    fragments("fragments_feature_aligned_exact")
    fragments("fragments_reviewed_role_alignment", reviewed=True)
    fragments("focus_fragments_reviewed_role_alignment", reviewed=True, focus=True)
    removed = {feature for change in metadata["expected_changes"] if change["operation"] == "remove"
               for feature in change["features_before"]}
    if removed:
        retained_names = sorted(removed & {canonical(f) for f in features})
        retained_mapping = sum(canonical(f) in removed for _, f in mappings)
        retained_fragments = sum(canonical(row[3]) in removed for row in generated)
        metric("removed_feature_references", None, None, None, "count_check",
               f"Removed features: {sorted(removed)}; retained model features: {retained_names}; "
               f"retained file associations: {retained_mapping}; retained fragment associations: {retained_fragments}. "
               "Compatibility parsing of old archive data is not itself a stale annotation.")

    # All extraction, alignment, and arithmetic completes before writing outputs.
    for name, content in reference_bytes.items():
        save(f"ground_truth/{step}/{name}", content)
    save(f"generated/{step}/file_mappings.csv", csv_bytes(["file", "feature"], sorted(mappings)))
    save(f"generated/{step}/fragments.csv", generated_bytes)
    if folders:
        save(f"generated/{step}/folder_mappings.csv", csv_bytes(["folder", "feature"], sorted(folders)))
    save(f"results/{step}/metrics.csv", csv_bytes(["metric", "tp", "fp", "fn", "precision", "recall", "f1", "status", "notes"], metrics))
    save(f"results/{step}/comparison.csv", csv_bytes(["metric", "classification", "reference_id", "generated_id", "reference_record", "generated_record", "reason"], comparisons))
    lines = [f"# Evaluation: {CONFIG['branch']}", "",
             f"Generated commit: {commit}. Reference CSVs/normalizer: {REFERENCE}. Reference source: {reference_source}.", "",
             f"Reference: {len(expected_mappings)} explicit file mappings, {len(expected_fragments)} fragments. "
             f"Generated: {len(mappings)} explicit/expanded file mappings, {len(generated)} fragments.", "",
             "## Scores", "", "| Metric | TP | FP | FN | Precision | Recall | F1 |",
             "| --- | ---: | ---: | ---: | ---: | ---: | ---: |"]
    for name, tp, fp, fn, precision, recall, f1, status, note in metrics:
        percent = lambda value: "N/A" if value is None else f"{value*100:.2f}%"
        count = lambda value: "N/A" if value is None else str(value)
        lines.append(f"| {name} | {count(tp)} | {count(fp)} | {count(fn)} | "
                     f"{percent(precision)} | {percent(recall)} | {percent(f1)} |")
    lines.extend(["", "## Definitions and scope", "",
        "- Precision = TP/(TP+FP); recall = TP/(TP+FN); F1 = 2TP/(2TP+FP+FN). Undefined values are blank in CSV and N/A here. Empty sets are not assigned an invented 100% score.",
        "- Feature presence and hierarchy compare feature names and strict ancestor-descendant pairs. Raw-name and aligned-name results are both retained.",
        "- Feature-name correspondences: " + (json.dumps(aliases, sort_keys=True) if aliases else "none") + ". Only documented one-to-one feature correspondences are applied; unrelated/retained features are not collapsed to improve scores.",
        "- File presence compares distinct (file, feature) pairs after combining explicit file mappings, recursive folder expansion when present, and fragment-to-file projections. Explicit mapping scores remain separate to expose representation differences. Paths are relative to project/; different physical file layouts can affect these scores.",
        "- Fragment scores measure explicit inline coverage only. Whole-file/folder mappings are not expanded into statement coverage. Fields are file, context, normalized statement, feature. Repeated independent associations are retained with one-to-one matching.",
        "- Exact matching uses statement/context text after Java parser formatting and whitespace normalization outside literals. CSS units are property declarations. Comma-separated shared feature markers are expanded in comments only; code and string literals are preserved.",
        "- Reviewed role alignment adds explicitly inspected one-to-one code-role correspondences. Feature labels must still agree after the documented aliases. A reviewed role with differing labels stays FP/FN, with a cross-reference in comparison.csv. No fuzzy matching or unstated automatic variable/method renaming is used.",
        "- Role alignment is an inspectable judgment, not proof of identical behavior. Some rewrites split/merge code units; unpaired units remain unmatched rather than receiving invented many-to-one credit. File and fragment scores are overlap against a potentially sparse reference; FP/FN are not all demonstrated annotation defects.",
        "- Focus fragment scope: " + ", ".join(CONFIG["focus_features"]) + ". " + CONFIG["focus_reason"],
        "- Source is read from pinned commits, excluding local settings, prompts, tools, and setup files. Original ground truth is copied unchanged. No source edits, staging, commits, or pushes are performed by this evaluator.",
        "- This evaluation does not independently establish runtime implementation correctness, mandatory/optional constraints, or a quantitative domain-validity score.",
        "", "## Findings and review notes", "",
        *["- " + note for note in CONFIG["notes"]],
        *["- Extractor: " + note for note in notes],
        "", "## Reviewed statement correspondences", "",
        "R/G IDs are 1-based data-row numbers in the copied reference/generated fragment CSVs; they are not source line numbers.", "",
        "| Reference | Generated | Feature labels agree | Rationale |", "| --- | --- | --- | --- |"])
    for r, g, reason in CONFIG["reviewed"]:
        same = canonical(expected_fragments[r-1][3]) == canonical(generated[g-1][3])
        lines.append(f"| R{r:03} | G{g:03} | {'yes' if same else 'no: stays FP/FN'} | {reason.replace('|', '/')} |")
    lines.extend(["", "## Reproduction and files", "",
                  f"Run from this branch's worktree root: python scripts/evaluate_step_{CONFIG['step']}.py", "",
                  f"Reference copies: ../../ground_truth/{step}/. Generated CSVs: ../../generated/{step}/. "
                  "metrics.csv contains counts, ratios, and applicability. comparison.csv contains the complete TP/FP/FN evidence.", "",
                  "## Input hashes", ""])
    for name, content in reference_bytes.items():
        lines.append(f"- Reference {name} SHA-256: {hashlib.sha256(content).hexdigest()}")
    lines.append(f"- Generated fragments.csv SHA-256: {hashlib.sha256(generated_bytes).hexdigest()}")
    save(f"results/{step}/README.md", ("\n".join(lines) + "\n").encode("utf-8"))
    print(json.dumps({"branch": CONFIG["branch"], "step": step, "files": len(mappings), "fragments": len(generated), "metrics": metrics}, indent=2))


if __name__ == "__main__":
    main()
