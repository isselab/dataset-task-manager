# Evaluation: eval-v2-step-001-run-01

Generated commit: 30615059c1d2d3dfe37ebe83c83c7f2cc87c3a7f. Reference CSVs/normalizer: 693cc1e8b57f176f037bac0ab54c9b01de50851c. Reference source: c296e608ebd6cc1d897f0ec6106d5ecb4dea4931.

Reference: 2 explicit file mappings, 11 fragments. Generated: 3 explicit/expanded file mappings, 80 fragments.

## Scores

| Metric | TP | FP | FN | Precision | Recall | F1 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| features_raw_names | 2 | 5 | 5 | 28.57% | 28.57% | 28.57% |
| hierarchy_raw_names | 1 | 8 | 9 | 11.11% | 10.00% | 10.53% |
| file_presence_raw_names | 0 | 16 | 6 | 0.00% | 0.00% | 0.00% |
| features_aligned | 6 | 1 | 1 | 85.71% | 85.71% | 85.71% |
| hierarchy_aligned | 8 | 1 | 2 | 88.89% | 80.00% | 84.21% |
| file_presence_aligned | 3 | 13 | 3 | 18.75% | 50.00% | 27.27% |
| explicit_file_mappings_aligned | 0 | 3 | 2 | 0.00% | 0.00% | 0.00% |
| fragment_file_presence_aligned | 3 | 10 | 1 | 23.08% | 75.00% | 35.29% |
| folder_mappings | N/A | N/A | N/A | N/A | N/A | N/A |
| fragments_raw_names | 0 | 80 | 11 | 0.00% | 0.00% | 0.00% |
| fragments_feature_aligned_exact | 0 | 80 | 11 | 0.00% | 0.00% | 0.00% |
| fragments_reviewed_role_alignment | 7 | 73 | 4 | 8.75% | 63.64% | 15.38% |
| focus_fragments_reviewed_role_alignment | 4 | 44 | 2 | 8.33% | 66.67% | 14.81% |

## Definitions and scope

- Precision = TP/(TP+FP); recall = TP/(TP+FN); F1 = 2TP/(2TP+FP+FN). Undefined values are blank in CSV and N/A here. Empty sets are not assigned an invented 100% score.
- Feature presence and hierarchy compare feature names and strict ancestor-descendant pairs. Raw-name and aligned-name results are both retained.
- Feature-name correspondences: {"AddTask": "CreateTasks", "AssignLabel": "AssignTaskLabels", "CreateLabel": "CreateLabels", "Tasks": "TaskManagement"}. Only documented one-to-one feature correspondences are applied; unrelated/retained features are not collapsed to improve scores.
- File presence compares distinct (file, feature) pairs after combining explicit file mappings, recursive folder expansion when present, and fragment-to-file projections. Explicit mapping scores remain separate to expose representation differences. Paths are relative to project/; different physical file layouts can affect these scores.
- Fragment scores measure explicit inline coverage only. Whole-file/folder mappings are not expanded into statement coverage. Fields are file, context, normalized statement, feature. Repeated independent associations are retained with one-to-one matching.
- Exact matching uses statement/context text after Java parser formatting and whitespace normalization outside literals. CSS units are property declarations. Comma-separated shared feature markers are expanded in comments only; code and string literals are preserved.
- Reviewed role alignment adds explicitly inspected one-to-one code-role correspondences. Feature labels must still agree after the documented aliases. A reviewed role with differing labels stays FP/FN, with a cross-reference in comparison.csv. No fuzzy matching or unstated automatic variable/method renaming is used.
- Role alignment is an inspectable judgment, not proof of identical behavior. Some rewrites split/merge code units; unpaired units remain unmatched rather than receiving invented many-to-one credit. File and fragment scores are overlap against a potentially sparse reference; FP/FN are not all demonstrated annotation defects.
- Focus fragment scope: Labels, CreateLabels, AssignTaskLabels, PersistLabels, Persistence. Label-creation/assignment/persistence responsibilities requested by step-001; broad unmatched Persistence remains included.
- Source is read from pinned commits, excluding local settings, prompts, tools, and setup files. Original ground truth is copied unchanged. No source edits, staging, commits, or pushes are performed by this evaluator.
- This evaluation does not independently establish runtime implementation correctness, mandatory/optional constraints, or a quantitative domain-validity score.

## Findings and review notes

- The implementation substantially rewrites services, dialogs, persistence, and UI; exact fragment overlap is zero despite role correspondences.
- Aliases Tasks->TaskManagement, AddTask->CreateTasks, CreateLabel->CreateLabels, and AssignLabel->AssignTaskLabels reflect the implemented responsibilities and hierarchy.
- Persistence is deliberately NOT aliased to PersistLabels: it covers both task and label persistence, whereas the reference uses the narrower label feature. This remains a feature identity/abstraction difference.
- The reference has only 11 inline associations and two file mappings. Many generated inline relationships have no annotated reference counterpart. Low fragment precision is not proof all extra annotations are wrong.
- Reference name normalization/duplicate checks and CSS task-title annotations have no one-to-one annotated generated counterparts. UI validation moved into larger callbacks; no invented many-to-one credit is awarded.

## Reviewed statement correspondences

R/G IDs are 1-based data-row numbers in the copied reference/generated fragment CSVs; they are not source line numbers.

| Reference | Generated | Feature labels agree | Rationale |
| --- | --- | --- | --- |
| R003 | G004 | yes | Create label object; method renamed, trimming occurs in UI rather than this service statement. |
| R004 | G005 | yes | Insert created label into observable collection; method renamed. |
| R005 | G006 | yes | Return created label; method renamed. |
| R006 | G077 | yes | Create task object; method renamed, title trimming delegated to dialog. |
| R007 | G078 | yes | Insert created task into collection; method renamed. |
| R008 | G079 | yes | Return created task; method renamed. |
| R009 | G080 | yes | Assign or clear task label ID; method/accessor names differ. |

## Reproduction and files

Run from this branch's worktree root: python scripts/evaluate_step_001.py

Reference copies: ../../ground_truth/step-001/. Generated CSVs: ../../generated/step-001/. metrics.csv contains counts, ratios, and applicability. comparison.csv contains the complete TP/FP/FN evidence.

## Input hashes

- Reference file_mappings.csv SHA-256: f0c4cd7f7d6e0d8b1b6e317a5413075b042741caa81eed0d06f6c9deeaf64878
- Reference fragments.csv SHA-256: f4ef09608d1d5e4507ef7242432698c0eb4061ececfb75d1ea491e3f92c982db
- Generated fragments.csv SHA-256: 2f3e21156e472bb4ea1732a1e914713ac69c6340f2ab57ef2ac9a3ac543af71f
