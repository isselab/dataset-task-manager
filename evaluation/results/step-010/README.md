# Step-010 evaluation: eval-v2-step-010-run-01

Generated branch: eval-v2-step-010-run-01; evaluated source commit: 7a0222bd130b7f6fbb94b7c102fb976599b31f7e.
Reference CSVs and normalizer: 693cc1e8b57f176f037bac0ab54c9b01de50851c; reference source v010: 45dcb46407d1cda4237e245464ce7b16e820b41e.

Generated implementation was normalized using the same Java/CSS extractor as ground truth. There are 10 explicit generated file mappings and 138 generated fragment associations, versus 0 explicit reference file mappings and 125 reference fragments. No folder mappings exist.

## Results

| Metric | TP | FP | FN | Precision | Recall | F1 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| features_raw_names | 15 | 1 | 1 | 93.75% | 93.75% | 93.75% |
| hierarchy_raw_names | 22 | 2 | 2 | 91.67% | 91.67% | 91.67% |
| file_presence_raw_names | 21 | 16 | 3 | 56.76% | 87.50% | 68.85% |
| features_aligned | 16 | 0 | 0 | 100.00% | 100.00% | 100.00% |
| hierarchy_aligned | 24 | 0 | 0 | 100.00% | 100.00% | 100.00% |
| file_presence_aligned | 24 | 13 | 0 | 64.86% | 100.00% | 78.69% |
| fragment_file_presence_aligned | 24 | 3 | 0 | 88.89% | 100.00% | 94.12% |
| explicit_file_mappings | N/A | N/A | N/A | N/A | N/A | N/A |
| folder_mappings | N/A | N/A | N/A | N/A | N/A | N/A |
| fragments_raw_names | 106 | 32 | 19 | 76.81% | 84.80% | 80.61% |
| fragments_feature_aligned_exact | 109 | 29 | 16 | 78.99% | 87.20% | 82.89% |
| fragments_reviewed_role_alignment | 122 | 16 | 3 | 88.41% | 97.60% | 92.78% |
| due_dates_fragments_reviewed_role_alignment | 15 | 15 | 3 | 50.00% | 83.33% | 62.50% |

## Interpretation and limits

- Precision = TP/(TP+FP), recall = TP/(TP+FN), F1 = 2TP/(2TP+FP+FN). Undefined scores are blank in CSV and N/A here; no fabricated 0% or 100%.
- Raw-name scores preserve generated names. Aligned scores use the explicit correspondence DueDates -> TaskDueDates. Both models place this feature below TaskManagement and its implementation covers task dates, overdue display, persistence, and ordering. No other name/hierarchy correspondence is introduced.
- File presence unions explicit file mappings, expanded folder mappings, and fragment-to-file projections. Comparison is on distinct (file, feature) pairs; annotation syntax/granularity differences do not cause mismatches at this level.
- Step-010's reference explicit mapping CSV is empty although v010 source has mappings. It remains unchanged. Explicit mapping correctness is not evaluable until this oracle gap is resolved. File-presence scores are partial-reference diagnostics: unmatched mappings can be valid relationships missing from the reference.
- Fragment exact scores use (file, context, normalized statement, feature) and preserve repeated associations with one-to-one matching. Formatting is normalized; code rewrites are not automatically equated by fuzzy string similarity.
- Reviewed role alignment adds 13 manually inspected one-to-one correspondences after 109 exact feature-aligned matches. These cover callback refactoring, renamed overdue UI variables/selectors, UI composition, serialization/parsing roles, and CSS property roles. Every pair and rationale is in comparison.csv. This is an inspectable role-level judgment, not a proof of behavioral equivalence.
- The resulting 16 unmatched generated associations and 3 unmatched reference associations are counted FP/FN only under the supplied closed-reference overlap metric. These are not all demonstrated annotation errors: generated code includes additional dates/ordering/service annotations, whereas reference getters/setters and ordering implementation lack inline due-date coverage. Implementation differences and reference incompleteness affect these scores.
- The three remaining reference rows R059-R061 describe a dedicated clear-date button absent from the generated implementation. The prompt does not explicitly require that button. A missing reference-specific control is not by itself proof of incorrect feature traceability.
- Due-date-only results are separate because unchanged inherited annotations inflate the all-features result. Generated overdue logic additionally excludes completed tasks, and date persistence uses a different field order/null representation. These behavioral differences have not been validated by runtime tests.
- CSS alignments compare the roles of properties on an overdue badge; colors, dimensions, selector names, and exact visual appearance differ.
- Feature presence and ancestor-descendant hierarchy scores assess feature identity/structure only. No mandatory/optional/cross-tree constraint or independent domain-validity score is claimed.

## Files and reproduction

- ../../ground_truth/step-010/: exact reference CSV copies from the pinned main commit.
- ../../generated/step-010/: generated file_mappings.csv and fragments.csv.
- metrics.csv: scores as ratios in [0,1], counts, applicability, and limits.
- comparison.csv: TP/FP/FN evidence, including reviewed statement correspondences. R/G IDs are 1-based CSV data-row numbers, not source line numbers.

From the evaluation worktree root: python scripts/evaluate_step_010.py

This script reads pinned Git objects and writes only evaluation outputs. It does not change source, stage files, commit, or push. Uncommitted local agent settings are outside evaluation scope.

## Input hashes

- Reference file_mappings.csv SHA-256: eb2836016aa8682434dc5a74b9eaa43463ac64286e7290834c992ed6e1f786e9
- Reference fragments.csv SHA-256: 511e4954b69f7260a57c8f93d37f4571c18cd51656c52fb1fb2a0513d68224b8
