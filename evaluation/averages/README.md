# Average evaluation metrics

Scope: the six branches requested together. Step-010 and all other runs are excluded. Different evaluation dimensions are kept separate; there is no combined overall accuracy score.

## Macro averages (equal weight per branch)

| Metric | Precision | Recall | F1 | Contributing branches |
| --- | ---: | ---: | ---: | ---: |
| features_aligned | 91.23% | 97.62% | 94.25% | 6/6 |
| hierarchy_aligned | 86.86% | 96.67% | 90.83% | 6/6 |
| file_presence_aligned | 72.76% | 88.76% | 78.81% | 6/6 |
| fragments_feature_aligned_exact | 59.53% | 67.64% | 63.24% | 6/6 |
| fragments_reviewed_role_alignment | 67.91% | 86.24% | 73.22% | 6/6 |
| focus_fragments_reviewed_role_alignment | 38.66% | 61.93% | 43.59% | 5/6 |

## Micro averages (pooled association counts)

| Metric | TP | FP | FN | Precision | Recall | F1 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| features_aligned | 79 | 7 | 1 | 91.86% | 98.75% | 95.18% |
| hierarchy_aligned | 128 | 20 | 2 | 86.49% | 98.46% | 92.09% |
| file_presence_aligned | 139 | 37 | 11 | 78.98% | 92.67% | 85.28% |
| fragments_feature_aligned_exact | 431 | 249 | 101 | 63.38% | 81.02% | 71.12% |
| fragments_reviewed_role_alignment | 481 | 199 | 51 | 70.74% | 90.41% | 79.37% |
| focus_fragments_reviewed_role_alignment | 49 | 108 | 29 | 31.21% | 62.82% | 41.70% |

## Calculation and interpretation

- Macro precision, recall, and F1 are separate arithmetic means of the unrounded source values. Each applicable run receives equal weight. The macro F1 is the mean of branch F1 scores, not the harmonic mean of macro precision and macro recall.
- Micro precision = sum(TP)/(sum(TP)+sum(FP)); micro recall = sum(TP)/(sum(TP)+sum(FN)); micro F1 = 2sum(TP)/(2sum(TP)+sum(FP)+sum(FN)). Larger association sets contribute more weight.
- Numeric zero scores remain included. Undefined/blank scores are excluded rather than replaced by zero or 100%. The CSV records the number of contributing runs for each score and how many runs supply association counts.
- Step-013's archive-removal focus has TP=FP=FN=0, so its focus scores are undefined. Focus macro averages use five branches; its zero counts do not change the pooled totals. Focus features differ across scenarios, so focus averages summarize each run's chosen scenario scope rather than one common feature.
- Folder mapping scores are undefined in all six runs and remain N/A. Removed-feature reference counts appear only for step-013 and are not averaged as accuracy scores. All other common metrics use six branches.
- Raw-name and aligned-name metrics, exact and reviewed fragment metrics, explicit file mappings and combined file presence, and feature/hierarchy metrics remain separate. No score is averaged across these different dimensions.
- These averages inherit the source evaluations' limitations: sparse reference annotations, implementation/file-layout differences, and manually reviewed role correspondences. They describe overlap with the supplied references, not independently demonstrated annotation correctness.
- The tp/fp/fn columns contain pooled totals in both macro and micro rows for audit purposes. Only the micro scores are calculated from those totals. CSV scores are ratios in [0,1]; tables display percentages rounded to two decimals.

## Input runs and provenance

Each input came from evaluation/results/step-NNN/metrics.csv in the corresponding branch worktree. The working-file metrics were read without modifying the branch. source_head_commit identifies the branch containing the saved evaluation; evaluated_source_commit identifies the implementation that was scored.

| Branch | Step | Evaluated implementation commit | Branch HEAD at collection | Metrics SHA-256 |
| --- | --- | --- | --- | --- |
| eval-v2-step-001-run-01 | step-001 | 30615059c1d2d3dfe37ebe83c83c7f2cc87c3a7f | 041d7dd3ccfcd55b44cf37021430cd441ee9d7ee | 2f7ce6c6689d6f6a0cf90cb7413efe46a7f3abd18a7ad1ed194292584bf75565 |
| eval-v2-step-004-run-01 | step-004 | 8ff51d710c775a7d51ca8de7c2da27941dc0c2ef | 552f9ec9917c8594bb9aaf447111c16543217b37 | 3b1aa3d859ec1f0e436a2d07edcc994478ef6ce774976b33e25a22b2c293a8bd |
| eval-v2-step-009-run-02 | step-009 | ef0f15b8ff5d64548324a8c0c79b0bfa4cd5db52 | 57de6190fa391816ad4643c481fdd880cd35621e | c3184f8a846ebb5ba5c2984f7be17bf5e9edb0cf4424205b4ac788affe9b6e7f |
| eval-v2-step-003-run-03 | step-003 | 244b7444a6386f2440665f1d4644a53436b8de15 | 000ceda1bca4a098a047a875ec25a18aa22a8905 | e0c0708505c718cbf1af1eceb92daf934b9f3bffde9412fd89899a23013d01c1 |
| eval-v2-step-007-run-01 | step-007 | b888d00190786c5290b12f7ec0cebd7b035767c0 | 72bf091424badfcf1e11d29f7e514b9ed5d93504 | 838991ae14b27bac63bd21c4606c874644bf301986099635f81bd2a600559092 |
| eval-v2-step-013-run-01 | step-013 | 2a6af2eae83ba4afa21955e1dd54964e22182528 | a005d73a7c5998d0f898b6ac4649c249d980034c | 33b8358dc972f2ba467971654c8ec814f190c638ed3eba1955bf8a46d931d4fa |

## Files and reproduction

- [averages.csv](averages.csv): macro/micro results for every metric, applicability, sample sizes, and pooled counts.
- [source_metrics.csv](source_metrics.csv): source metric rows with branch identity and provenance; sufficient to reproduce averages without the other worktrees.

From the main worktree root:

```powershell
python scripts/average_evaluation_metrics.py
```

To deliberately refresh the input snapshot from the same six branch worktrees:

```powershell
python scripts/average_evaluation_metrics.py --refresh-sources
```

The script validates source counts and formulas, writes only main's average artifacts, and never stages or commits.
