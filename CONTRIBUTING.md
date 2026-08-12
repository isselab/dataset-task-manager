# Contributing

## Using the template outside `isselab`

You do not need to belong to the `isselab` organization to use this template
or validate a dataset. Create a repository from the template in your personal
account or another organization where you have permission to create
repositories.

Bootstrap customization works outside `isselab` and removes its one-shot files
without an organization secret. Parent synchronization is specific to
`isselab`: its dispatched run will fail because an external repository cannot
access `PARENT_REPO_PAT` or update the official dataset catalog. Do not request
or substitute an organization token. After bootstrap finishes:

1. Remove `.github/workflows/sync-parent.yml` from your generated repository.
2. Keep `.github/workflows/validate.yml`, `scripts/validate_dataset.py`, and the
   `schema/` directory.
3. Replace the remaining placeholder metadata and README sections with a
   description of your dataset.
4. Follow the authoring workflow in the README and run the validator locally
   before pushing changes:

   ```bash
   python scripts/validate_dataset.py
   ```

The retained **Validate dataset** workflow runs the same validation on every
push and pull request and does not require organization secrets.

An externally maintained dataset is not registered automatically in
`isselab/agentic-feature-traced-datasets`. Contact that repository's
maintainers separately if you want to propose it for inclusion.
