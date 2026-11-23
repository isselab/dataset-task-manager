---
name: feature-model-skill
description: Updates feature model based on user intent and the existing feature model
---

# Feature model syntax

The feature model reflects the logical dependencies and hierarchy between features.
The feature model MUST be created in a file called `.feature-model` in the project root.
If the `.feature-model` file doesn't exist, create it with the root feature named after the project.

Each feature is represented by its feature name in PascalCase.
An intendation represents the hierarchy between features, a child is indented one step from its parent.
The root feature should be named after the project.

## Feature identity and interpretation

- Each feature name must be unique across the whole model. Annotations and mappings use only the name, without its path in the tree. Before adding a feature, check whether its name or responsibility already exists anywhere in the tree.
- Understand a feature by its name, its parent branch, and the work it implements or is meant to implement. Do not assign code just because it uses similar words. Nested annotations do not mean that one feature is the parent of another. Sharing code does not by itself make one feature a child of another.
- If the same name appears more than once in the model, do not guess which feature an annotation means. Use the work each feature is meant to do to resolve the name. If different features need different names, update the model and the affected annotations and mappings together. Ask for clarification if it is still unclear which feature is meant.
- A feature in the model may not have code in the current version. Do not remove it just because no annotations mention it. Do not annotate unrelated code just to give every feature coverage. Remove a feature when the work it represents has actually been removed.

```text
Store
    Products
        AddProduct
        RemoveProduct
        UpdateProduct
    Staff
        ReportTime
        Authorization
    Customers
        Delivery
        Return
```

## Example of feature model

```text
HumanResourcesManager
    User
        Payroll
        Authentication
    GoogleIntegration
    OtherFeature1
        ChildFeature1
    OtherFeature2
```

GoogleIntegration is a separate feature that affects the entire project, but reflects in the user authentication by only one line. Therefore it isn't a child of authentication, even though it looks like it from the feature annotations.
When removing the Authentication feature, the usual rule is to not touch other feature annotations, but in this case, it can be inferred that this line is highly linked to authentication and can therefore also be removed.
