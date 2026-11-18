# Evaluation: eval-v2-step-007-run-01

Generated commit: b888d00190786c5290b12f7ec0cebd7b035767c0. Reference CSVs/normalizer: 693cc1e8b57f176f037bac0ab54c9b01de50851c. Reference source: 9445b35274f51f768164d8cac360ce99838df8ed.

Reference: 9 explicit file mappings, 88 fragments. Generated: 8 explicit/expanded file mappings, 102 fragments.

## Scores

| Metric | TP | FP | FN | Precision | Recall | F1 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| features_raw_names | 12 | 2 | 0 | 85.71% | 100.00% | 92.31% |
| hierarchy_raw_names | 16 | 10 | 0 | 61.54% | 100.00% | 76.19% |
| file_presence_raw_names | 24 | 6 | 1 | 80.00% | 96.00% | 87.27% |
| features_aligned | 12 | 2 | 0 | 85.71% | 100.00% | 92.31% |
| hierarchy_aligned | 16 | 10 | 0 | 61.54% | 100.00% | 76.19% |
| file_presence_aligned | 24 | 6 | 1 | 80.00% | 96.00% | 87.27% |
| explicit_file_mappings_aligned | 8 | 0 | 1 | 100.00% | 88.89% | 94.12% |
| fragment_file_presence_aligned | 16 | 6 | 0 | 72.73% | 100.00% | 84.21% |
| folder_mappings | N/A | N/A | N/A | N/A | N/A | N/A |
| fragments_raw_names | 79 | 23 | 9 | 77.45% | 89.77% | 83.16% |
| fragments_feature_aligned_exact | 79 | 23 | 9 | 77.45% | 89.77% | 83.16% |
| fragments_reviewed_role_alignment | 80 | 22 | 8 | 78.43% | 90.91% | 84.21% |
| focus_fragments_reviewed_role_alignment | 1 | 22 | 8 | 4.35% | 11.11% | 6.25% |

## Definitions and scope

- Precision = TP/(TP+FP); recall = TP/(TP+FN); F1 = 2TP/(2TP+FP+FN). Undefined values are blank in CSV and N/A here. Empty sets are not assigned an invented 100% score.
- Feature presence and hierarchy compare feature names and strict ancestor-descendant pairs. Raw-name and aligned-name results are both retained.
- Feature-name correspondences: none. Only documented one-to-one feature correspondences are applied; unrelated/retained features are not collapsed to improve scores.
- File presence compares distinct (file, feature) pairs after combining explicit file mappings, recursive folder expansion when present, and fragment-to-file projections. Explicit mapping scores remain separate to expose representation differences. Paths are relative to project/; different physical file layouts can affect these scores.
- Fragment scores measure explicit inline coverage only. Whole-file/folder mappings are not expanded into statement coverage. Fields are file, context, normalized statement, feature. Repeated independent associations are retained with one-to-one matching.
- Exact matching uses statement/context text after Java parser formatting and whitespace normalization outside literals. CSS units are property declarations. Comma-separated shared feature markers are expanded in comments only; code and string literals are preserved.
- Reviewed role alignment adds explicitly inspected one-to-one code-role correspondences. Feature labels must still agree after the documented aliases. A reviewed role with differing labels stays FP/FN, with a cross-reference in comparison.csv. No fuzzy matching or unstated automatic variable/method renaming is used.
- Role alignment is an inspectable judgment, not proof of identical behavior. Some rewrites split/merge code units; unpaired units remain unmatched rather than receiving invented many-to-one credit. File and fragment scores are overlap against a potentially sparse reference; FP/FN are not all demonstrated annotation defects.
- Focus fragment scope: TaskQuery, KeywordSearch, StatusFilter. Expected merged TaskQuery and the two source features retained by the generated model.
- Source is read from pinned commits, excluding local settings, prompts, tools, and setup files. Original ground truth is copied unchanged. No source edits, staging, commits, or pushes are performed by this evaluator.
- This evaluation does not independently establish runtime implementation correctness, mandatory/optional constraints, or a quantitative domain-validity score.

## Findings and review notes

- KeywordSearch and StatusFilter remain children of TaskQuery in the generated model. The reference removes those identities through a merge. They are deliberately not aliased to TaskQuery, which would conceal the merge difference.
- Query behavior moves from a mapped TaskQuery.java class into annotated TaskService.createQuery. File-level path mismatch and fragment representation mismatch are implementation-dependent diagnostics.
- The generated model also retains the v006 hierarchy arrangement; ancestor-descendant scores expose those structural differences.
- Eight reviewed role pairs keep differing feature labels and therefore remain FP/FN; their code correspondence is documented without granting feature-association credit.

## Reviewed statement correspondences

R/G IDs are 1-based data-row numbers in the copied reference/generated fragment CSVs; they are not source line numbers.

| Reference | Generated | Feature labels agree | Rationale |
| --- | --- | --- | --- |
| R014 | G014 | yes | Apply combined query predicate: Runnable with TaskQuery constructor becomes listener using taskService.createQuery. |
| R008 | G008 | no: stays FP/FN | Search text-field declaration; generated KeywordSearch differs from expected merged TaskQuery, so no TP credit. |
| R009 | G009 | no: stays FP/FN | Search prompt; generated KeywordSearch differs from expected merged TaskQuery, so no TP credit. |
| R010 | G010 | no: stays FP/FN | Filtered task-list declaration; generated KeywordSearch differs from expected merged TaskQuery, so no TP credit. |
| R011 | G011 | no: stays FP/FN | Status selector declaration; generated StatusFilter differs from expected merged TaskQuery, so no TP credit. |
| R012 | G012 | no: stays FP/FN | Status selector values; constants move to service and feature remains StatusFilter rather than TaskQuery. |
| R013 | G013 | no: stays FP/FN | Select initial status value; StatusFilter remains distinct from expected TaskQuery. |
| R036 | G038 | no: stays FP/FN | Compose query UI; generated feature StatusFilter differs from merged reference TaskQuery. |
| R037 | G039 | no: stays FP/FN | Compose main view; generated KeywordSearch differs from merged reference TaskQuery. |

## Reproduction and files

Run from this branch's worktree root: python scripts/evaluate_step_007.py

Reference copies: ../../ground_truth/step-007/. Generated CSVs: ../../generated/step-007/. metrics.csv contains counts, ratios, and applicability. comparison.csv contains the complete TP/FP/FN evidence.

## Input hashes

- Reference file_mappings.csv SHA-256: 22b12a34e67fa53e0d0aff3ce1c382fd74af9c3beca987c4b5f17cdca011eea6
- Reference fragments.csv SHA-256: e018f1f5250d4349e2bd5b81ed3a5cccdf04e306c6e0d48b72776e719f9c29e6
- Generated fragments.csv SHA-256: bbb143d0d918f363e4b8fddde4f532e901aa0a6050f08e4b7264d4aac0c781cc
