# Three-Person Project Plan: Course Grade and Target Score Calculator

Planning reference dated October 6, 2026. A, B, and C are placeholders for the three team members. This document describes the agreed scope and suggested handoff; actual implementation status is in the README. It is not evidence of individual authorship.

## 1. Goal

Build a Java GUI for DKU students to enter syllabus grading weights, record individual homework and quiz scores, review category averages, and calculate the exam score needed for a target course grade.

The first version manages one course with local files. No accounts, dashboard integration, PDF parsing, or network services.

Sample weights from the provided screenshot: Homework 10%, Quiz 20%, Midterm 25%, Final project 15%, Final exam 30%. These are a sample configuration; the screenshot did not define within-category aggregation.

## 2. Scope

Required workflow: create/edit categories and weights; add/edit/delete entries; distinguish blank scores and zero; support equal-weight and total-points averages; mark completed categories; enter a target and predictions; solve one unknown exam; explain reachability and minimum/maximum projected totals; save/load actual data.

Possible later additions: report export, CSV imports, multiple courses, trend charts. Complete and test the main workflow before adding them.

Exclude dropped scores, extra credit, curves, letter-grade thresholds, mandatory exam pass rules, automated syllabus parsing, online accounts, and simultaneous optimization of multiple unknown scores.

## 3. Shared Calculation Rules

- Each entry has earned points and positive maximum points. A blank earned score means ungraded; zero is graded.
- Equal weighting averages graded entry percentages. Total Points divides total earned points by total possible points across graded entries.
- Example: 8/10 and 90/100 produce 85 with equal weighting and approximately 89.09 with total points.
- An empty or wholly ungraded category has no average, rather than an average of zero.
- An unfinished category's current average is provisional. Only finalized categories count toward finalized contribution.
- Finalized contribution is the sum of final category averages multiplied by their weight fractions. The finalized weighted average divides that contribution by the total finalized weight; no positive finalized weight means no average.
- A category may be finalized only when it contains at least one entry and all entries are graded.
- Solve exactly one unfinished, positive-weight category with exactly one ungraded entry. Other finalized categories use actual grades; unfinished positive-weight categories need explicit expected final averages.
- Predictions do not modify actual scores. Copying current averages is an explicit user action.
- Required exam score = (target minus other contributions) / exam weight fraction.
- Example: expected Homework90, Quiz85, Project90, actual Midterm80 yields other contributions59.5. Target85 and Final exam30% require85.
- Above100 means unreachable under the assumptions. At or below0 means zero suffices, not that taking the exam is optional.
- Keep internal precision. Display ordinary numbers to two places and round a displayed required minimum upward.

## 4. Responsibilities

### A: Models and Calculation

Own Course, GradeCategory, GradeEntry, aggregation rules, contribution calculations, target solving, and result classification. Deliver model fields and method contracts, independently runnable calculation examples, formula explanations, and calculation tests. Present design and algorithms.

The calculator must not depend on Swing controls or file dialogs.

### B: GUI and User Workflow

Own the main window, category and entry controls, expected-grade inputs, result display, and integration with calculation/storage services. Deliver a usable full workflow, input/empty-state messages, and final screenshots. Operate the live demo and explain interaction design.

Inputs must be editable without changing source. Edits invalidate stale results; deletion and cancellation must behave consistently.

### C: Storage, Validation, and Integration

Own the versioned file format, save/load, model validation, boundary testing, startup instructions, and clean-directory packaging checks. Deliver storage and validation contracts, reproducible examples, test records, and an actual issue log. Present testing and persistence.

Load a temporary model and validate it before replacing the current course. Failed loads must preserve current data. C should implement code rather than only writing slides.

### Shared Work

Confirm syllabus aggregation rules and the full AI policy. Review and integrate each other's code. Each member writes their report section, participates in presentation/Q&A, and understands the whole workflow. Record real contributions rather than assuming equal percentages.

## 5. Contracts Before Parallel Development

- Use stable string IDs; renaming does not change identity.
- Store weights as percentages from0 to100. The total must be100 for target calculations; editing and saving an incomplete draft is allowed.
- Scores range from0 to maximum points; maximum points must be positive.
- Store an ungraded score as null, never as zero.
- Expected averages are percentages keyed by category ID, separate from actual entries.
- Reject duplicate category names and IDs and duplicate entry IDs within a category.
- Actual finalized grades override any prediction for the same category.
- Return or throw understandable validation failures including the relevant field/category.
- GUI calls the shared calculator rather than duplicating formulas.

Use the actual implemented interfaces in GradeCalculator and CourseStorage as the final contracts. Earlier draft signatures in discussion were proposals, not required method names.

Storage uses JDK Properties XML in UTF-8 with versioned fields, preserving IDs, order, scores, and completion state. Do not save derived averages. Test special characters, non-Latin names, blank scores, and zero. Predictions are temporary in the current version.

## 6. GUI Layout

One main window has: course name and file controls; category table with weights/averages/status; individual entry table with score/maximum/percentage/status; target exam selector and expected averages; result and assumptions.

Use layout managers. Keep predictions visibly separate from actual grades. Editing a score, weight, target, or assumption must invalidate the previous prediction. Category completion is an explicit action.

## 7. Data Structures

| Structure | Purpose |
|---|---|
| List of categories | Preserve display order and support insertion, deletion, and traversal |
| List of entries | Support a variable number of assignments/quizzes and aggregation |
| Map keyed by category ID | Look up expected averages without relying on changing row indices |
| Set | Detect duplicate identities/names |

Do not add queues, trees, or heaps without an actual feature needing them. Explain why IDs are more stable than GUI row numbers. Category aggregation traverses the graded entries and grows linearly with entry count.

The supplied project brief did not explicitly require every structure to be implemented from scratch; check any additional course instructions.

## 8. Suggested Schedule

| Date | Deliverable |
|---|---|
| Oct6 | Agree on scope, formulas, interfaces, UI sketch, validation, and owners |
| Oct7 | Independent model/calculator, GUI skeleton, and storage implementations |
| Oct8 | Integrate one complete multi-entry grading and target-solving workflow |
| Oct9 | Complete basic editing, persistence, validation, assumptions, and boundaries |
| Oct10 | Cross-test modules, fix problems, record real issues, prepare UML/screenshots |
| Oct11 | Assemble report/slides and rehearse five-minute demonstration |
| Oct12 | Freeze features, extract the submission into a clean directory, verify startup |
| Oct13 | Final checks and upload; leave time for upload failures |

The brief gives Oct13 at23:50 for the application, and Oct13 without an explicit time for the report and slides. Check Canvas for the latter. Presentation is Oct15 during lab. Adjust work allocation to actual availability without removing report and testing time.

## 9. Essential Test Cases

| Case | Expected behavior | Owner |
|---|---|---|
| 8/10 and90/100, Equal Weight | 85 | A |
| Same entries, Total Points | Approximately89.09 | A |
| Zero plus blank | Include zero, exclude blank | A/B |
| No graded entries | N/A, no division by zero | A |
| Zero maximum, negative or excess score | Reject with explanation | C/B |
| Weights90 or110 | Edit/save allowed; target calculation blocked | C/B |
| Target85, other contributions59.5, exam30% | Require85 | A |
| Target95, same assumptions | Unreachable; maximum89.5 | A |
| Target55, same assumptions | Zero sufficient, assumptions explicit | A/B |
| Exam weight0 | Not eligible; no division by zero | A/C |
| Missing expected average | Request it; no silent zero/current-average substitution | A/B |
| Finalize a category with blank scores | Reject | C/B |
| Prediction for finalized category | Use actual average | A |
| Edit/delete/change aggregation | Update values and invalidate old prediction | B |
| Save and reload non-Latin names, zero, blank | Preserve values and order | C |
| Corrupt or unsupported file | Preserve current course | C/B |
| All categories finalized | Show final grade rather than solve a completed exam | A/B |
| Extract ZIP into a different directory | Compile/run independently of original paths | C/all |

Keep an actual issue log with discovery date, symptom, reproduction, cause, fix, and regression result. Planned tests are not bugs encountered. The report requires at least two significant actual bugs/problems and how they were found and resolved; never invent development experiences.

## 10. Report and Presentation

Report:4–5 pages. B drafts goals/workflow/screenshots; A drafts UML, models, formulas, structures; C drafts testing, actual problems, persistence and packaging. All members contribute AI-use disclosure and collaboration reflection. The final report must describe implemented behavior.

Suggested five-minute presentation: B explains the need and demonstrates entries (1m40s); A explains aggregation and target solving (1m40s); C shows persistence/validation, testing and key areas without AI (1m20s); reserve20s for switching. B can operate the mouse throughout.

Presentation sequence: enter your own course data; compare two aggregation methods; show blank vs zero; enter90/85/90 predictions; solve target85; change target95; save and reload.

Prepare Q&A on missing scores, aggregation differences, multiple unknown grades, formula verification, data structures, assumptions, and excluded grading policies.

## 11. Submission Checklist

Include the complete Java source project, README with JDK/startup instructions, and dependency details. Avoid personal absolute paths. Extract the ZIP into another folder/computer and verify compilation and startup.

Use members' first names for X_Y_Z.zip and the matching PDF name. The PDF must cover design, data structures, testing with actual problems, AI use, and collaboration/contribution percentages if they differ. Slides must include key areas where the group did not use AI. One member submits all deliverables and the team verifies the uploaded versions.

The provided two-page brief references a Use of AI section that is absent; confirm the full policy from other course materials or the instructor. This plan does not grant permission beyond the course policy.

## 12. Next Team Discussion

Replace A/B/C with names; confirm available time, JDK21 and startup method; verify category aggregation; review completion/prediction semantics; agree on actual model/service contracts; appoint the submitter and daily integration check time.

Deliver one understandable, correct, persistable course before adding PDF parsing or complex grading policies. This plan is a team recommendation, not an additional instructor requirement.
