---
description: Review implemented changes against the spec and project conventions
---
Read $ARGUMENTS/spec.md, $ARGUMENTS/plan.md, and the project's CLAUDE.md.

Review the current diff (git diff against the base branch, or uncommitted
changes) for this feature:
- Does the implementation satisfy every requirement in spec.md?
- Were any tasks in plan.md skipped or only partially done?
- Check for bugs, unhandled edge cases from spec.md, and violations of
  conventions in CLAUDE.md
- Check test coverage: are the verification steps from plan.md actually
  backed by tests?
- Flag anything risky, unclear, or that deviates from the plan without a
  stated reason

Do not fix anything automatically. List findings as a numbered list, ordered
by severity (bug > missing requirement > convention violation > nitpick),
and wait for me to tell you which ones to fix.