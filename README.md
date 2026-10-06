# DKU Grade Planner

**English** | [Chinese README](README.zh-CN.md)

Version 0.2: an English-language course grade and target exam score calculator built with **Java 21 + Swing** for a three-person COMPSCI 201 project. No Maven, Gradle, database, or third-party library is required.

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
- Prompt for unsaved grade changes; keep current data when loading fails.

## Quick Demo

The initial data is fictional. Category weights are Homework 10%, Quiz 20%, Midterm 25%, Final project 15%, and Final exam 30%.

1. Select Homework. HW1 is 8/10, HW2 is 90/100, and HW3 is ungraded. The equal-weight average is 85.
2. Click **Edit Category**, choose **Total Points**, and the average becomes approximately 89.09. Switch back if desired.
3. Select **Final exam** under **Exam to Solve**. Double-click the last column of the prediction table and enter Homework 90, Quiz 85, and Final project 90. Midterm is finalized and uses the actual score of 80.
4. With a target of 85, the required final exam score is 85.00. Change the target to 95: it is unreachable, and the highest projected overall grade is 89.50.
5. Click **Save** to create XML. Reopening restores actual entries, weights, and order. Predictions and the target are temporary and are not saved.

For a new course, add categories and then grade entries. The exam to solve must have **positive weight, exactly one ungraded entry, and an unfinished status**. Once all entries are graded, finalize the category through **Edit Category** to include its contribution. Reopen a finalized category before adding an ungraded entry.

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
| Storage, demo data, tests | CourseStorage, DemoData, GradePlannerTest | C |

Lists preserve categories and entries, a map looks up predictions by stable category IDs, and sets validate duplicate names and IDs. See the [three-person plan](docs/team-plan.md).

## Version 0.2

All built-in application text, validation messages, demo names, and source comments are in English. Swing's built-in confirmation controls and file chooser labels use English as well. This README and its Chinese counterpart document the same application.

XML field names, grading rules, and saved-data compatibility are preserved. Existing user-entered names remain as entered; the app does not translate personal data.

## Scope

This version handles one course with standard weighted grading. It does not handle dropped scores, extra credit, curves, minimum exam pass rules, letter-grade boundaries, or automatic syllabus parsing. Solve one exam at a time and provide assumptions for other unfinished categories.

Codex assisted with implementation. Team members should understand the code, test it themselves, and disclose AI use according to the full course policy. Suggested ownership in the plan does not mean each student has already authored those modules.
