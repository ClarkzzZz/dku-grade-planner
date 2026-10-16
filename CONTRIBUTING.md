# Contributing to DKU Grade Planner

Everyone is welcome to contribute. Issues and pull requests may be written in English or Chinese. No classroom membership or assigned module ownership is required.

## Development

1. Fork the repository and create a branch for one focused change.
2. Install JDK 21 or newer. Check both `java -version` and `javac -version`.
3. Run `bash scripts/test.sh` on macOS/Linux, or `powershell -ExecutionPolicy Bypass -File scripts/test.ps1` on Windows.
4. Run `bash scripts/run.sh` or `powershell -ExecutionPolicy Bypass -File scripts/run.ps1` to inspect UI changes.

No Maven, Gradle, external dependencies, or account configuration is required. See [architecture](docs/architecture.md) before changing calculation or persistence behavior.

## Issues and pull requests

For bugs, include OS, Java version, app version, steps, expected behavior, and actual behavior. Use invented grades and remove names, IDs, local paths, and personal information from screenshots or XML. Discuss large features or grading-rule changes in an issue first.

Keep pull requests focused. Explain the problem, resulting behavior, and validation performed. Add meaningful regression coverage for calculation/storage fixes. Update both READMEs when user-facing instructions change. Keep built-in UI text in English and preserve user-entered names.

Preserve blank-versus-zero semantics, explicit prediction assumptions, upward rounding of required scores, and existing XML version 1 files. Changes to the format require a compatibility plan. Do not commit `build/`, credentials, or personal course data; use `local-data/` for local files.

Use the [validation checklist](docs/validation.md) for relevant manual checks. Report checks you did not run; do not present planned CI or manual acceptance as completed verification. Maintainers review changes before merging; contributors do not publish release tags.

## License and collaboration

Contributions are submitted under the repository's [MIT license](LICENSE). Submit work you have permission to share, retain existing attribution, and treat other contributors respectfully. AI-assisted changes follow the same review and validation requirements as all other contributions. Describe generated changes that materially affect reviewer understanding.
