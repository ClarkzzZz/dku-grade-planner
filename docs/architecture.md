# Architecture

DKU Grade Planner is a single-process Java 21 Swing desktop application. It uses only the JDK, with no server, network services, database, or third-party runtime dependencies.

```text
App → MainFrame → Course / GradeCategory / GradeEntry
                → GradeCalculator → models
                → CourseStorage   → models + local XML
GradePlannerTest → models + calculator + storage + locale configuration
```

## Components

| Component | Responsibility |
| --- | --- |
| `App` | Configure English application/Swing defaults and create the window on the Swing event dispatch thread |
| `MainFrame` | UI layout, table models, edits, selection, unsaved-change prompts, prediction input, file dialogs, and result formatting |
| `Course` | Immutable course name and ordered categories; category count and unique names/IDs |
| `GradeCategory` | Immutable weight, aggregation mode, completion state, ordered entries; entry limits and unique entry IDs |
| `GradeEntry` | Immutable earned/possible points; null earned means ungraded; bounded numeric input |
| `GradeCalculator` | Category averages, finalized contributions, target solving, reachability, and minimum-score rounding |
| `CourseStorage` | XML format version 1; validate loaded data and save via a temporary file followed by replacement |
| `GradePlannerTest` | Standalone headless test runner with test-only fixtures |

All Java files live under `src/main/java/edu/dku/gradeplanner/`; tests use the matching package under `src/test/java/`. Scripts compile with `--release 21`, clear compiled classes, and package only production classes into a runnable JAR. `VERSION` supplies its manifest version.

## State and data flow

The model records defensively copy their lists. UI edits construct replacement records rather than mutating model lists. Lists preserve display order; stable IDs identify categories through renames and selection changes. Sets enforce uniqueness; a map stores temporary predictions keyed by category ID.

`MainFrame` owns the active course, path, dirty flag, target, and prediction inputs. Actual grade edits mark the course dirty. Grade, target, and prediction edits invalidate old results. Predictions and targets are never saved. Model constructors enforce structural constraints; the calculator additionally requires weights totaling 100 and explicit assumptions before solving one eligible exam.

On open, `CourseStorage` loads and validates a temporary course before the UI replaces its active state. Failed loads keep the current course. Saves write versioned JDK Properties XML; atomic replacement is attempted, with a regular replacement fallback when the filesystem does not support atomic moves. Files over 2 MB are rejected on load. The XML format version is independent of the application version.

## Boundaries and extension points

Keep formulas and grading validation in the model/calculator, independent of Swing. Keep persistence out of calculations. Add regression cases to `GradePlannerTest` for changed grading or file behavior. Preserve version 1 compatibility or design a migration before changing storage fields.

`MainFrame` currently combines layout, controller behavior, and table models. File I/O runs on the event dispatch thread; large or slow filesystems can delay the UI. Extracting controllers/table models or using background I/O can be future focused improvements. There is no need to introduce a build framework just to contribute.

The first public release handles one course and one unknown exam at a time. Extra credit, dropped scores, curves, letter grades, and syllabus parsing are outside its current calculation model.
