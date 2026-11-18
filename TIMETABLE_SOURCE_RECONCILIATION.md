# Source reconciliation note

The cleaned CSV is kept as a reference, but it is not used as the authority for class start periods where it expands a `LAB` continuation as a separate row.

Example from FIRST YEAR A:
- Monday P1 / 08:00: `AA8 = UES103P`
- The room/facility is in `AA9`.
- Monday P2 / 08:50: `AA10 = LAB` (continuation marker)

The practical starts at 08:00 and occupies P1–P2. The interface shows it at P1 and labels P2 as a continuation, instead of presenting P2 as a fresh practical.

The continuation handling is applied to any scheduled L/T/P cell immediately followed by a `LAB` marker in the same column. The source marker itself is not added as a duplicate course.
