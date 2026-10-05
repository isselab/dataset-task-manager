---
name: embedded-feature-annotation-skill
description: Create or update embedded feature annotations based on implemented responsibilities
---

# Feature annotation syntax

Embedded feature annotations MUST follow the following syntax.
Features can be annotated using both fragment and line syntax:

## Fragment

&begin[FeatureName]
<Feature Implementation>
&end[FeatureName]

For a shared block, use `&begin[FeatureA,FeatureB]` and `&end[FeatureA,FeatureB]`. Both lists must contain the same names in the same order.

## Line

<Feature Implementation> &line[FeatureName]

For a shared statement, use `&line[FeatureA,FeatureB]`. In both forms, separate feature names with commas and list each feature once.

Feature names MUST be written in PascalCase.
Feature annotations MUST be written in a comment (different depending on language)
Place one line annotation on the statement's final line, with its feature names separated by commas. List each feature once. Use fragment syntax for several statements or a whole control block, subject to the scope rules below.
No blank lines between feature annotations and the code it wraps.

FeatureNames should be standalone and not include any parents or children.

## Wrong implementation

&begin[Parent.Parent.Child]

## Choosing the annotation scope

Annotations MUST be placed at the smallest meaningful implementation scope:

- Do NOT annotate imports or class field and property declarations. Do not attach line annotations to them or include them in a fragment annotation.
- Do NOT attach line annotations to method declarations or signatures.
- Annotate a whole method using fragment syntax: place `&begin[FeatureName]` immediately before the method declaration and `&end[FeatureName]` immediately after its body. This also applies to single-line methods.
- Use line syntax for a single executable statement inside a method, and fragment syntax for multiple contiguous statements implementing a separate feature within the method.
- Do NOT annotate constructor declarations or bodies, including calls to another constructor. Keep constructors outside fragment annotations. Constructor calls inside ordinary methods follow the shared-statement rules.
- Do not put line annotations on `if`, `else`, or loop headers. Use a fragment only when the feature describes the whole block. If only a condition's call or argument implements the feature, separate that call into a local statement and annotate it; keep the body's existing coverage. Preserve execution order and conditional behavior.
- NEVER use a fragment annotation to wrap an entire class, struct, interface, enum, component, module, or other top-level declaration. In particular, do not put `&begin[FeatureName]` directly before a class declaration and `&end[FeatureName]` after the class body.
- If a complete class is the implementation of a feature, represent that relationship in `.feature-to-file` instead of placing `begin`/`end` around the class. `.feature-to-file` associates the feature with the class's file, so this is appropriate when the file can be treated as belonging to that feature.
- If a file contains multiple unrelated classes or features, do not use `.feature-to-file` for the whole file. Annotate whole methods with fragment syntax and smaller implementation blocks or statements with fragment or line syntax as appropriate.

If a whole file is related to a feature it can be annotated with a file named `.feature-to-file`. Instead of wrapping an entire file in a single feature, prefer using `.feature-to-file`.
This file should be placed in the same directory as the given file. It contains consecutive pairs of non-empty lines: a filename, then its feature name.

For a folder that belongs to a feature, use `.feature-to-folder` in the folder's parent directory. It uses the same two-line pairs: folder name, then feature name. A folder mapping covers the files in that folder and its subfolders.

Before adding a file or folder mapping, read the existing `.feature-to-file` or `.feature-to-folder` in that directory. Append the new target/feature pair if it is absent. Preserve every existing pair, including mappings for other targets and other features of the same target. Never replace a mapping file with only the new pair or remove old pairs merely because they are unrelated to the current task. Remove or update an old pair only when its mapped target or feature relationship has actually been removed or changed. After editing, compare the mapping file with its previous version and verify that all still-valid pairs remain.

Example:
UserController.cs
UserManagement
PayrollService.cs
Payroll

For a class-level relationship, use the same file mapping and do not add an inline class wrapper.

## Shared code and session changes

Annotations describe the features implemented by the code, not which feature motivated the current session's edits.

- Inspect the surrounding implementation and existing annotations before annotating changed code. Preserve existing feature annotations and file and folder mappings while those relationships remain valid. Adding or changing a file or folder mapping does not replace valid fragment or line annotations in the covered code. Keep those annotations where their feature relationships remain valid.
- Do NOT relabel a shared method or block solely with the current feature because it was changed in this session. Include all features the annotated code actually implements, including existing responsibilities. Do not add unrelated features merely because they call the code.
- When the current feature affects only part of an existing method, retain the method's feature block and annotate the relevant statements inside it. Do not extend the current feature's annotation over unrelated code.
- When features share the same scope, combine their names in one line annotation or one matching fragment pair. Nest fragments only for different scopes; close inner fragments first.
- These rules do not override the exclusions for imports, class fields, properties, and constructors or the requirement to use fragment syntax for whole methods.

For Google login in `authenticateUser` below, keep the `Authentication` method block and apply `GoogleIntegration` only to its provider-selection block.

## UI composition and feature-specific arguments

Adding a child, argument, property, style, or callback does not give its feature exclusive ownership of a UI statement. Line annotations cover whole statements, not individual arguments.

- Preserve the component's existing responsibility through enclosing annotations, mappings, or the same comma-separated line annotation. Add the contribution's feature at the smallest meaningful scope. If existing coverage encloses the statement, only the added feature needs a line annotation.
- Annotate directly implemented behavior. Shared layout, mounting, ancestor containers, and later object uses do not inherit a child's features merely by referencing it. Do not invent generic UI features.
- Adding `chips` to an `HBox` contributes `AssignTaskLabels`; passing `color` to a label constructor contributes `LabelColors`. A line annotation for that contribution is valid only together with coverage for row composition or label creation. Alignment and collection insertion do not automatically gain those features.
- Prefer existing statement boundaries. When a condition needs a separate feature-specific call, preserve when and how often that call runs.

## Review changed annotations

After implementation, MUST review the final diff with enclosing annotations and mappings: verify feature ownership, shared coverage, preserved annotations, and excluded declarations. Check that line annotations appear only at statement endings, not on control headers. If behavior remains without the added feature, the scope is shared. Resolve findings before the summary.

Run `node <annotation-skill-directory>/scripts/validate-annotations.mjs --root .` from the project root. Fix reported errors. It checks annotation syntax, feature names, fragment balance, imports, closing braces, and mappings. Statement endings, declaration scope, and feature ownership still need manual review.

## Example of code with annotations

class User {
const userId
const fullName

    constructor(userId, fullName) {
        this.userId = userId
        this.fullName = fullName
    }

    getCredentials() {
        return {
            userId: this.userId,
            token: "session-token"
        }
    }

    const bankAccountNumber
    const monthlyBaseSalary
    const absenceDaysLastMonth

    // &begin[Payroll]
    getMonthlySalary() {
        const dailyRate = this.monthlyBaseSalary / 22
        const absenceDeduction = dailyRate * this.absenceDaysLastMonth
        return this.monthlyBaseSalary - absenceDeduction
    }
    // &end[Payroll]

    // &begin[Authentication]
    function authenticateUser(user, requiredAccess, loginMethod) {
        const credentials = user.getCredentials()

        // &begin[GoogleIntegration]
        if (loginMethod === "google") {
            credentials.provider = "google"
        }
        // &end[GoogleIntegration]

        const accessPoints = getAccessPoints(requiredAccess)
        const isAuthenticated = verifyCredentials(credentials, accessPoints)

        return isAuthenticated
    }
    // &end[Authentication]

}
