# DKU Grade Planner

**English** | [Chinese README](README.zh-CN.md)

Version 0.3: an English-language course grade and target exam score calculator built with **Java 21 + Swing** for a three-person COMPSCI 201 project. No Maven, Gradle, database, or third-party library is required.

## Run

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

Windows, after building:

```powershell
javac --release 21 -encoding UTF-8 -cp build/classes -d build/test-classes src/test/java/edu/dku/gradeplanner/GradePlannerTest.java
java -Djava.awt.headless=true -cp "build/classes;build/test-classes" edu.dku.gradeplanner.GradePlannerTest
```

Tests cover calculations, edge cases, locale defaults, and XML storage. See the [validation record](docs/validation.md) for executed checks and remaining manual acceptance steps.

## Code and Team Handoff

| Module | Files | Suggested owner |
|---|---|---|
| Models and calculations | Course, GradeCategory, GradeEntry, GradeCalculator | A |
| Swing interface | MainFrame, App | B |
| Storage and tests | CourseStorage, GradePlannerTest | C |

Lists preserve categories and entries, a map looks up predictions by stable category IDs, and sets validate duplicate names and IDs. See the [three-person plan](docs/team-plan.md).

## Version 0.3

The app now starts with a blank course. The application heading is fixed, while the user-editable course name is shown separately. The Load Demo action, production demo-data class, and bundled example XML have been removed. Calculation fixtures live only in test sources and are excluded from the application JAR.

Built-in text and controls remain in English, with English and Chinese READMEs. XML field names and grading rules remain compatible with existing saved courses. User-entered names are preserved.

## Scope

This version handles one course with standard weighted grading. It does not handle dropped scores, extra credit, curves, minimum exam pass rules, letter-grade boundaries, or automatic syllabus parsing. Solve one exam at a time and provide assumptions for other unfinished categories.

Codex assisted with implementation. Team members should understand the code, test it themselves, and disclose AI use according to the full course policy. Suggested ownership in the plan does not mean each student has already authored those modules.
