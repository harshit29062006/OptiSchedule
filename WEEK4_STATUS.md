# Week 4 Status: Member B (Core Logic)

## Delivered

| Day | Deliverable | Status |
|---|---|---|
| 1 | Section-clash analysis; decision record `WEEK4_DECISIONS.md` | Code done; fill D1 and D2 from the run |
| 2 | Log4j2 configuration; logging in loader and optimizer | Done |
| 3 | Versioned snapshot save and load; CSV export; round-trip tests | Done |
| 4 | Property tests: 200 seeds, eight invariants, regression list | Done; regression list empty until a sweep fails |
| 5 | `BookingSourceContractTest`; `ENGINE_API.md` | Done; sign-off by A and C pending |
| 6 | `reproduce.sh`; `ENGINE_REPORT.md`; `VIVA_QUESTIONS.md` | Done; figures to fill from `results/` |
| 7 | `verify_clean_clone.sh`; `DEMO_SCRIPT.md`; this file; tag | Done; tag to create after verification |

## Verification

Run these in order, and record each result below.

1. `mvn test`: record the total count and failures.
2. `scripts/reproduce.sh`: produces `results/`. Copy the figures into the report.
3. `scripts/verify_clean_clone.sh <repo> `: must print `CLEAN CLONE BUILD PASSED`.

| Check | Result |
|---|---|
| Test count | [FILL] |
| Test failures | [FILL] |
| Clean clone passes | [FILL: yes / no] |
| Property sweep regression seeds recorded | [FILL: none / list] |

## Release

After the clean clone passes, tag and push:

```bash
git add -A
git commit -m "Week 4: logging, snapshots, CSV export, property tests, API sign-off, report and release scripts"
git tag -a v1.0 -m "Scheduling engine release 1.0"
git push origin <branch> --follow-tags
```

## Open items

| Item | Owner | Status |
|---|---|---|
| Confirm `ENGINE_API.md` | A and C | [PENDING] |
| Implement `JdbcBookingSource` and extend the contract test | A | [PENDING, if A has a database layer] |
| Decide D1 (section clash) and D2 (session index) | B with the timetable owner | [PENDING] |
| Replace placeholder faculty limit and room capacity with real values | Timetable owner | [PENDING] |
| Check the PG sheet against the Excel and decide D3 | B | [PENDING] |
| Fill every `{{...}}` placeholder in `ENGINE_REPORT.md` and `DEMO_SCRIPT.md` | B | [PENDING, from `results/`] |

## Known limitations

Recorded in `ENGINE_REPORT.md` section 9. None of them is hidden in the code.
