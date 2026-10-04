# Contributing

Team workflow for the CAB302 Biodex project.

## Branches and pull requests

- Work on a branch, never directly on `main`. Existing branches use `Name-Feature` or `feature/<name>` style names.
- Open a pull request into `main` and fill in the template.
- `main` stays green: CI builds the project and runs the full test suite on every pull request and push to `main`.

## Build and test

| Task | Windows | macOS / Linux |
|---|---|---|
| Build and run all tests | `build.bat` | `./build.sh` |
| Run the app | `loader\run.bat` | `loader/run.sh` |
| Tests only | `mvn test` | `mvn test` |

JDK 17 or newer. Maven is not required, the bundled wrapper in `loader/` handles it.

## Commits

- Keep each commit scoped to one change.
- Short imperative summaries. Conventional prefixes (`feat:`, `fix:`, `docs:`) are welcome.

## UI changes

Check both light and dark themes before opening the pull request.
