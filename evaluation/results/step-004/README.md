# Evaluation: eval-v2-step-004-run-01

Generated commit: 8ff51d710c775a7d51ca8de7c2da27941dc0c2ef. Reference CSVs/normalizer: 693cc1e8b57f176f037bac0ab54c9b01de50851c. Reference source: 3572bcffc34ae402c12abb50000b8f1fff5d8224.

Reference: 2 explicit file mappings, 82 fragments. Generated: 3 explicit/expanded file mappings, 100 fragments.

## Scores

| Metric | TP | FP | FN | Precision | Recall | F1 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| features_raw_names | 12 | 1 | 0 | 92.31% | 100.00% | 96.00% |
| hierarchy_raw_names | 20 | 2 | 0 | 90.91% | 100.00% | 95.24% |
| file_presence_raw_names | 20 | 5 | 0 | 80.00% | 100.00% | 88.89% |
| features_aligned | 12 | 1 | 0 | 92.31% | 100.00% | 96.00% |
| hierarchy_aligned | 20 | 2 | 0 | 90.91% | 100.00% | 95.24% |
| file_presence_aligned | 20 | 5 | 0 | 80.00% | 100.00% | 88.89% |
| explicit_file_mappings_aligned | 2 | 1 | 0 | 66.67% | 100.00% | 80.00% |
| fragment_file_presence_aligned | 18 | 4 | 0 | 81.82% | 100.00% | 90.00% |
| folder_mappings | N/A | N/A | N/A | N/A | N/A | N/A |
| fragments_raw_names | 70 | 30 | 12 | 70.00% | 85.37% | 76.92% |
| fragments_feature_aligned_exact | 70 | 30 | 12 | 70.00% | 85.37% | 76.92% |
| fragments_reviewed_role_alignment | 79 | 21 | 3 | 79.00% | 96.34% | 86.81% |
| focus_fragments_reviewed_role_alignment | 6 | 17 | 3 | 26.09% | 66.67% | 37.50% |

## Definitions and scope

- Precision = TP/(TP+FP); recall = TP/(TP+FN); F1 = 2TP/(2TP+FP+FN). Undefined values are blank in CSV and N/A here. Empty sets are not assigned an invented 100% score.
- Feature presence and hierarchy compare feature names and strict ancestor-descendant pairs. Raw-name and aligned-name results are both retained.
- Feature-name correspondences: none. Only documented one-to-one feature correspondences are applied; unrelated/retained features are not collapsed to improve scores.
- File presence compares distinct (file, feature) pairs after combining explicit file mappings, recursive folder expansion when present, and fragment-to-file projections. Explicit mapping scores remain separate to expose representation differences. Paths are relative to project/; different physical file layouts can affect these scores.
- Fragment scores measure explicit inline coverage only. Whole-file/folder mappings are not expanded into statement coverage. Fields are file, context, normalized statement, feature. Repeated independent associations are retained with one-to-one matching.
- Exact matching uses statement/context text after Java parser formatting and whitespace normalization outside literals. CSS units are property declarations. Comma-separated shared feature markers are expanded in comments only; code and string literals are preserved.
- Reviewed role alignment adds explicitly inspected one-to-one code-role correspondences. Feature labels must still agree after the documented aliases. A reviewed role with differing labels stays FP/FN, with a cross-reference in comparison.csv. No fuzzy matching or unstated automatic variable/method renaming is used.
- Role alignment is an inspectable judgment, not proof of identical behavior. Some rewrites split/merge code units; unpaired units remain unmatched rather than receiving invented many-to-one credit. File and fragment scores are overlap against a potentially sparse reference; FP/FN are not all demonstrated annotation defects.
- Focus fragment scope: TaskPriority, PrioritySort. Priority functionality and the generated separate sorting feature; the extra name is not merged into TaskPriority.
- Source is read from pinned commits, excluding local settings, prompts, tools, and setup files. Original ground truth is copied unchanged. No source edits, staging, commits, or pushes are performed by this evaluator.
- This evaluation does not independently establish runtime implementation correctness, mandatory/optional constraints, or a quantitative domain-validity score.

## Findings and review notes

- Generated PrioritySort is an additional feature absent from the reference model. It is not an alias of TaskPriority because both features coexist in the generated model.
- The priority enum/file is renamed from TaskPriority.java to Priority.java; raw physical paths are preserved in file comparisons.
- A reference pair of sorted-list/comparator statements becomes constructor-based sorting plus a service comparator. Unpaired split/merge differences remain unmatched.
- Generated annotations also cover priority persistence, model accessors/constructors, and task-creation selectors that lack explicit inline coverage in the reference. These extras are not automatically runtime or annotation defects.

## Reviewed statement correspondences

R/G IDs are 1-based data-row numbers in the copied reference/generated fragment CSVs; they are not source line numbers.

| Reference | Generated | Feature labels agree | Rationale |
| --- | --- | --- | --- |
| R006 | G013 | yes | Task creation callback adds selected priority and resets priority input; same task creation/persistence role. |
| R036 | G043 | yes | Task creation form includes an additional priority selector. |
| R040 | G058 | yes | Priority editor declaration; ChoiceBox/TaskPriority becomes ComboBox/model.Priority. |
| R041 | G059 | yes | Populate priority selector with priority enum values; enum/control names differ. |
| R042 | G060 | yes | Set selected priority from task; variable renamed. |
| R043 | G061 | yes | Priority edit callback persists and refreshes task via observable-list replacement. |
| R053 | G056 | yes | Completion callback persists and refreshes task; observable-list replacement is inlined. |
| R075 | G090 | yes | Default-priority task creation; constructor default replaces explicit MEDIUM argument. |
| R076 | G093 | yes | Initialize task with priority; explicit optional priority overload replaces default-priority construction. |
| R018 | G096 | no: stays FP/FN | Completed-then-priority comparator moves into service method. Feature labels TaskPriority/PrioritySort differ, so this pair stays FP/FN. |

## Reproduction and files

Run from this branch's worktree root: python scripts/evaluate_step_004.py

Reference copies: ../../ground_truth/step-004/. Generated CSVs: ../../generated/step-004/. metrics.csv contains counts, ratios, and applicability. comparison.csv contains the complete TP/FP/FN evidence.

## Input hashes

- Reference file_mappings.csv SHA-256: 2ff2b5af45dd7840c0d3efe98ab6587c56843c8326c2da7f60f352e8603b510b
- Reference fragments.csv SHA-256: 634ceb1acdd6b87980826440c054f948b2e3196246bf8e40a1317f7742b8737a
- Generated fragments.csv SHA-256: 6754f7d903069608fe723e5ce2487cd2069bbed456d32a1eba61820284b22130
