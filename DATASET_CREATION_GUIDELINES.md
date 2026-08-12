# Guidelines for Creating Agentic Feature-Traceability Datasets

## 1. Purpose and Principles

These guidelines define benchmark datasets for evaluating whether an agentic coding workflow maintains feature traceability as software evolves.

A dataset is a controlled evolution history containing:

- a complete initial project
- an ordered sequence of exact developer prompts
- documented feature-level changes for every step
- manually validated post-step ground truth for the feature model, folder and file mappings and fragment annotations

The ground truth is the evaluation oracle. It must be created independently of the evaluated tool's output and must never be provided to that tool during a benchmark run.

Each dataset must be:

- **Controlled:** Every step introduces reviewable changes whose intended feature operations are known in advance.
- **Realistic:** The subject resembles a small real application, using ordinary structures such as modules, services, models, persistence, configuration, tests, or UI components.
- **Progressive:** Later steps introduce harder cases such as scattered implementation, shared files, interactions, restructuring, and deletion.
- **Reproducible:** Exact prompts, the authoring environment, and continuous logical versions are preserved.
- **Independent:** Tool-generated models or annotations are review candidates, not ground truth.

## 2. Scope and Repository Contract

Each repository represents one software project and one ordered evolution history.

| Property                          | Requirement                                                       |
| --------------------------------- | ----------------------------------------------------------------- |
| Initial logical version           | `v000`                                                            |
| Evolution steps                   | 10-12; 12 preferred when feasible                                 |
| Prompts                           | At least one per step; normally 1-3 related prompts               |
| Initial top-level domain features | At least 2                                                        |
| Final feature-model size          | Normally 8-20 meaningful features                                 |
| Final source-code size            | Normally 15-60 relevant source files                              |
| Language                          | Prefer one primary language (could have exceptions)               |
| Build and tests                   | Must run locally with documented commands                         |
| Operations                        | Add, modify, delete, rename, relocate, split, and merge           |
| Complex implementation            | At least one scattered feature and one shared or tangled fragment |

Retain the template structure:

```text
.
|-- project/                           initial project at v000
|-- benchmark/
|   |-- steps.json                     ordered step index
|   `-- steps/
|       `-- step-NNN/
|           |-- step.json              transition and intended changes
|           |-- prompts/               exact prompts in execution order
|           `-- ground-truth/          expected post-step traceability
|-- docs/                              dataset-specific documentation
|-- schema/                            JSON schemas
|-- scripts/
|   `-- validate_dataset.py
|-- dataset.json                       dataset metadata
|-- LICENSE
`-- README.md
```

`step-001` transitions from `v000` to `v001`, and each later step starts from the state produced by its predecessor. Do not rename schema-defined fields or use alternative step numbering. Additional documentation belongs under `docs/` and does not replace required machine-readable files.

## 3. Subject and Evolution Plan

Choose something you are genuinely interested in building. The project should be engaging enough that you can make credible product and technical decisions throughout its evolution. Prefer a domain that naturally supports related features and interactions, such as task management, booking, inventory, issue tracking, learning management, or content management.

Plan the subject and every evolution step from the perspectives of all three stakeholder roles:

- **Product owner (PO):** Define the product goal, prioritize valuable increments and control scope.
- **User:** Consider real workflows, usability, observable behavior, feedback, and edge cases that affect whether the software is useful.
- **Developer:** Consider technical feasibility, architecture, maintainability, testing, refactoring needs, and the cost of changing existing code.

These perspectives must shape the evolution together. A feature that appears valuable to the PO should solve a plausible user need and be implementable and maintainable from the developer's perspective. Individual prompts may emphasize one role, but the complete evolution must demonstrate that all three were considered. You can even record important stakeholder assumptions and motivations in the dataset documentation so reviewers can understand why each change belongs in the project.

Let the project evolve as a real project would. Start with a useful baseline, add capabilities in response to product goals and user needs, and allow feedback, newly discovered constraints, technical debt, and changing priorities to motivate later modifications, refactorings, relocations, splits, merges, or deletions. You can refer to [ReferenceManager dataset](https://johan.martinson.phd/agentic-traceability-maintenance-dataset/) as an example.

The initial state may be an implemented baseline, but its features and traceability state must be documented as `v000`.

The following sequence is recommended. Authors may change or combine scenarios as long as all required operations remain clear and covered.

| Step | Scenario                                                   | Main challenge                                       |
| ---- | ---------------------------------------------------------- | ---------------------------------------------------- |
| 001  | Add a substantial domain capability                        | Establish a feature subtree and mappings             |
| 002  | Add an independent capability                              | Distinguish separate implementations                 |
| 003  | Extend a feature across several files                      | Track scattered changes                              |
| 004  | Add a capability that depends on existing features         | Represent interaction or tangled code                |
| 005  | Move implementation                                        | Replace stale locations                              |
| 006  | Rename a feature without changing its identity             | Record its old and new names                         |
| 007  | Split a broad feature                                      | Create successors and redistribute traces            |
| 008  | Merge overlapping features                                 | Consolidate traces and remove obsolete ones          |
| 009  | Add a cross-cutting capability                             | Trace multiple modules and features                  |
| 010  | Change shared interaction logic                            | Update only affected interaction traces              |
| 011  | Delete an established feature                              | Remove code and all stale traces                     |
| 012  | Apply a compound change or behavior-preserving refactoring | Distinguish feature evolution from structural change |

Across the dataset, include at least:

- two additions
- two modifications, one spanning multiple files
- one deletion, rename, relocation, split, and merge
- two interaction cases, including a later modification or removal
- one refactoring that changes code locations without adding a user-visible feature

For a 10-step dataset, compatible operations may share a step, but avoid combinations that make the expected result ambiguous.

## 4. Operation and Transition Rules

| Operation     | Meaning                                                                                         |
| ------------- | ----------------------------------------------------------------------------------------------- |
| `add`         | Introduces new behavior or a new selectable/domain capability.                                  |
| `modify`      | Changes an existing feature's behavior, responsibility, model structure, or traceability scope. |
| `delete`      | Removes a feature and artifacts no longer required.                                             |
| `rename`      | Changes the display name while preserving conceptual identity.                                  |
| `relocate`    | Moves implementation without changing behavior or conceptual identity.                          |
| `split`       | Replaces one feature with two or more successors and records the predecessor.                   |
| `merge`       | Replaces multiple features with one successor.                                                  |
| `interaction` | Adds or changes behavior that depends jointly on two or more features.                          |

A refactoring is not automatically a feature modification.

In `step.json`, use the exact feature names from the feature model in `expected_changes[].features_before` and `features_after`.

## 5. Prompts and Step Workflow

Store every prompt exactly as submitted; never rewrite it after seeing the result. A prompt should:

- express one coherent development objective;
- describe observable behavior and important constraints;
- be realistic for an agentic coding workflow;
- avoid prescribing traceability artifacts

Avoid vague requests unless ambiguity is intentionally under evaluation.

Example:

```text
Add reusable labels for tasks. A task may have multiple labels, and the same
label may be assigned to multiple tasks. Provide endpoints to create, list,
rename, and delete labels. Deleting a label must remove its assignments but
must not delete any task.
```

Store multiple prompts as separate files in execution order and list them in the same order in `step.json`. After receiving each agent response, use `\export` and store the exported response in `prompts/agent_response.md` beside the prompt files.

For each step:

1. Start from the exact preceding state and confirm the build and tests pass.
2. Submit the stored prompts in order.
3. Export and store each response with its corresponding prompt by using `\export`.
4. Preserve the result without silently adding manual feature work. Use recorded corrective prompts for defects, or explicitly document manual intervention.
5. Verify the requested behavior, build, and tests.
6. Inspect the implementation and record the actual feature-level result in `step.json`.
7. Create and cross-check the complete post-step ground truth.
8. Run `python scripts/validate_dataset.py`.
9. Commit the validated step with its logical version.

Reject or repeat a step if it does not implement the requested behavior, does not build, or is too ambiguous for defensible ground truth.

## 6. Ground-Truth Rules

After each evolution step, the developer must review the feature model and all feature-artifact files and store the validated versions under that step's `ground-truth/` directory. All feature annotations and artifacts must accurately describe the software state produced by that specific step and satisfy the applicable schemas.

## 7. Validation and Review

Validate both:

1. **Implementation correctness:** Requested behavior works, required existing behavior remains, builds and tests pass, and no evaluation-relevant placeholders remain.
2. **Traceability correctness:** The feature model matches the implementation; mappings and fragments are complete and justified; interactions follow policy; and no unsupported, deleted, or relocated traces remain.

At least one step must help detect hallucinated traces, for example:

- a behavior-preserving refactoring with no new feature;
- a change to one feature while nearby features remain unchanged
- removal of an interaction while its features remain independently.

Never copy plausible but unsupported tool output into the ground truth. Hallucination is an evaluation error, not an intended operation.

## 8. Metadata, Documentation, and Versioning

Complete `dataset.json` according to its schema, including:

- dataset identity, repository, domain, language, framework, and build system;
- authors and ground-truth reviewers;
- licenses and provenance for code, prompts, annotations, and reused assets;
- known limitation

Do not include secrets, private data, credentials, proprietary code, or material that cannot be redistributed.

The README must summarize the domain, size, operation coverage, build commands, and limitations. Put other detailed information under `docs/`.

Use continuous logical versions:

```text
v000 -> v001 -> v002 -> ... -> v012
```

Commit the validated initial subject as `v000` and create one identifiable commit per completed step. Add an annotated Git tag to each of these commits using the corresponding logical version: `v000` for the initial state, `v001` for the state after `step-001`, and so on. A tag identifies the complete validated repository state at that version, including the implementation and the benchmark artifacts accumulated up to that point. For example:

```bash
git tag -a v000 -m "Initial subject project"
git tag -a v001 -m "State after evolution step 001"
git push origin --tags
```

The Git tags complement the version fields in the dataset metadata. They do not replace them. Do not move or reuse a published version tag.

You can use [Semantic Versioning](https://semver.org/) (`MAJOR.MINOR.PATCH`) for releases of the complete dataset. Increment `MAJOR` for incompatible changes to the dataset format or evaluation assumptions, `MINOR` for backward-compatible additions, and `PATCH` for backward-compatible corrections. Semantic release versions complement rather than replace the logical evolution versions: `v000` through `v012` identify states within the project history, whereas a tag such as `dataset-v1.0.0` identifies a published release of the complete dataset.

After a dataset has been published or used for evaluation, do not modify its prompts. Publish ground-truth corrections as a new dataset release and document the changes.

## 9. Acceptance Checklist

A dataset is ready only when:

- [ ] `v000` is complete, documented, buildable, and tested.
- [ ] It contains 10-12 continuously ordered steps with at least one exact prompt each.
- [ ] The subject is motivated by a coherent product story, and the PO, user, and developer perspectives are represented across its evolution.
- [ ] Every agent response is preserved with its corresponding prompt using `\export`.
- [ ] All required operations and complexity cases are covered.
- [ ] Each step has a complete, schema-valid post-step feature model, mappings and fragments.
- [ ] Feature names are consistent across the feature model, transition metadata, and tracking artifacts.
- [ ] The project builds and relevant tests pass after every step.
- [ ] Every validated version commit has the corresponding annotated Git tag (`v000`, `v001`, and so on), and published tags have not been moved or reused.
- [ ] Ground truth is manually validated, independent of tool output, and additionally reviewed for complex steps where possible.
- [ ] No stale, superficial, unsupported, or hallucinated traces remain.
- [ ] Manual edits, retries, corrections, provenance, environment, licenses, and limitations are documented.
- [ ] `python scripts/validate_dataset.py` succeeds.

Datasets that fail these criteria must be revised before inclusion in the benchmark collection.
