# Base-agent average evaluation metrics

Five base runs: steps 001, 003, 004, 009, and 013, each run-01. The original enhanced six-run averages in ../averages/ are unchanged. Use ../base_vs_enhanced/ for comparison over the same five steps.

## Macro averages: equal weight per applicable run

| Metric | Precision | Recall | F1 | Defined input F1 scores |
| --- | ---: | ---: | ---: | ---: |
| features_aligned | 90.07% | 95.81% | 92.61% | 5/5 |
| hierarchy_aligned | 88.27% | 94.18% | 90.83% | 5/5 |
| file_presence_aligned | 65.35% | 73.71% | 68.76% | 5/5 |
| fragments_feature_aligned_exact | 53.70% | 61.30% | 57.06% | 5/5 |
| fragments_reviewed_role_alignment | 65.80% | 86.38% | 71.53% | 5/5 |
| focus_fragments_reviewed_role_alignment | 42.84% | 78.89% | 50.82% | 4/5 |

## Micro averages: pooled TP/FP/FN counts

| Metric | Precision | Recall | F1 | Defined input F1 scores |
| --- | ---: | ---: | ---: | ---: |
| features_aligned | 91.67% | 97.06% | 94.29% | 5/5 |
| hierarchy_aligned | 90.16% | 96.49% | 93.22% | 5/5 |
| file_presence_aligned | 72.22% | 83.20% | 77.32% | 5/5 |
| fragments_feature_aligned_exact | 59.82% | 76.80% | 67.26% | 5/5 |
| fragments_reviewed_role_alignment | 71.05% | 91.22% | 79.88% | 5/5 |
| focus_fragments_reviewed_role_alignment | 40.46% | 76.81% | 53.00% | 4/5 |

## Calculation and limits

- Macro precision, recall, and F1 are independent arithmetic means of unrounded branch values. Macro F1 is not recomputed from macro precision/recall.
- Micro scores are recomputed after pooling TP/FP/FN separately within each evaluation dimension. Score units in CSV are ratios, not percentages.
- Numeric zeros are included. Undefined scores are excluded rather than filled with zero or 100%. Folder mappings remain N/A. Step-013 archive-removal focus has empty expected/generated associations, so focus macro averages use four defined runs.
- Raw/aligned features and hierarchy, file presence/explicit mappings, exact/reviewed fragments, and scenario focus remain separate. No overall accuracy is invented by averaging different dimensions together.
- The source evaluations use the same normalization and matching policies as the enhanced runs. They measure overlap against a potentially sparse reference, with inspectable role-level judgments. File layout, code-unit splitting, and implementation differences affect results independently of annotation quality.
- In particular, base step-001 relocates classes into taskmanager packages, producing zero literal-path file overlap. Step-009 does not introduce a separate TaskOrdering feature. Neither difference is silently normalized away.

## Sources

| Base branch | Step | Evaluated implementation commit | Metrics SHA-256 |
| --- | --- | --- | --- |
| eval-step-001-run-01 | step-001 | 178ac9520d0d6c8731fdbb6329d1b3a909c80bb6 | ed31d3b245a814a55622dbceb56327b8d70b37d649778b1be2be85b31c0fe15b |
| eval-step-003-run-01 | step-003 | b47c2a71132efbafe21eeadd521c66c8b79cbaa4 | 7c3b7313e06ed164b6a5650674d0dfa461bb537fc388931cb13c205fe9a5a909 |
| eval-step-004-run-01 | step-004 | 11f0758c151c629398abcb2f39be4445185ae0a7 | 606bf6778f70ac7d830475ae3415945a51b60ef34dd0a3d0376d96e02099bc68 |
| eval-step-009-run-01 | step-009 | ad27cf9f4b1301b845c98b3440c42c2c78306071 | b1ba6f656afd43dfc962835098eb179d925787bee6092c2264addcfb7cc3b1b7 |
| eval-step-013-run-01 | step-013 | e4fe5906dd4bed14aa94fee685f22723b4c21165 | 63d11c6a6ea718b86854c49c88e5ac51366dd663927336f6eab2cc79fc963a50 |

## Files and reproduction

- [averages.csv](averages.csv): macro/micro results for every metric, applicability, sample sizes, and pooled counts.
- [source_metrics.csv](source_metrics.csv): all base source rows plus branch/commit/hash provenance.
- [Matched comparison](../base_vs_enhanced/README.md): base versus enhanced results on the same five steps.

Run from main: python scripts/compare_base_enhanced_metrics.py

Refresh base inputs explicitly: python scripts/compare_base_enhanced_metrics.py --refresh-base-sources
