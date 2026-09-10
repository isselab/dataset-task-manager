# dataset-task-manager

Feature-evolution dataset maintained as part of
[`isselab/agentic-feature-traced-datasets`](https://github.com/isselab/agentic-feature-traced-datasets).

## Dataset

This dataset captures controlled feature evolution for a desktop task
management application. The seed project in `project/` is a Maven-based JavaFX
application that starts from a compact task manager and includes 
feature-tracing support through `.feature-model`, `.feature-to-file`, and
embedded feature annotations.

The benchmark defines 14 sequential evolution steps from `v000` to `v014`.
The scenarios cover additions, modifications, removals, renames, relocations,
merges, splits, and feature interactions.
Each step under `benchmark/steps/step-*/` contains the user prompts that drove
the change, expected feature-level changes, and manually curated ground-truth
traceability artifacts. Ground truth is represented as:

- `.feature-model` for the expected hierarchical feature model after the step.
- `.feature-to-file` for file-level feature ownership mappings.
- `.feature-to-folder` for folder-level mappings when applicable.

The subject project also uses inline annotations in source comments with
`&begin[FeatureName]` / `&end[FeatureName]` blocks or `&line[FeatureName]`
markers for smaller feature fragments. Feature names are PascalCase and refer
to local feature names from the feature model.

Limitations: the dataset focuses on a small JavaFX application and its
traceability artifacts, not on broad production task-management behavior.

## Contents

- `project/` is the initial project supplied to Agent-HAnS.
- `benchmark/steps.json` orders the feature-evolution steps.
- Each step contains exact prompts, expected changes, and expected
  traceability artifacts.
- `docs/` contains dataset-specific documentation.
- `schema/` defines the machine-readable formats.

## Validate

```bash
python scripts/validate_dataset.py
```

The validator checks dataset structure. it does not evaluate generated output.

## License

See `LICENSE` and the `license` field in `dataset.json`.
