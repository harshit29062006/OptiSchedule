# Week 4 Decisions

Each limitation from `WEEK3_STATUS.md` gets one decision, the evidence behind it, and the reason.

## D1. Section-clash check (one section in two rooms at one slot)

**Evidence:** `DecisionAnalysis` run on the cleaned nine-sheet dataset.

| Measure | Value |
|---|---|
| Slots with a section clash | [FILL FROM RUN] |
| Bookings a strict rule would reject | [FILL FROM RUN] |

**Decision:** [FILL: enforce / advisory warning / defer]

**Reason:** If the count is above zero, the source data itself contains the clashes. Enforcing the rule would then reject real bookings, which makes it an advisory warning until the spreadsheet has been checked. If the count is zero, enforce it.

## D2. Session index (a course meeting several times a week)

**Evidence:** Count of courses in the source that appear in more than one slot for the same section.

| Measure | Value |
|---|---|
| Sections with a course in multiple slots | [FILL FROM RUN] |

**Decision:** [FILL: add session index / keep duplicate-rejection and document / defer]

**Reason:** [FILL]

## D3. PG sheet room and teacher field

**Decision:** [FILL: parse with PG rule / exclude PG from conflict checks / document only]

**Reason:** [FILL]

## D4. Mid-semester changes

**Decision:** Documented limitation. Modelling a semester-half dimension changes `Booking` and `TimeSlot`, which affects every test and every caller. That is too large a change for Week 4.

**Reason:** The data is recorded, the limitation is stated, and the change is listed as future work.
