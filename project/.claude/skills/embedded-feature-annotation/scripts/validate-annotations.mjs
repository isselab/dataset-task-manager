#!/usr/bin/env node
// Structural checks only; semantic ownership and coverage need agent review.
import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const sourceExtensions = new Set([
  ".java",
  ".js",
  ".mjs",
  ".cjs",
  ".ts",
  ".jsx",
  ".tsx",
  ".css",
  ".scss",
  ".c",
  ".h",
  ".cpp",
  ".hpp",
  ".cs",
  ".go",
  ".rs",
  ".py",
  ".sh",
  ".bat",
  ".html",
  ".vue",
  ".svelte",
  ".sql",
]);
const mappingNames = new Set([".feature-to-file", ".feature-to-folder"]);
const annotationPattern = /&(begin|end|line)\[([^\]\r\n]*)\]/g;

// Preserve offsets and line breaks, extracting annotations only from comments.
// This is a lexer, not a full language parser; template literals are opaque.
function scan(text, extension) {
  const masked = text.split("");
  const annotations = [];
  const problems = [];
  const hashComments = [".py", ".sh"].includes(extension);
  const batch = extension === ".bat";
  const sql = extension === ".sql";
  function hide(start, end) {
    for (let i = start; i < end && i < text.length; i++) {
      if (text[i] !== "\n" && text[i] !== "\r") masked[i] = " ";
    }
  }
  function comment(start, end) {
    const body = text.slice(start, end);
    let offset = start;
    for (const line of body.split("\n")) {
      // An annotation starts the comment content; mentions in prose are examples.
      const content = line.replace(
        /^\s*(?:\/\/|\/\*+|<!--|#|--|rem\b|::)?\s*\*?\s*/i,
        "",
      );
      if (/^&(begin|end|line)\b/.test(content)) {
        for (const match of line.matchAll(annotationPattern)) {
          annotations.push({
            kind: match[1],
            feature: match[2],
            offset: offset + match.index,
          });
        }
        if (
          /&(begin|end|line)\b/.test(content.replace(annotationPattern, ""))
        ) {
          problems.push({
            offset,
            message: "Malformed annotation annotation.",
          });
        }
      }
      offset += line.length + 1;
    }
    hide(start, end);
  }
  for (let i = 0; i < text.length; ) {
    const lineStart = i === 0 || text[i - 1] === "\n";
    const batchComment =
      batch && lineStart && /^[ \t]*(?:rem\b|::)/i.test(text.slice(i));
    const lineComment =
      batchComment ||
      (hashComments && text[i] === "#") ||
      (!hashComments && !batch && !sql && text.startsWith("//", i)) ||
      (sql && text.startsWith("--", i));
    if (lineComment) {
      const end = text.indexOf("\n", i);
      comment(i, end < 0 ? text.length : end);
      i = end < 0 ? text.length : end;
    } else if (
      !batch &&
      !hashComments &&
      (text.startsWith("/*", i) || text.startsWith("<!--", i))
    ) {
      const closing = text.startsWith("<!--", i) ? "-->" : "*/";
      const end = text.indexOf(closing, i + 2);
      const stop = end < 0 ? text.length : end + closing.length;
      comment(i, stop);
      i = stop;
    } else if (
      text[i] === "'" ||
      text[i] === '"' ||
      text.charCodeAt(i) === 96
    ) {
      const start = i;
      const quote = text[i];
      const delimiter = text.startsWith(quote.repeat(3), i)
        ? quote.repeat(3)
        : quote;
      i += delimiter.length;
      while (i < text.length) {
        if (text[i] === "\\") i += 2;
        else if (text.startsWith(delimiter, i)) {
          i += delimiter.length;
          break;
        } else i++;
      }
      hide(start, i);
      masked[start] = "x"; // A string expression is code, not an empty line.
      for (let j = start; j < i && j < text.length; j++) {
        if (text[j] === "\n" && j + 1 < i) masked[j + 1] = "x";
      }
    } else i++;
  }
  return { code: masked.join(""), annotations, problems };
}

export function validateSource(text, extension, features) {
  const { code, annotations, problems } = scan(text, extension);
  const lines = code.split("\n");
  const stack = [];
  const issues = [];
  const lineAt = (offset) => text.slice(0, offset).split("\n").length;
  const byLine = new Map();
  for (const problem of problems)
    issues.push({ line: lineAt(problem.offset), message: problem.message });
  for (const annotation of annotations) {
    const line = lineAt(annotation.offset);
    if (!byLine.has(line)) byLine.set(line, []);
    byLine.get(line).push(annotation);
  }
  for (let index = 0; index < lines.length; index++) {
    const line = index + 1;
    const statement = lines[index].trim();
    let covered = stack.length > 0;
    for (const annotation of byLine.get(line) ?? []) {
      const { kind, feature } = annotation;
      if (!/^[A-Z][A-Za-z0-9]*$/.test(feature) || !features.has(feature)) {
        issues.push({
          line,
          message: "Unknown or invalid feature: " + feature + ".",
        });
      }
      if (kind === "begin") {
        stack.push({ feature, line });
        covered = true;
      } else if (kind === "end") {
        const opening = stack.pop();
        if (!opening || opening.feature !== feature) {
          issues.push({
            line,
            message: "Unmatched or incorrectly nested end: " + feature + ".",
          });
        }
      } else {
        covered = true;
        if (!statement || /^\}+;?$/.test(statement)) {
          issues.push({
            line,
            message:
              "Line annotation needs code, not an empty line or bare closing brace.",
          });
        }
      }
    }
    if (
      covered &&
      /^(?:import\s+(?!\()|from\s+\S+\s+import\b|using\s+(?:static\s+)?[\w.]+\s*;)/.test(
        statement,
      )
    ) {
      issues.push({
        line,
        message: "Imports must remain unannotated, including inside fragments.",
      });
    }
  }
  for (const opening of stack) {
    issues.push({
      line: opening.line,
      message: "Unclosed fragment: " + opening.feature + ".",
    });
  }
  return issues;
}

function validateMapping(filename, root, features) {
  const rows = fs
    .readFileSync(filename, "utf8")
    .split(/\r?\n/)
    .map((text, index) => ({ text: text.trim(), line: index + 1 }))
    .filter((row) => row.text);
  const issues = [];
  if (rows.length % 2)
    issues.push({
      line: rows.at(-1).line,
      message: "Mapping needs target/feature pairs.",
    });
  for (let i = 0; i + 1 < rows.length; i += 2) {
    const target = rows[i];
    const feature = rows[i + 1];
    if (!features.has(feature.text))
      issues.push({
        line: feature.line,
        message: "Unknown feature: " + feature.text + ".",
      });
    const resolved = path.resolve(path.dirname(filename), target.text);
    const relative = path.relative(root, resolved);
    if (
      path.isAbsolute(target.text) ||
      relative.startsWith(".." + path.sep) ||
      relative === ".." ||
      path.isAbsolute(relative)
    ) {
      issues.push({
        line: target.line,
        message: "Mapping target must stay inside the project.",
      });
    } else {
      const stat = fs.existsSync(resolved) ? fs.statSync(resolved) : null;
      const folder = path.basename(filename) === ".feature-to-folder";
      if (!stat || (folder ? !stat.isDirectory() : !stat.isFile())) {
        issues.push({
          line: target.line,
          message: "Missing or incorrect mapping target: " + target.text + ".",
        });
      }
    }
  }
  return issues;
}

export function main(argv = process.argv.slice(2)) {
  let root = process.cwd();
  let base = "HEAD";
  let all = false;
  for (let i = 0; i < argv.length; i++) {
    if (argv[i] === "--all") all = true;
    else if (
      ["--root", "--base"].includes(argv[i]) &&
      argv[i + 1] &&
      !argv[i + 1].startsWith("--")
    ) {
      if (argv[i] === "--root") root = path.resolve(argv[++i]);
      else base = argv[++i];
    } else if (argv[i] === "--help") {
      console.log(
        "Usage: node validate-annotations.mjs [--root directory] [--all] [--base commit]",
      );
      return 0;
    } else {
      console.error("Invalid or incomplete option: " + argv[i]);
      return 2;
    }
  }
  try {
    const model = fs.readFileSync(path.join(root, ".feature-model"), "utf8");
    const features = new Set(
      model
        .split(/\r?\n/)
        .map((line) => line.trim())
        .filter(Boolean),
    );
    if (
      !features.size ||
      [...features].some((feature) => !/^[A-Z][A-Za-z0-9]*$/.test(feature))
    ) {
      throw new Error(
        "Feature model must contain nonempty PascalCase feature names.",
      );
    }
    const git = (args) =>
      execFileSync("git", ["-C", root, ...args], {
        encoding: "utf8",
        stdio: ["ignore", "pipe", "pipe"],
      })
        .split("\0")
        .filter(Boolean);
    const listed = git([
      "ls-files",
      "--cached",
      "--others",
      "--exclude-standard",
      "-z",
    ]);
    let changed;
    try {
      changed = all
        ? listed
        : git(["diff", "--relative", "--name-only", "-z", base, "--"]);
    } catch (error) {
      // A freshly initialized repository has no HEAD yet; review all files.
      if (base !== "HEAD") throw error;
      let hasHead = true;
      try {
        git(["rev-parse", "--verify", "HEAD"]);
      } catch {
        hasHead = false;
      }
      if (hasHead) throw error;
      changed = listed;
    }
    const candidates =
      all || changed.includes(".feature-model")
        ? listed
        : [
            ...changed,
            ...git(["ls-files", "--others", "--exclude-standard", "-z"]),
          ];
    const files = [...new Set(candidates)].filter(
      (filename) =>
        mappingNames.has(path.basename(filename)) ||
        sourceExtensions.has(path.extname(filename)),
    );
    let checked = 0;
    let count = 0;
    for (const filename of files) {
      const absolute = path.resolve(root, filename);
      if (!fs.existsSync(absolute) || !fs.statSync(absolute).isFile()) continue;
      checked++;
      const issues = mappingNames.has(path.basename(filename))
        ? validateMapping(absolute, root, features)
        : validateSource(
            fs.readFileSync(absolute, "utf8"),
            path.extname(filename),
            features,
          );
      for (const issue of issues)
        console.error(filename + ":" + issue.line + ": " + issue.message);
      count += issues.length;
    }
    console.log(
      "Annotation validation: " +
        checked +
        " files checked, " +
        count +
        " errors. Semantic review is still required.",
    );
    return count ? 1 : 0;
  } catch (error) {
    console.error("Annotation validation could not run: " + error.message);
    return 2;
  }
}

if (
  process.argv[1] &&
  path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)
) {
  process.exitCode = main();
}
