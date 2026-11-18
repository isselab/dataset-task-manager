# Evaluation: eval-v2-step-009-run-02

Generated commit: ef0f15b8ff5d64548324a8c0c79b0bfa4cd5db52. Reference CSVs/normalizer: 693cc1e8b57f176f037bac0ab54c9b01de50851c. Reference source: 8dab0c2894a983a256b8d9a3b3bb54f0f04cf528.

Reference: 10 explicit file mappings, 108 fragments. Generated: 10 explicit/expanded file mappings, 109 fragments.

## Scores

| Metric | TP | FP | FN | Precision | Recall | F1 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| features_raw_names | 14 | 1 | 1 | 93.33% | 93.33% | 93.33% |
| hierarchy_raw_names | 20 | 2 | 2 | 90.91% | 90.91% | 90.91% |
| file_presence_raw_names | 29 | 2 | 2 | 93.55% | 93.55% | 93.55% |
| features_aligned | 15 | 0 | 0 | 100.00% | 100.00% | 100.00% |
| hierarchy_aligned | 22 | 0 | 0 | 100.00% | 100.00% | 100.00% |
| file_presence_aligned | 31 | 0 | 0 | 100.00% | 100.00% | 100.00% |
| explicit_file_mappings_aligned | 10 | 0 | 0 | 100.00% | 100.00% | 100.00% |
| fragment_file_presence_aligned | 21 | 0 | 0 | 100.00% | 100.00% | 100.00% |
| folder_mappings | N/A | N/A | N/A | N/A | N/A | N/A |
| fragments_raw_names | 101 | 8 | 7 | 92.66% | 93.52% | 93.09% |
| fragments_feature_aligned_exact | 107 | 2 | 1 | 98.17% | 99.07% | 98.62% |
| fragments_reviewed_role_alignment | 108 | 1 | 0 | 99.08% | 100.00% | 99.54% |
| focus_fragments_reviewed_role_alignment | 8 | 0 | 0 | 100.00% | 100.00% | 100.00% |

## Definitions and scope

- Precision = TP/(TP+FP); recall = TP/(TP+FN); F1 = 2TP/(2TP+FP+FN). Undefined values are blank in CSV and N/A here. Empty sets are not assigned an invented 100% score.
- Feature presence and hierarchy compare feature names and strict ancestor-descendant pairs. Raw-name and aligned-name results are both retained.
- Feature-name correspondences: {"TaskPriority": "TaskPriorityLevels"}. Only documented one-to-one feature correspondences are applied; unrelated/retained features are not collapsed to improve scores.
- File presence compares distinct (file, feature) pairs after combining explicit file mappings, recursive folder expansion when present, and fragment-to-file projections. Explicit mapping scores remain separate to expose representation differences. Paths are relative to project/; different physical file layouts can affect these scores.
- Fragment scores measure explicit inline coverage only. Whole-file/folder mappings are not expanded into statement coverage. Fields are file, context, normalized statement, feature. Repeated independent associations are retained with one-to-one matching.
- Exact matching uses statement/context text after Java parser formatting and whitespace normalization outside literals. CSS units are property declarations. Comma-separated shared feature markers are expanded in comments only; code and string literals are preserved.
- Reviewed role alignment adds explicitly inspected one-to-one code-role correspondences. Feature labels must still agree after the documented aliases. A reviewed role with differing labels stays FP/FN, with a cross-reference in comparison.csv. No fuzzy matching or unstated automatic variable/method renaming is used.
- Role alignment is an inspectable judgment, not proof of identical behavior. Some rewrites split/merge code units; unpaired units remain unmatched rather than receiving invented many-to-one credit. File and fragment scores are overlap against a potentially sparse reference; FP/FN are not all demonstrated annotation defects.
- Focus fragment scope: TaskPriorityLevels, TaskOrdering. The two post-split priority-level and ordering responsibilities.
- Source is read from pinned commits, excluding local settings, prompts, tools, and setup files. Original ground truth is copied unchanged. No source edits, staging, commits, or pushes are performed by this evaluator.
- This evaluation does not independently establish runtime implementation correctness, mandatory/optional constraints, or a quantitative domain-validity score.

## Findings and review notes

- Generated TaskPriority is aligned to TaskPriorityLevels: the remaining feature covers priority editing/default priority, while ordering has its own TaskOrdering feature. The pre-split name alone is not treated as a new feature.
- Scenario metadata names PriorityOrdering, but the authoritative post-step ground-truth model names TaskOrdering; comparison uses the model.
- The only remaining unmatched generated fragment after reviewed alignment is an import annotated RenameTasks. Imports are retained because the existing ground-truth normalizer includes explicitly annotated declarations.

## Reviewed statement correspondences

R/G IDs are 1-based data-row numbers in the copied reference/generated fragment CSVs; they are not source line numbers.

| Reference | Generated | Feature labels agree | Rationale |
| --- | --- | --- | --- |
| R016 | G016 | yes | Install task-order comparator: static factory becomes Comparator implementation instantiated with new TaskOrdering(). |

## Reproduction and files

Run from this branch's worktree root: python scripts/evaluate_step_009.py

Reference copies: ../../ground_truth/step-009/. Generated CSVs: ../../generated/step-009/. metrics.csv contains counts, ratios, and applicability. comparison.csv contains the complete TP/FP/FN evidence.

## Input hashes

- Reference file_mappings.csv SHA-256: 19def5fe6152107a59d1df0e313214aab2fc8ff6a0ee7083d5ae1813c7a0cdc6
- Reference fragments.csv SHA-256: 6f2da378ce831108500c1d2b9f2a67caa420e7b2163c4a3eb27d5a5e5eec0f06
- Generated fragments.csv SHA-256: e159afb53c209b93da20ea732868e1d191fb35ed3d12cb3c420f5e3e71febb3a
