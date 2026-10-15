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
Use the exact names in the current `.feature-model`. Do not invent a feature name just to annotate a piece of code. If the same name appears more than once in the model, use the feature-model skill to clarify which feature it means before adding annotations.

## Wrong implementation

&begin[Parent.Parent.Child]

## Choosing the annotation scope

Annotations MUST be placed at the smallest meaningful implementation scope:

- Do NOT annotate imports or class field and property declarations. Do not attach line annotations to them or include them in a fragment annotation.
- Do NOT attach line annotations to method declarations or signatures.
- Annotate a whole method using fragment syntax: place `&begin[FeatureName]` immediately before the method declaration and `&end[FeatureName]` immediately after its body. This also applies to single-line methods.
- Use line syntax for a single executable statement inside a method, and fragment syntax for multiple contiguous statements implementing a separate feature within the method.
- Do NOT annotate constructor declarations or bodies, including calls to another constructor. Keep constructors outside fragment annotations. Constructor calls inside ordinary methods follow the shared-statement rules.
- Do not put line annotations on `if`, `else`, or loop headers. Use a fragment only when the feature describes the whole block. If only a condition's call or argument implements the feature, separate that call into a local statement and annotate it. Keep the body's existing coverage. Preserve execution order and conditional behavior.
- NEVER use a fragment annotation to wrap an entire class, struct, interface, enum, component, module, or other top-level declaration. In particular, do not put `&begin[FeatureName]` directly before a class declaration and `&end[FeatureName]` after the class body.
- If a complete class is the implementation of a feature, represent that relationship in `.feature-to-file` instead of placing `begin`/`end` around the class. `.feature-to-file` associates the feature with the class's file, so this is appropriate when the file can be treated as belonging to that feature.
- If a file contains multiple unrelated classes or features, do not use `.feature-to-file` for the whole file. Annotate whole methods with fragment syntax and smaller implementation blocks or statements with fragment or line syntax as appropriate.
- If unrelated statements sit between parts of a feature, annotate those parts with separate fragments.
- Start and end fragments at complete code boundaries. Do not split a statement, expression, argument list, or control-flow chain. To annotate part of a branch, wrap complete statements inside that branch. To annotate a whole control block, include the full chain. Do not wrap a method signature together with only part of its body.

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
- When features share the same scope, combine their names in one line annotation or one matching fragment pair. Nest fragments only for different scopes. Close inner fragments first.
- These rules do not override the exclusions for imports, class fields, properties, and constructors or the requirement to use fragment syntax for whole methods.

For Google login in `authenticateUser` below, keep the `Authentication` method block and apply `GoogleIntegration` only to its provider-selection block.

## Feature ownership and hierarchy

- Choose features based on what the code does and what each feature means in the model, including its place in the tree. A class name, type reference, parameter, or log message alone is not enough. For example, getting a credential-provider object does not itself handle a secret key. Registering a lease does not evict it.
- Use the most specific feature that describes what the code does. Add its parent only when the same code also does separate work for that parent. Do not automatically add parent features when the code sits outside a parent block.
- Nest fragments based on how the code is structured. The nesting does not define the feature tree. Independent features can have nested fragments when their code is nested. A parent and child can have separate fragments when they are implemented in separate methods.
- If an outer fragment already covers a feature, name only the additional features in new inner annotations. Do not add repeated markers for every local variable or type reference inside that fragment. Keep existing valid annotations and mappings as required by the session-change rules above.

## Scattered features and supporting code

- For each affected feature, check the related code for missing coverage. Look at contracts, implementations, methods that read or write feature settings, serialization and deserialization, registration or setup code, and relevant callers. Follow the same scope rules everywhere. Do not annotate fields, constructors, or signatures without a method body. Use a file or folder mapping only when the whole file or folder belongs to the feature.
- Compare code that does the same work, such as an interface and its implementation, XML and JSON serializers, or different clients. Code with the same feature responsibilities should cover the same features. Its annotation scopes or mappings can differ. Check why coverage differs before copying labels or changing unrelated implementations.
- Annotate a caller, a helper that forwards a call, a serializer, or a configuration statement when it implements or directly sets up behavior for a feature. Do not copy every called method's labels to its callers or to all code that uses a shared type.
- A helper written for a feature can implement that feature even if it is called a utility. Generic timers, locks, collections, and other infrastructure do not belong to every feature that uses them. You can annotate their use when it does work for a specific feature and the scope rules allow it.
- Assign logging code to a logging or monitoring feature only when it does the work described by that feature in the model. Mentioning another feature in a log message is not enough to assign that feature. List multiple features only when the annotated logging code does work for each one.
- For tests, build files, and configuration files, follow what the project has chosen to track and what the code actually does. Do not exclude them just because one dataset left them unannotated. Use comments that fit the language or use mappings, following the same scope rules.

## UI composition and feature-specific arguments

Adding a child, argument, property, style, or callback does not give its feature exclusive ownership of a UI statement. Line annotations cover whole statements, not individual arguments.

- Preserve the component's existing responsibility through enclosing annotations, mappings, or the same comma-separated line annotation. Add the contribution's feature at the smallest meaningful scope. If existing coverage encloses the statement, only the added feature needs a line annotation.
- Annotate directly implemented behavior. Shared layout, mounting, ancestor containers, and later object uses do not inherit a child's features merely by referencing it. Do not invent generic UI features.
- Prefer existing statement boundaries. When a condition needs a separate feature-specific call, preserve when and how often that call runs.

## Review changed annotations

After implementation, MUST review the final diff with enclosing annotations and mappings: verify feature ownership, shared coverage, preserved annotations, and excluded declarations. Check that line annotations appear only at statement endings, not on control headers. If behavior remains without the added feature, the scope is shared. Resolve findings before the summary.

Also check that each label matches the feature's meaning in the model. Compare coverage in code that does the same work. Check that separate pieces use separate fragments, fragment boundaries do not split code, and new inner annotations do not repeat labels already covered. Existing annotations show how the project has annotated code, but they can be wrong. Fix clear errors in the affected code and report anything that remains unclear instead of guessing.

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
