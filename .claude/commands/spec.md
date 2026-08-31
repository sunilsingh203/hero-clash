---
description: Generate a feature spec from a README
---
Read the README at $ARGUMENTS/README.md describing this feature, along with the
project's CLAUDE.md for context.

Write a spec.md file in the same folder covering:
- Feature summary and goal
- Functional requirements (numbered, testable)
- API / WebSocket message or method signature changes
- Data model changes
- Edge cases and error handling
- Out of scope

Do not write any implementation code yet. Stop after writing spec.md and
summarize it for me to review.