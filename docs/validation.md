# Version 0.3 Validation Record

Date: October 6, 2026.

## Version 0.3 Changes

- Startup and New create an empty Untitled Course with no categories or scores.
- The fixed heading is DKU Grade Planner; the editable course name is displayed separately.
- The target field starts blank. Production demo data, the Load Demo action, and bundled XML are removed.
- Calculation fixtures remain in test sources only. Builds clear old compiled classes so deleted demo classes cannot remain in the JAR.

## Version 0.3 Verification

- Oracle JDK21.0.12.1 build and all44 calculation, storage, locale, and empty-course checks passed.
- Inspected the Java21 desktop startup: fixed application heading, separate Untitled Course label, empty category/entry/prediction tables, blank target, and onboarding instructions. No Load Demo action is present.
- Inspected JAR contents: no DemoData class or bundled course XML remains.

## Previous Executed Checks

- Version0.1 was compiled and tested on Oracle JDK21.0.12.1 using --release21:38 calculation/storage checks passed. A JDK25.0.3 cross-build also passed.
- Version0.2 compiled and ran on Oracle JDK21.0.12.1: all42 checks passed, both under normal defaults and with user.language=zh / user.country=CN. Four new checks cover application/Swing defaults and built-in Yes/Open labels.
- Launched the version0.2 preview with a Java21 runtime. Inspected English main panels at the default window size and confirmed an English validation prompt with OK and an English file chooser with Open/Cancel.
- Source scanning verifies that all built-in text in source, scripts, and English documentation is free of Chinese characters. Non-Latin storage fixtures remain encoded with Java Unicode escapes.
- XML field names and file version remain unchanged so existing saved course data is compatible. User-entered names are preserved rather than translated.

Version0.1 GUI checks: launched the Swing window, inspected the main panels and sample values, clicked Calculate with missing predictions, and observed a category-specific error prompt.

The automated macOS input tool could not reliably enter text into Swing fields during those checks. Full manual input and file chooser workflows remain acceptance tasks; Windows/Linux scripts have not been executed on those systems.

## Manual Acceptance Checklist

1. Add/edit/delete homework and quiz entries; cancelling an edit must preserve data.
2. For8/10 and90/100, verify Equal Weight85 and Total Points approximately89.09. Zero counts; blank does not.
3. An unfinished entry prevents finalization. Reopen a finalized category before adding ungraded work.
4. Enter expected averages90/85/90 and target85: Final exam requires85. Target95 is unreachable, with maximum89.5.
5. Switch the exam while editing a prediction; values must stay mapped to category IDs.
6. Edits to grades, weights, targets, or predictions invalidate the old result.
7. Save, restart, and reload: order, non-Latin names, blank, and zero round-trip; predictions are not persisted.
8. When closing/creating a new course with changes, Cancel keeps them and Save writes them.
9. Loading a damaged file reports the error and preserves the current course.
10. Build and run from a clean directory using the README.
11. On a Chinese-language system, verify English buttons, tables, results, warnings, confirmation buttons, and file chooser labels.
12. Check table headings and longer English messages at the minimum window size.

This record is not the final course report. Planned tests are not evidence of actual bugs; keep an honest issue log separately.
