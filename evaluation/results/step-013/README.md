# Evaluation: eval-v2-step-013-run-01

Generated commit: 2a6af2eae83ba4afa21955e1dd54964e22182528. Reference CSVs/normalizer: 693cc1e8b57f176f037bac0ab54c9b01de50851c. Reference source: a56735e20074e620e288a80263031b483dce861c.

Reference: 15 explicit file mappings, 169 fragments. Generated: 14 explicit/expanded file mappings, 206 fragments.

## Scores

| Metric | TP | FP | FN | Precision | Recall | F1 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| features_raw_names | 23 | 2 | 0 | 92.00% | 100.00% | 95.83% |
| hierarchy_raw_names | 44 | 5 | 0 | 89.80% | 100.00% | 94.62% |
| file_presence_raw_names | 45 | 6 | 7 | 88.24% | 86.54% | 87.38% |
| features_aligned | 23 | 2 | 0 | 92.00% | 100.00% | 95.83% |
| hierarchy_aligned | 44 | 5 | 0 | 89.80% | 100.00% | 94.62% |
| file_presence_aligned | 45 | 6 | 7 | 88.24% | 86.54% | 87.38% |
| explicit_file_mappings_aligned | 13 | 1 | 2 | 92.86% | 86.67% | 89.66% |
| fragment_file_presence_aligned | 34 | 5 | 7 | 87.18% | 82.93% | 85.00% |
| folder_mappings | N/A | N/A | N/A | N/A | N/A | N/A |
| fragments_raw_names | 138 | 68 | 31 | 66.99% | 81.66% | 73.60% |
| fragments_feature_aligned_exact | 138 | 68 | 31 | 66.99% | 81.66% | 73.60% |
| fragments_reviewed_role_alignment | 149 | 57 | 20 | 72.33% | 88.17% | 79.47% |
| focus_fragments_reviewed_role_alignment | 0 | 0 | 0 | N/A | N/A | N/A |
| removed_feature_references | N/A | N/A | N/A | N/A | N/A | N/A |

## Definitions and scope

- Precision = TP/(TP+FP); recall = TP/(TP+FN); F1 = 2TP/(2TP+FP+FN). Undefined values are blank in CSV and N/A here. Empty sets are not assigned an invented 100% score.
- Feature presence and hierarchy compare feature names and strict ancestor-descendant pairs. Raw-name and aligned-name results are both retained.
- Feature-name correspondences: none. Only documented one-to-one feature correspondences are applied; unrelated/retained features are not collapsed to improve scores.
- File presence compares distinct (file, feature) pairs after combining explicit file mappings, recursive folder expansion when present, and fragment-to-file projections. Explicit mapping scores remain separate to expose representation differences. Paths are relative to project/; different physical file layouts can affect these scores.
- Fragment scores measure explicit inline coverage only. Whole-file/folder mappings are not expanded into statement coverage. Fields are file, context, normalized statement, feature. Repeated independent associations are retained with one-to-one matching.
- Exact matching uses statement/context text after Java parser formatting and whitespace normalization outside literals. CSS units are property declarations. Comma-separated shared feature markers are expanded in comments only; code and string literals are preserved.
- Reviewed role alignment adds explicitly inspected one-to-one code-role correspondences. Feature labels must still agree after the documented aliases. A reviewed role with differing labels stays FP/FN, with a cross-reference in comparison.csv. No fuzzy matching or unstated automatic variable/method renaming is used.
- Role alignment is an inspectable judgment, not proof of identical behavior. Some rewrites split/merge code units; unpaired units remain unmatched rather than receiving invented many-to-one credit. File and fragment scores are overlap against a potentially sparse reference; FP/FN are not all demonstrated annotation defects.
- Focus fragment scope: ArchiveTasks, ShowArchivedTasks. This is a removal scenario: expected post-step archive associations are empty. When generated references are also empty, precision/recall/F1 are undefined; removal counts are reported separately.
- Source is read from pinned commits, excluding local settings, prompts, tools, and setup files. Original ground truth is copied unchanged. No source edits, staging, commits, or pushes are performed by this evaluator.
- This evaluation does not independently establish runtime implementation correctness, mandatory/optional constraints, or a quantitative domain-validity score.

## Findings and review notes

- TaskDetails and EditTaskDescription are additional generated feature identities. The step prompts request a details window and description editing, so these names are domain-supported candidates even though absent from the curated model.
- The generated model and annotations contain no ArchiveTasks or ShowArchivedTasks references. Compatibility parsing of legacy archived JSON is intentionally retained and is not counted as a stale annotation.
- Both reference and generated JsonFileStore contain an unmatched end[PersistSubtasks] marker. The existing normalization policy ignores that orphan without inventing a start; remaining valid scopes are preserved.
- TaskDetailsController refactors show/saveTask/rebuildSubtasks into render and inline persistence. Some missing explicit inline pairs are covered at broader file ownership granularity; fragment scores deliberately remain separate from effective whole-file coverage.
- Four reviewed role pairs keep differing feature labels and remain FP/FN. The subtask entry field retains Subtasks coverage but lacks the separate reference AddSubtasks association; its existing Subtasks pair is already matched. Other added details/description annotations and CSS roles are unsupported by this reference model rather than proven incorrect.
- Extractor: src/main/java/repository/JsonFileStore.java: Unmatched &end[PersistSubtasks] at source line 68; ignored marker, no missing start inferred.

## Reviewed statement correspondences

R/G IDs are 1-based data-row numbers in the copied reference/generated fragment CSVs; they are not source line numbers.

| Reference | Generated | Feature labels agree | Rationale |
| --- | --- | --- | --- |
| R039 | G038 | yes | Task title editor moves from show to render method; variable renamed. |
| R040 | G052 | yes | Priority selector moves from show to render; variable renamed. |
| R041 | G056 | yes | Due-date editor moves from show to render; variable renamed. |
| R042 | G068 | yes | Assigned-tag selector moves from show to render; variable renamed. |
| R043 | G077 | yes | Delete-task action button moves from show to render; caption differs. |
| R044 | G080 | yes | Subtask section container now created by section helper. |
| R049 | G084 | yes | Add-subtask callback saves and rerenders instead of saveTask/rebuildSubtasks helpers. |
| R052 | G090 | yes | Insert subtask entry form; inline HBox construction becomes named form inserted later. |
| R053 | G089 | yes | Render subtask editor rows; callbacks persist/rerender through different helpers. |
| R064 | G092 | yes | Render priority label; enum.toString becomes String.valueOf. |
| R066 | G099 | yes | Render assigned tag chips; container renamed and chip helper becomes static. |
| R068 | G103 | no: stays FP/FN | Open task details on click; generated TaskDetails differs from expected TaskManagement, so no TP credit. |
| R162 | G202 | no: stays FP/FN | Details heading font-size role; selector/value differ and feature changes TaskManagement to TaskDetails. |
| R163 | G203 | no: stays FP/FN | Details heading font-weight role; feature changes TaskManagement to TaskDetails. |
| R164 | G204 | no: stays FP/FN | Details heading text-color role; feature changes TaskManagement to TaskDetails. |

## Reproduction and files

Run from this branch's worktree root: python scripts/evaluate_step_013.py

Reference copies: ../../ground_truth/step-013/. Generated CSVs: ../../generated/step-013/. metrics.csv contains counts, ratios, and applicability. comparison.csv contains the complete TP/FP/FN evidence.

## Input hashes

- Reference file_mappings.csv SHA-256: ebb4574a7f58a2523490cc5ab68c46054d470e52078b2234ff0346581d601ea7
- Reference fragments.csv SHA-256: 1a593de7b39beb87c6d8395c8436619a5d211057ae3c06f29db792d27e283e46
- Generated fragments.csv SHA-256: b27e130dba3473463dbcdc6bcb1c3d71ec819936a3175ad7e6bb2277cf3bec5e
