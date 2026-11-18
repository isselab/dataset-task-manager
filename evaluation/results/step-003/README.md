# Evaluation: eval-v2-step-003-run-03

Generated commit: 244b7444a6386f2440665f1d4644a53436b8de15. Reference CSVs/normalizer: 693cc1e8b57f176f037bac0ab54c9b01de50851c. Reference source: d434a9f2080770af36a8f62553c9fe4d1a8c52ac.

Reference: 2 explicit file mappings, 74 fragments. Generated: 2 explicit/expanded file mappings, 83 fragments.

## Scores

| Metric | TP | FP | FN | Precision | Recall | F1 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| features_raw_names | 11 | 1 | 0 | 91.67% | 100.00% | 95.65% |
| hierarchy_raw_names | 18 | 2 | 0 | 90.00% | 100.00% | 94.74% |
| file_presence_raw_names | 16 | 7 | 0 | 69.57% | 100.00% | 82.05% |
| features_aligned | 11 | 1 | 0 | 91.67% | 100.00% | 95.65% |
| hierarchy_aligned | 18 | 2 | 0 | 90.00% | 100.00% | 94.74% |
| file_presence_aligned | 16 | 7 | 0 | 69.57% | 100.00% | 82.05% |
| explicit_file_mappings_aligned | 2 | 0 | 0 | 100.00% | 100.00% | 100.00% |
| fragment_file_presence_aligned | 14 | 7 | 0 | 66.67% | 100.00% | 80.00% |
| folder_mappings | N/A | N/A | N/A | N/A | N/A | N/A |
| fragments_raw_names | 37 | 46 | 37 | 44.58% | 50.00% | 47.13% |
| fragments_feature_aligned_exact | 37 | 46 | 37 | 44.58% | 50.00% | 47.13% |
| fragments_reviewed_role_alignment | 58 | 25 | 16 | 69.88% | 78.38% | 73.89% |
| focus_fragments_reviewed_role_alignment | 30 | 25 | 16 | 54.55% | 65.22% | 59.41% |

## Definitions and scope

- Precision = TP/(TP+FP); recall = TP/(TP+FN); F1 = 2TP/(2TP+FP+FN). Undefined values are blank in CSV and N/A here. Empty sets are not assigned an invented 100% score.
- Feature presence and hierarchy compare feature names and strict ancestor-descendant pairs. Raw-name and aligned-name results are both retained.
- Feature-name correspondences: none. Only documented one-to-one feature correspondences are applied; unrelated/retained features are not collapsed to improve scores.
- File presence compares distinct (file, feature) pairs after combining explicit file mappings, recursive folder expansion when present, and fragment-to-file projections. Explicit mapping scores remain separate to expose representation differences. Paths are relative to project/; different physical file layouts can affect these scores.
- Fragment scores measure explicit inline coverage only. Whole-file/folder mappings are not expanded into statement coverage. Fields are file, context, normalized statement, feature. Repeated independent associations are retained with one-to-one matching.
- Exact matching uses statement/context text after Java parser formatting and whitespace normalization outside literals. CSS units are property declarations. Comma-separated shared feature markers are expanded in comments only; code and string literals are preserved.
- Reviewed role alignment adds explicitly inspected one-to-one code-role correspondences. Feature labels must still agree after the documented aliases. A reviewed role with differing labels stays FP/FN, with a cross-reference in comparison.csv. No fuzzy matching or unstated automatic variable/method renaming is used.
- Role alignment is an inspectable judgment, not proof of identical behavior. Some rewrites split/merge code units; unpaired units remain unmatched rather than receiving invented many-to-one credit. File and fragment scores are overlap against a potentially sparse reference; FP/FN are not all demonstrated annotation defects.
- Focus fragment scope: Labels, CreateLabels, RenameLabels, DeleteLabels, AssignTaskLabels, PersistLabels, LabelColors. Modified label assignment/color behavior and new label rename/delete features.
- Source is read from pinned commits, excluding local settings, prompts, tools, and setup files. Original ground truth is copied unchanged. No source edits, staging, commits, or pushes are performed by this evaluator.
- This evaluation does not independently establish runtime implementation correctness, mandatory/optional constraints, or a quantitative domain-validity score.

## Findings and review notes

- LabelColors is an additional generated feature absent from the reference model. Color behavior is requested by the prompt, so this difference alone does not demonstrate a hallucinated feature.
- The implementation replaces label selector cell factories with a converter and task label ChoiceBox with per-label menu toggles. Some responsibilities split/merge across different statements and are left unmatched conservatively.
- Scenario metadata names RemoveLabels; the authoritative reference model and annotations name DeleteLabels. Scores follow the latter.
- Generated RenameLabels/DeleteLabels service methods have different signatures/return contracts. Reviewed alignments identify corresponding implementation roles, not proof of identical null/error behavior.

## Reviewed statement correspondences

R/G IDs are 1-based data-row numbers in the copied reference/generated fragment CSVs; they are not source line numbers.

| Reference | Generated | Feature labels agree | Rationale |
| --- | --- | --- | --- |
| R020 | G019 | yes | Create label color picker; default literal becomes model constant and Color import. |
| R022 | G022 | yes | Create-label callback passes selected color; conversion becomes explicit hexadecimal formatting. |
| R029 | G026 | yes | Prompt for replacement label name; wording differs. |
| R030 | G027 | yes | Create rename-label button; variable and caption differ. |
| R031 | G028 | yes | Rename selected label, persist, and refresh; service returns replacement object rather than boolean. |
| R033 | G029 | yes | Create delete-label button; variable and caption differ. |
| R034 | G030 | yes | Delete selected label, unlink task references, persist, and refresh; cleanup is delegated to task service. |
| R036 | G034 | yes | Compose label-management controls; variables renamed and shared with DeleteLabels. |
| R038 | G036 | yes | Compose main view including label management; container variable renamed. |
| R039 | G038 | yes | Label selection control: ChoiceBox becomes multi-toggle MenuButton. Same selection role, different interaction structure. |
| R043 | G039 | yes | Container for assigned label chips; variable and spacing differ. |
| R053 | G064 | yes | Construct colored label; generated expression additionally handles blank/null color defaults. |
| R057 | G068 | yes | Normalize requested label name; method signature changes. |
| R058 | G070 | yes | Validate replacement name uniqueness/nonempty; generated guard also checks label index. |
| R059 | G071 | yes | Create replacement label with existing identity and color; method signature changes. |
| R060 | G069 | yes | Find label collection index; method signature changes. |
| R062 | G072 | yes | Replace label in observable collection; method signature changes. |
| R063 | G073 | yes | Return rename success result; boolean contract becomes replacement Label contract. |
| R064 | G074 | yes | Remove label and report deletion result; guard and boolean return are combined. |
| R065 | G081 | yes | Remove deleted label IDs from tasks; moved from label service to task service. |
| R068 | G076 | yes | Create task without labels; null representation becomes empty list. |

## Reproduction and files

Run from this branch's worktree root: python scripts/evaluate_step_003.py

Reference copies: ../../ground_truth/step-003/. Generated CSVs: ../../generated/step-003/. metrics.csv contains counts, ratios, and applicability. comparison.csv contains the complete TP/FP/FN evidence.

## Input hashes

- Reference file_mappings.csv SHA-256: 2ff2b5af45dd7840c0d3efe98ab6587c56843c8326c2da7f60f352e8603b510b
- Reference fragments.csv SHA-256: 02f071e09cd539e4f8d3133f064d8d97ff17e4943e3ee4dfba74aa0fa72eb410
- Generated fragments.csv SHA-256: 3a861ff7b79f56146f6df284de8b41cc99135a020800c4e01da6ce36578368a7
