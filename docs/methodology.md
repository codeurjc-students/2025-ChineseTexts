# Methodology

ChineseReads is developed following an **iterative and incremental** process inspired by Agile principles and practices from XP (Extreme Programming) and Kanban.

## Key Practices

- **Iterative development** — the application is built in small increments, each adding working functionality.
- **Continuous testing** — automated tests are written alongside the code to catch regressions early.
- **Simple design** — the codebase is kept as simple as possible while meeting requirements.
- **Refactoring** — code is improved continuously as new understanding is gained.

## Task Management

- **GitHub Issues** — each feature, bug, or task is tracked as a GitHub Issue with a description and labels.
- **GitHub Projects** — a Kanban board with columns (Backlog, In Progress, In Review, Done) gives a visual overview of progress.

## Git Workflow

- `main` — stable production branch. Only receives merges from feature branches via reviewed Pull Requests.
- `feature/<name>` — one branch per feature or fix.
```bash
git checkout -b feature/my-feature
# ... develop and commit ...
git push -u origin feature/my-feature
# Open Pull Request → Review → Merge into main
git checkout main && git pull
```

## Commit Convention

Commit branches follow the conventional commits format:

| Prefix | Meaning |
|---|---|
| `feat:` | New feature |
| `fix:` | Bug fix |
| `docs:` | Documentation |
| `test:` | Tests |
| `refactor:` | Code refactoring without behaviour change |
| `chore:` | Build, config, dependencies |