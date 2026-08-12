# dataset-task-manager

Feature-evolution dataset maintained as part of
[`isselab/agentic-feature-traced-datasets`](https://github.com/isselab/agentic-feature-traced-datasets).

## Dataset

Replace this section with the dataset's domain, initial project, controlled
evolution scenarios, feature/annotation formats, and limitations. Keep
`dataset.json` synchronized with this description.

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

The validator checks dataset structure; it does not evaluate generated output.

## License

See `LICENSE` and the `license` field in `dataset.json`.
