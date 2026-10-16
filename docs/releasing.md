# Release checklist

The first public release is planned as **v1.0.0**. Preparation does not publish a release. `VERSION` is the application version; XML format version 1 remains independent.

## Before tagging

1. Review changes and the MIT license; merge the intended release commit through a pull request.
2. Run the commands in [validation](validation.md), inspect the JAR, and complete the manual acceptance checklist. Record OS, JDK, date, and actual outcomes. Resolve release-blocking failures.
3. Confirm GitHub Actions passes all six OS/JDK combinations on the release commit. Workflow configuration alone is not a passing result. Ensure Actions is enabled in the repository.
4. Make `VERSION`, `CHANGELOG.md`, both READMEs, and `docs/releases/v1.0.0.md` consistent. Date the changelog when publication is ready. Use repository links at the tag in release notes so they work in the GitHub release view.
5. Check that the source contains no credentials or personal grade data and that build outputs are ignored.

## Draft and publication

When the checks are complete, a maintainer creates and pushes an annotated `v1.0.0` tag on the reviewed commit. Pushing this tag triggers `.github/workflows/release.yml`: it reruns the full CI matrix, checks the tag against `VERSION` and the release-notes filename, builds with JDK 21, and creates a **draft** release.

The draft attaches `dku-grade-planner.jar`, `SHA256SUMS`, and `LICENSE`. GitHub provides source archives. The workflow uses the repository's built-in token with write access only in its draft job; no personal access token is needed. It does not publish automatically.

Download the draft JAR, compare its SHA-256 checksum, and test `java -jar dku-grade-planner.jar` on a graphical desktop. Inspect notes and assets, then publish the draft in GitHub when acceptance is complete. Do not move a published tag; fix problems in a new patch release. For later releases, update `VERSION` and provide matching `docs/releases/vX.Y.Z.md` notes before tagging.

## Workflow references

The workflow follows GitHub's [reusable workflow documentation](https://docs.github.com/en/actions/how-tos/reuse-automations/reuse-workflows) and the [GitHub CLI release-create options](https://cli.github.com/manual/gh_release_create), including `--draft` and `--verify-tag`.
