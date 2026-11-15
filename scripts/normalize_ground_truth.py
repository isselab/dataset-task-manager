#!/usr/bin/env python3
"""Normalize benchmark steps into file_mappings.csv and fragments.csv (JDK 21+).

Java fragments are statements/declarations; a fully annotated control statement
includes its body. Partial control coverage is represented by its covered child
statements. CSS fragments are property declarations. Signatures are emitted only
when explicitly line-annotated; standalone braces and comments are excluded.
Java parser formatting is used, then whitespace outside literals is collapsed.
Context distinguishes methods/selectors; repeated statements remain separate rows.
Text matching alone cannot establish equivalence of rewritten implementations.
File mappings remain explicit; file-level evaluation can additionally project
fragments onto (file, feature) pairs. No feature hierarchy propagation is applied.
Folder mappings are emitted only when present. Inline annotations come from
tagged source and remain subject to the user's ground-truth review. Review notes
are collected once in evaluation/ground_truth/README.md, not in every step.
"""

import argparse
import base64
import csv
import io
import json
import re
import subprocess
import tarfile
import tempfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / "evaluation" / "ground_truth"
MARKER = re.compile(r"&(begin|end|line)\[([^\]]+)\]")

# Parse syntax using the installed JDK, without resolving JavaFX dependencies.
JAVA_PARSER = r'''
import com.sun.source.tree.*;
import com.sun.source.util.*;
import javax.tools.*;
import java.nio.file.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

class ExtractStatements {
    public static void main(String[] args) throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var manager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            var task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                List.of("-proc:none"), null, manager.getJavaFileObjects(args));
            var units = task.parse();
            var positions = Trees.instance(task).getSourcePositions();
            for (var unit : units) {
                String source = unit.getSourceFile().getCharContent(true).toString();
                String filename = Path.of(unit.getSourceFile().toUri()).getFileName().toString();
                new TreePathScanner<Void, Void>() {
                    String context = "";
                    void emit(Tree tree, String kind, String text, long start, long end) {
                        if (start < 0 || end <= start) throw new IllegalStateException("Missing source span");
                        var row = List.of(filename, context, Long.toString(start), Long.toString(end),
                            Long.toString(unit.getLineMap().getLineNumber(start)),
                            Long.toString(unit.getLineMap().getLineNumber(end - 1)), kind, text);
                        System.out.println(String.join("\t", row.stream().map(value ->
                            Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8))).toList()));
                    }
                    @Override public Void scan(Tree tree, Void unused) {
                        Tree parent = getCurrentPath() == null ? null : getCurrentPath().getLeaf();
                        boolean parameter = tree instanceof VariableTree && (parent instanceof MethodTree
                            || parent instanceof LambdaExpressionTree || parent instanceof CatchTree
                            || parent instanceof EnhancedForLoopTree);
                        if (tree instanceof StatementTree && !(tree instanceof BlockTree)
                                && !(tree instanceof ClassTree) && !parameter) {
                            emit(tree, "STATEMENT", tree instanceof VariableTree ? tree + ";" : tree.toString(),
                                positions.getStartPosition(unit, tree), positions.getEndPosition(unit, tree));
                        }
                        return super.scan(tree, unused);
                    }
                    @Override public Void visitImport(ImportTree tree, Void unused) {
                        emit(tree, "DECLARATION", tree.toString(), positions.getStartPosition(unit, tree),
                            positions.getEndPosition(unit, tree));
                        return super.visitImport(tree, unused);
                    }
                    @Override public Void visitMethod(MethodTree method, Void unused) {
                        String previous = context;
                        context = method.getName() + "(" + String.join(", ", method.getParameters()
                            .stream().map(p -> p.getType().toString()).toList()) + ")";
                        if (method.getBody() != null) {
                            long start = positions.getStartPosition(unit, method);
                            long end = positions.getStartPosition(unit, method.getBody());
                            emit(method, "DECLARATION", source.substring((int) start, (int) end), start, end);
                        }
                        super.visitMethod(method, unused);
                        context = previous;
                        return null;
                    }
                }.scan(unit, null);
            }
            for (var diagnostic : diagnostics.getDiagnostics()) {
                if (diagnostic.getKind() == Diagnostic.Kind.ERROR)
                    throw new IllegalArgumentException(diagnostic.toString());
            }
        }
    }
}
'''


def git(*args):
    return subprocess.check_output(["git", "-C", str(ROOT), *args], text=True, encoding="utf-8")


def comment_tokens(language):
    pattern = r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|/\*[\s\S]*?\*/'
    return pattern + (r"|//[^\n]*" if language == "java" else "")


def strip_comments(text, language):
    """Preserve literals and newlines while removing comments."""
    return re.sub(comment_tokens(language), lambda m: (" " + "\n" * m[0].count("\n"))
                  if m[0].startswith(("/*", "//")) else m[0], text)


def normalize(text, language):
    text = strip_comments(text, language)
    pattern = r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|\s+'
    return re.sub(pattern, lambda m: " " if m[0][0].isspace() else m[0], text).strip()


def coverage(text, features, language, notes):
    """Track block and line annotations separately; never invent missing bounds."""
    code = strip_comments(text, language).splitlines()
    comments = {}
    for match in re.finditer(comment_tokens(language), text):
        if match[0].startswith(("/*", "//")):
            number = text.count("\n", 0, match.start()) + 1
            for offset, line in enumerate(match[0].splitlines()):
                comments.setdefault(number + offset, []).extend(MARKER.findall(line))
    result, direct, active = {}, {}, []
    for number, line in enumerate(code, 1):
        direct[number] = set()
        for kind, feature in comments.get(number, []):
            if feature not in features:
                raise ValueError("Unknown feature: " + feature)
            if kind in ("begin", "end") and line.strip():
                raise ValueError("Mixed code/block-boundary lines need an explicit policy")
            if kind == "begin":
                active.append(feature)
            elif kind == "end":
                if feature not in active:
                    notes.append(f"Unmatched &end[{feature}] at source line {number}; "
                                 "ignored marker, no missing start inferred.")
                else:
                    index = len(active) - 1 - active[::-1].index(feature)
                    if index != len(active) - 1:
                        notes.append(f"Crossing block boundary for {feature} at source line {number}.")
                    active.pop(index)
            else:
                direct[number].add(feature)
        if line.strip():
            result[number] = set(active) | direct[number]
    if active:
        raise ValueError("Unclosed inline annotations: " + ", ".join(active))
    return result, direct


def contains(outer, inner):
    return outer["start"] <= inner["start"] and inner["end"] <= outer["end"]


def java_fragments(source, features, parser_dir, notes):
    with tempfile.TemporaryDirectory(prefix="normalization-source-") as temp_name:
        temp = Path(temp_name)
        java_files, lookup = [], {}
        for path, text in source.items():
            if path.endswith(".java"):
                target = temp / path
                target.parent.mkdir(parents=True, exist_ok=True)
                # Keep package directories and send absolute filenames to the parser.
                target.write_bytes(text.encode("utf-8"))
                java_files.append(str(target))
                basename = target.name
                if basename in lookup:
                    raise ValueError("Duplicate Java basenames require parser path identifiers")
                lookup[basename] = path
        output = subprocess.check_output(
            ["java", "-cp", str(parser_dir), "ExtractStatements", *java_files],
            text=True, encoding="utf-8")
    nodes_by_file = {path: [] for path in lookup.values()}
    for row in output.splitlines():
        filename, context, start, end, first, last, kind, statement = [
            base64.b64decode(value).decode() for value in row.split("\t")]
        nodes_by_file[lookup[filename]].append(dict(
            context=context, start=int(start), end=int(end), first=int(first),
            last=int(last), kind=kind, statement=normalize(statement, "java")))
    fragments = []
    for path, nodes in nodes_by_file.items():
        local_notes = []
        covered, direct = coverage(source[path], features, "java", local_notes)
        notes.extend(f"{path}: {note}" for note in local_notes)
        chosen = {}
        for index, node in enumerate(nodes):
            if node["kind"] != "STATEMENT":
                continue
            labels = [labels for line, labels in covered.items()
                      if node["first"] <= line <= node["last"]]
            chosen[index] = set.intersection(*labels) if labels else set()
        # A marker on any physical part of a multiline statement annotates that
        # complete statement. Choose innermost statements touched by the marker,
        # then suppress child rows when a complete parent is selected below.
        for line, labels in direct.items():
            if not labels:
                continue
            candidates = [i for i, node in enumerate(nodes)
                          if node["kind"] == "STATEMENT" and node["first"] <= line <= node["last"]]
            if not candidates:
                candidates = [i for i, node in enumerate(nodes)
                              if node["kind"] == "DECLARATION" and node["first"] <= line <= node["last"]]
            leaves = [i for i in candidates if not any(
                j != i and contains(nodes[i], nodes[j]) and
                (nodes[i]["start"], nodes[i]["end"]) != (nodes[j]["start"], nodes[j]["end"])
                for j in candidates)]
            if not leaves:
                notes.append(f"{path}: &line marker at source line {line} has no statement/declaration.")
            for i in leaves:
                chosen.setdefault(i, set()).update(labels)
        for index, node in enumerate(nodes):
            for feature in sorted(chosen.get(index, set())):
                if any(feature in labels and i != index and contains(nodes[i], node)
                       and (nodes[i]["start"], nodes[i]["end"]) != (node["start"], node["end"])
                       for i, labels in chosen.items()):
                    continue
                fragments.append((path, node["context"], node["statement"], feature))
        # Flag partial annotations whose labeled executable portion is not
        # represented by an exported parent or child, rather than silently losing it.
        for index, node in enumerate(nodes):
            if node["kind"] != "STATEMENT":
                continue
            for line in range(node["first"], node["last"] + 1):
                if line not in covered:
                    continue
                for feature in covered[line]:
                    if any(feature in labels and nodes[i]["first"] <= line <= nodes[i]["last"]
                           for i, labels in chosen.items()):
                        continue
                    text = strip_comments(source[path], "java").splitlines()[line - 1].strip()
                    if text and text not in ("{", "}", "};"):
                        note = f"{path}: partially covered code at source line {line} ({feature}) needs review."
                        if note not in notes:
                            notes.append(note)
    return fragments


def css_fragments(source, features, notes):
    fragments = []
    for path, text in sorted(source.items()):
        if not path.endswith(".css"):
            continue
        local_notes = []
        covered, direct = coverage(text, features, "css", local_notes)
        notes.extend(f"{path}: {note}" for note in local_notes)
        clean = strip_comments(text, "css")
        for rule in re.finditer(r"([^{}]+)\{([^{}]*)\}", clean):
            selector = normalize(rule[1], "css")
            for declaration in re.finditer(r"[-\w]+\s*:\s*[^;{}]+;", rule[2]):
                start = rule.start(2) + declaration.start()
                end = rule.start(2) + declaration.end()
                first = clean.count("\n", 0, start) + 1
                last = clean.count("\n", 0, end - 1) + 1
                labels = [labels for line, labels in covered.items() if first <= line <= last]
                associations = set.intersection(*labels) if labels else set()
                associations.update(feature for line, labels in direct.items()
                                    if first <= line <= last for feature in labels)
                for feature in sorted(associations):
                    fragments.append((path, selector, normalize(declaration[0], "css"), feature))
    return fragments


def mapping_pairs(truth, name, features, candidates):
    lines = [line.strip() for line in (truth / name).read_text(encoding="utf-8").splitlines()
             if line.strip()]
    if len(lines) % 2:
        raise ValueError("Incomplete mapping entry in " + str(truth / name))
    rows = set()
    for target, labels in zip(lines[::2], lines[1::2]):
        normalized = target.replace("\\", "/").rstrip("/")
        matches = [path for path in candidates if path == normalized or path.endswith("/" + normalized)]
        if len(matches) != 1:
            raise ValueError(f"Ambiguous or missing mapping target: {target}: {matches}")
        for feature in map(str.strip, labels.split(",")):
            if feature not in features:
                raise ValueError("Unknown mapped feature: " + feature)
            rows.add((matches[0], feature))
    return sorted(rows)


def normalize_step(step_path, parser_dir):
    step = json.loads(step_path.read_text(encoding="utf-8"))
    commit = git("rev-parse", step["after_version"] + "^{commit}").strip()
    truth = step_path.parent / step["ground_truth"]
    model = (truth / ".feature-model").read_text(encoding="utf-8")
    features = set(model.split())
    notes = []
    if model.splitlines() != git("show", commit + ":project/.feature-model").splitlines():
        notes.append("Benchmark feature hierarchy differs from tagged project hierarchy; "
                     "benchmark model used for feature validation.")
    # Git archive reads every source from the same immutable version without
    # changing the checkout. No extracted source copies are persisted.
    archive = subprocess.check_output(["git", "-C", str(ROOT), "archive", commit, "project/src"])
    source = {}
    with tarfile.open(fileobj=io.BytesIO(archive)) as bundle:
        for member in bundle.getmembers():
            if member.isfile() and member.name.endswith((".java", ".css")):
                source[member.name.removeprefix("project/")] = (
                    bundle.extractfile(member).read().decode("utf-8").replace("\r\n", "\n").replace("\r", "\n"))
    mappings = mapping_pairs(truth, ".feature-to-file", features, list(source))
    folders = sorted({str(Path(path).parent).replace("\\", "/") for path in source})
    folder_mappings = mapping_pairs(truth, ".feature-to-folder", features, folders)
    if not mappings:
        notes.append("Ground-truth .feature-to-file is empty; file_mappings.csv contains only its header. "
                     "No project mappings substituted.")
    fragments = java_fragments(source, features, parser_dir, notes)
    fragments.extend(css_fragments(source, features, notes))
    return dict(step=step["id"], version=step["after_version"], commit=commit,
                mappings=mappings, folders=folder_mappings, fragments=fragments, notes=notes)


def save_step(result):
    destination = DEST / result["step"]
    destination.mkdir(parents=True, exist_ok=True)
    outputs = [("file_mappings.csv", ["file", "feature"], result["mappings"]),
               ("fragments.csv", ["file", "context", "statement", "feature"], result["fragments"])]
    if result["folders"]:
        outputs.append(("folder_mappings.csv", ["folder", "feature"], result["folders"]))
    for filename, fields, rows in outputs:
        with (destination / filename).open("w", encoding="utf-8", newline="") as stream:
            writer = csv.writer(stream, lineterminator="\n")
            writer.writerow(fields)
            writer.writerows(rows)


def save_notes(results):
    lines = [
        "# Normalized ground truth", "",
        "Each step contains file_mappings.csv and fragments.csv. Folder mappings are "
        "saved as folder_mappings.csv only when present; all current inputs are empty.", "",
        "Fragments contain file, context, statement, feature. Paths are relative to project/. "
        "Java statements use JDK parser formatting and collapsed whitespace outside literals. "
        "CSS fragments are property declarations, with their selector as context.", "",
        "A fully covered control statement includes its body. When coverage changes inside "
        "a control statement, covered child statements are exported separately. Per feature, "
        "containing statements suppress duplicate child coverage. A line marker on a "
        "multiline statement covers that whole statement. Repeated independent statements "
        "remain separate rows. Explicitly line-annotated imports/method headers are retained "
        "as declaration text when no executable statement occupies that line.", "",
        "File mappings are explicit benchmark mappings; fragments can additionally be "
        "projected onto (file, feature) pairs for file-level comparison. Parent features "
        "are not propagated to code. Empty benchmark mappings are not filled from source. "
        "Malformed unmatched end markers are reported and ignored; their start is never inferred. "
        "Unannotated implementations are not assigned features.", "",
        "Statement text handles formatting and line wrapping; rewritten implementations "
        "still require a separate semantic alignment rule. Inline source annotations "
        "remain subject to manual ground-truth review.", "",
        "Reproduce with: python scripts/normalize_ground_truth.py", "",
        "| Step | Version | File pairs | Fragment pairs | Source commit |",
        "| --- | --- | ---: | ---: | --- |",
    ]
    for result in results:
        lines.append(f"| {result['step']} | {result['version']} | {len(result['mappings'])} | "
                     f"{len(result['fragments'])} | {result['commit']} |")
    lines.extend(["", "## Source issues requiring review", ""])
    for result in results:
        for note in result["notes"]:
            lines.append(f"- {result['step']}: {note}")
    (DEST / "README.md").write_text("\n".join(lines) + "\n", encoding="utf-8")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--steps", nargs="+", help="Optional step IDs; default: all benchmark steps")
    args = parser.parse_args()
    entries = json.loads((ROOT / "benchmark/steps.json").read_text(encoding="utf-8"))["steps"]
    if args.steps:
        unknown = set(args.steps) - {entry["id"] for entry in entries}
        if unknown:
            parser.error("Unknown steps: " + ", ".join(sorted(unknown)))
        entries = [entry for entry in entries if entry["id"] in args.steps]
    results = []
    with tempfile.TemporaryDirectory(prefix="normalization-parser-") as temp:
        helper = Path(temp) / "ExtractStatements.java"
        helper.write_text(JAVA_PARSER, encoding="utf-8")
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", temp, str(helper)], check=True)
        for entry in entries:
            result = normalize_step(ROOT / "benchmark" / entry["path"], Path(temp))
            results.append(result)
            print(f"{result['step']}: {len(result['mappings'])} file pairs, "
                  f"{len(result['fragments'])} fragment pairs, {len(result['notes'])} review notes", flush=True)
    # Do not leave a partially rewritten result set if a later step fails.
    for result in results:
        save_step(result)
    if not args.steps:
        save_notes(results)
    print(f"Saved {len(results)} step(s) under {DEST}", flush=True)


if __name__ == "__main__":
    main()
