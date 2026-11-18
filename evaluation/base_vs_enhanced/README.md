# Base versus enhanced agent: matched-step comparison

Comparison uses the same five steps: 001, 003, 004, 009, and 013. Base runs are run-01. Enhanced runs are the already selected v2 runs: 001/run-01, 003/run-03, 004/run-01, 009/run-02, and 013/run-01.

The [original enhanced averages](../averages/README.md) include step-007 (six runs) and are preserved unchanged. They should not be directly subtracted from the five-run base mean. Here enhanced averages are recalculated over the five shared steps.

## Macro comparison: five shared steps

| Metric | Base P | Base R | Base F1 | Enhanced P | Enhanced R | Enhanced F1 | F1 change (pp) |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| features_aligned | 90.07% | 95.81% | 92.61% | 92.34% | 97.14% | 94.64% | +2.03 |
| hierarchy_aligned | 88.27% | 94.18% | 90.83% | 91.92% | 96.00% | 93.76% | +2.93 |
| file_presence_aligned | 65.35% | 73.71% | 68.76% | 71.31% | 87.31% | 77.12% | +8.36 |
| fragments_feature_aligned_exact | 53.70% | 61.30% | 57.06% | 55.95% | 63.22% | 59.25% | +2.19 |
| fragments_reviewed_role_alignment | 65.80% | 86.38% | 71.53% | 65.81% | 85.30% | 71.02% | -0.51 |
| focus_fragments_reviewed_role_alignment | 42.84% | 78.89% | 50.82% | 47.24% | 74.64% | 52.93% | +2.11 |

## Micro comparison: five shared steps

| Metric | Base P | Base R | Base F1 | Enhanced P | Enhanced R | Enhanced F1 | F1 change (pp) |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| features_aligned | 91.67% | 97.06% | 94.29% | 93.06% | 98.53% | 95.71% | +1.43 |
| hierarchy_aligned | 90.16% | 96.49% | 93.22% | 91.80% | 98.25% | 94.92% | +1.69 |
| file_presence_aligned | 72.22% | 83.20% | 77.32% | 78.77% | 92.00% | 84.87% | +7.55 |
| fragments_feature_aligned_exact | 59.82% | 76.80% | 67.26% | 60.90% | 79.28% | 68.88% | +1.63 |
| fragments_reviewed_role_alignment | 71.05% | 91.22% | 79.88% | 69.38% | 90.32% | 78.47% | -1.41 |
| focus_fragments_reviewed_role_alignment | 40.46% | 76.81% | 53.00% | 35.82% | 69.57% | 47.29% | -5.71 |

## Interpretation

- Delta is enhanced minus base, in percentage points. Positive means a higher enhanced score; it is not a relative percentage change.
- Macro P/R/F1 are separate means of branch scores; micro P/R/F1 pool TP/FP/FN. No mixed overall score is calculated across dimensions.
- Focus definitions are checked to be identical for paired steps. Step-013 removal focus is undefined in both variants; focus macro averages therefore have four contributors. Folder scores remain N/A and deletion-reference checks are not averaged as accuracy.
- The two ground-truth CSVs for every shared step and the normalization script were verified byte-identical between main references 02cc7c313aee0e2a5607ead65b4366e726ef82cd (base) and 693cc1e8b57f176f037bac0ab54c9b01de50851c (enhanced). Naming aliases, code-role correspondences, and source IDs remain documented in each branch report.
- These are descriptive comparisons of one selected implementation per variant per step. Different selected run numbers and agent nondeterminism are not controlled by this calculation; no significance, causality, or general improvement claim is established.
- File metrics preserve physical paths. Base step-001 relocates classes, while base step-013 renames/refactors the details class. File overlap can change because of implementation layout rather than traceability defects.
- Reviewed fragment alignment is an inspectable one-to-one role judgment, not proof of behavioral equivalence. Unit splitting/merging, inherited annotations, extra valid features, and sparse reference coverage affect both variants. Exact and reviewed results are provided together so this sensitivity remains visible.
- The reviewed fragment macro change is -0.51 pp; the file-presence macro change is +8.36 pp. These overlap differences should not be presented as independently demonstrated changes in annotation quality.

## Files

- [comparison.csv](comparison.csv): paired-cohort macro/micro precision, recall, F1, pooled counts, contributors, and changes in percentage points.
- [paired_step_metrics.csv](paired_step_metrics.csv): unaveraged base/enhanced scores and changes for each shared step/metric.
- [enhanced_matched_averages.csv](enhanced_matched_averages.csv): enhanced means using five steps instead of the original six.
- [Base averages and source snapshot](../averages_base/README.md). Original enhanced inputs remain at ../averages/source_metrics.csv.

## Reproduction

Run from main: python scripts/compare_base_enhanced_metrics.py

The default uses saved source snapshots, so branch worktrees are not required. Use --refresh-base-sources to reread base metrics from the five branch worktrees. The script does not stage, commit, change branches, or overwrite the original enhanced averages.

Enhanced input snapshot SHA-256: d72fbec56a459b9f203ffcbcb700893d51ad29a6e85c6d4add9984a23de35580
