#!/usr/bin/env python3
"""Customize a repository generated from the dataset template."""

from __future__ import annotations

import datetime as dt
import json
import os
from pathlib import Path

root = Path(__file__).resolve().parents[2]
repository = os.environ["GITHUB_REPOSITORY"]
name = repository.split("/", 1)[1]

manifest_path = root / "dataset.json"
manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
manifest["name"] = name
manifest["title"] = name
manifest["description"] = f"Feature-traced dataset maintained in {repository}."
manifest["repository"] = f"https://github.com/{repository}"
manifest["created"] = dt.date.today().isoformat()
manifest_path.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")

readme = f"""# {name}

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
"""
(root / "README.md").write_text(readme, encoding="utf-8")

for relative in (
    ".github/.bootstrap",
    ".github/workflows/bootstrap.yml",
    ".github/scripts/bootstrap_dataset.py",
    "CONTRIBUTING.md",
    "DATASET_CREATION_GUIDELINES.md"
):
    (root / relative).unlink()

scripts_dir = root / ".github/scripts"
if scripts_dir.exists() and not any(scripts_dir.iterdir()):
    scripts_dir.rmdir()
