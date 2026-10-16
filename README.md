# DKU Grade Planner

**English** | [Chinese README](README.zh-CN.md)

An open-source course grade and target exam score calculator built with **Java 21 + Swing**. First public release: **v1.0.0**. No Maven, Gradle, database, or third-party library is required.

## Run

Download the runnable JAR from [GitHub Releases](https://github.com/ClarkzzZz/dku-grade-planner/releases), then run `java -jar dku-grade-planner.jar`. Java 21 or newer is required; Java is not bundled. To build from source, follow the steps below.

Configure **JDK 21** and check both `java -version` and `javac -version`. From the repository root:

macOS / Linux:

```bash
bash scripts/run.sh
```

Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run.ps1
```

The build uses `javac --release 21`. After compiling, you can also run:

```bash
java -jar build/dku-grade-planner.jar
```

Running requires Java 21 or newer and a graphical desktop. Generated files in `build/` are ignored by Git; rebuild after cloning.

In an IDE, select JDK 21, mark `src/main/java` as a source directory, and run `edu.dku.gradeplanner.App`.

## Features

- Add, edit, and delete grading categories, weights, and individual entries.
- Record multiple homework and quiz scores with separate earned and maximum points; up to 1,000 entries per category.
- Choose Equal Weight or Total Points aggregation.
- Exclude ungraded entries; include real scores of zero.
- View current category averages, finalized contributions, and the weighted average of finalized categories.
- Enter a target overall grade and expected final averages for unfinished categories to solve one unknown exam.
- Show unreachable targets, cases where zero suffices, projected grades with 0/100 on the exam, and the assumptions used.
- Validate weights, scores, and completion status; save and load actual grades in local XML files.
- Start with a blank course and guide users to add their own syllabus information.
- Prompt for unsaved grade changes; keep current data when loading fails.

## Getting Started

The header displays **DKU Grade Planner**. The course name appears separately underneath. On startup, **Untitled Course** has no categories or scores, and the target field is blank.

1. Click **Course Name** to name your course.
2. Click **Add Category** for each syllabus grading category. Enter its name, percentage weight, and aggregation method; weights must total 100% before target calculations.
3. Select a category and click **Add Entry** to record each homework, quiz, or exam. Enter earned and maximum points; leave the score blank if ungraded.
4. When every entry in a category is graded, use **Edit Category** to finalize it. Its actual average then contributes to the finalized total.
5. For target calculations, select an unfinished exam category containing exactly one ungraded entry and positive weight. Enter your target grade and expected final averages for other unfinished categories. Click **Calculate**.
6. Click **Save** to store your course locally, or **Open** to load an existing course. Predictions and the target are temporary and are not saved.

To add ungraded work to a finalized category, first reopen it using **Edit Category**. **New** creates another blank course and prompts you to save any unsaved grade changes. The app ships with no demonstration course files or seeded grades.

## Calculation Rules

```text
Entry percentage = earned points / maximum points × 100
Equal-weight average = sum of graded entry percentages / graded entry count
Total-points average = total earned points / total maximum points × 100
Finalized contribution = final category average × weight / 100
Required exam percentage = (target - other actual or expected contributions) / (exam weight / 100)
```

An unfinished category's current average describes its graded entries, not its final contribution. Enter predictions explicitly or use **Copy Current Averages**. Results list the assumptions. Finalized categories always use actual grades; zero-weight categories do not affect predictions.

Calculations use `BigDecimal` with DECIMAL128 precision. Ordinary display values use two decimal places; required minimum scores round upward to two places. The actual exam's scoring increments may differ.

When weights do not total 100%, you may edit and save a draft but cannot solve a target. When all categories are finalized and weights total 100%, the top summary shows the final overall grade.

## Tests

macOS / Linux:

```bash
bash scripts/test.sh
```

Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/test.ps1
```

Tests cover calculations, edge cases, locale defaults, and XML storage. See [validation](docs/validation.md) for reproducible checks and the manual acceptance checklist. GitHub Actions is configured to test Java 21 and 25 on Linux, macOS, and Windows; a configured job is not evidence that it has passed.

## Architecture and Contributing

The immutable models (`Course`, `GradeCategory`, `GradeEntry`), calculator (`GradeCalculator`), XML persistence (`CourseStorage`), and Swing interface (`App`, `MainFrame`) are separate components. See the [architecture guide](docs/architecture.md).

Contributions from everyone are welcome: report bugs, improve documentation, add tests, or propose focused changes through pull requests. Start with [CONTRIBUTING.md](CONTRIBUTING.md). English and Chinese issue descriptions are welcome. Do not upload personal grade files.

See the [changelog](CHANGELOG.md), [v1.0.0 release notes](docs/releases/v1.0.0.md), and [release checklist](docs/releasing.md). Earlier v0.1–v0.3 iterations were classroom prototypes; v1.0.0 is the first public release baseline.

## License

[MIT](LICENSE). Originally developed as a course project and now maintained as a community project. Codex assisted with implementation; contributors are responsible for reviewing and validating submitted changes.

## Scope

This version handles one course with standard weighted grading. It does not handle dropped scores, extra credit, curves, minimum exam pass rules, letter-grade boundaries, or automatic syllabus parsing. Solve one exam at a time and provide assumptions for other unfinished categories.
