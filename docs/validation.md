# Validation and release acceptance

This document is the reproducible validation guide for contributors and maintainers. It replaces the classroom v0.3 record and separates executed checks from pending acceptance. Application target: **v1.0.0**; course XML format: **1**.

## Executed locally — October 16, 2026

Environment: macOS 27.0.1 (arm64), Oracle JDK 25.0.3; compilation uses `--release 21`.

| Check | Result |
| --- | --- |
| `bash scripts/test.sh` | PASS, 44 checks |
| Same compiled suite with Chinese language/country JVM defaults | PASS, 44 checks |
| Copy source without `.git` or `build/` into a temporary path containing spaces, then run `bash scripts/test.sh` | PASS, 44 checks |
| Inspect runnable JAR manifest | Main class `edu.dku.gradeplanner.App`; implementation version `1.0.0` |
| Inspect every packaged class file | Java 21 bytecode (major version 65) |
| Inspect JAR contents | Production classes present; no test classes, DemoData, or course XML |
| `bash -n scripts/build.sh scripts/test.sh scripts/run.sh` | PASS |

Local compilation on JDK 25 with a Java 21 target is not execution on a Java 21 runtime. No Java 21 runtime or PowerShell is installed in this environment. Linux/Windows execution, the new GitHub Actions workflows, and complete GUI acceptance remain pending. Earlier classroom verification claims are not carried forward as evidence for this release.

## Reproduce automated checks

macOS / Linux:

```bash
bash scripts/test.sh
java -Djava.awt.headless=true -Duser.language=zh -Duser.country=CN -cp build/classes:build/test-classes edu.dku.gradeplanner.GradePlannerTest
```

Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test.ps1
java -Djava.awt.headless=true -Duser.language=zh -Duser.country=CN -cp "build/classes;build/test-classes" edu.dku.gradeplanner.GradePlannerTest
```

Both test scripts rebuild production and test classes. Tests exit unsuccessfully when an assertion fails; there are no external test dependencies. CI is configured for Java 21 and 25 on Ubuntu, macOS, and Windows, including Chinese-default locale and JAR content/version checks. Record the workflow URL and commit when those jobs have actually passed.

## Coverage and validation contracts

- **Models:** earned points from zero to maximum, positive maximum, numeric magnitude/precision bounds, unique category/entry identities, category weights in 0–100, and valid finalization. Empty courses and draft weight totals are allowed. Constructor checks also apply to loaded data.
- **Calculator:** equal-weight versus total-points averages; zero included and blank excluded; no average for ungraded categories; finalized-only contributions; weights totaling 100 before solving; one eligible unfinished exam; required predictions; finalized actuals overriding predictions; zero/unreachable/exact-boundary outcomes; required-score rounding upward.
- **Storage:** XML round trips with non-Latin names, special characters, zero and blank; overwrite; unsupported version, invalid stored score, corrupt XML, and failed save reported as errors.
- **Locale/startup defaults:** English application/Swing locale and standard labels; empty course name/categories.

Limits of this suite: it does not drive Swing interactions, verify every input branch, exercise every filesystem failure, or establish runtime compatibility on platforms not executed. Add regression coverage when behavior changes rather than increasing a check count for its own sake.

## Manual acceptance — pending for v1.0.0

Run the application on a graphical desktop with invented data. Record OS, JDK, commit, date, and PASS/FAIL for each applicable step in the release issue. Do not mark this checklist complete solely because headless tests pass.

- [ ] **Startup and editing:** confirm fixed DKU Grade Planner heading, separate Untitled Course name, empty tables and target, and onboarding text. Rename course; add/edit/delete categories and entries. Cancelling edits preserves data.
- [ ] **Averages:** create entries 8/10, 90/100, and one blank. Equal Weight shows 85.00; Total Points shows 89.09. Add zero and confirm it is included; blank remains excluded.
- [ ] **Finalization and invalid input:** blank entries prevent finalization. Reopen a finalized category before adding ungraded work. Negative/excess scores, zero maximum, and invalid weights show understandable errors. Draft weights not totaling 100 can be saved but cannot be used to solve a target.
- [ ] **Target fixture:** use Homework 10%, Quiz 20%, Midterm 25%, Project 15%, Final exam 30%. Midterm has a graded 80/100 entry and is finalized; the others are unfinished. Final exam has exactly one blank entry. Enter expected final averages Homework 90, Quiz 85, Project 90 and target 85. The required exam score is 85.00; projected totals at exam 0/100 are 59.50/89.50. Target 95 is unreachable; target 55 needs zero.
- [ ] **Prediction state:** in a separate course with two eligible exams, switch the exam while editing a prediction; values stay associated with category IDs. Editing grades, weights, target, or predictions invalidates the old result. Missing predictions are requested explicitly.
- [ ] **Persistence:** save, restart, reopen. Order, non-Latin names, zero, and blank survive; predictions and target are not persisted. New/close with unsaved data offers Save/Discard/Cancel; Cancel preserves data. Failed save keeps the course dirty.
- [ ] **Failed open:** opening corrupt/unsupported XML reports an error and preserves the active course.
- [ ] **Presentation:** inspect minimum window size, headings and long result/error messages. On Chinese-default systems, built-in buttons and dialogs remain English. User-entered names remain unchanged.
- [ ] **Release artifact:** run the downloaded draft JAR with Java 21 on a graphical desktop and compare its SHA-256 checksum. Repeat startup and file workflows on the supported desktop platforms; document limitations explicitly.

## Release gate

All automated CI jobs must pass on the intended release commit. Complete the manual checks or explicitly narrow the release's claimed platform support before publication. Record actual failures as issues with steps and evidence; planned tests are not an issue log. Follow [releasing](releasing.md) for tag, draft assets, and publication steps.
