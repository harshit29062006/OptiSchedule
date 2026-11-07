# Viva Question Bank: Scheduling Engine

## Design

**1. Why are faculty, room and course immutable value objects?**
Their identity is their code. Mutable keys in a `HashMap` break lookups, and the engine uses them as keys in its schedule map.

**2. Why is `TimeSlot` equal by day and period, not by start time?**
The source timetable has inconsistent times for the same period. Two entries for period 3 on Monday are the same slot even when one says 09:40 and the other 09:41.

**3. Why are checked exceptions used?**
A failed booking has to be handled by the caller. A checked exception makes the compiler enforce that, which an unchecked exception would not.

**4. What does `SchedulingService` do that `ConflictChecker` does not?**
It is the only entry point for the interface. It converts exceptions into result objects, so the UI does not need to catch checked exceptions, and it hides the internal classes so they can change without breaking callers.

**5. Why is there a `BookingSource` interface?**
The engine should not care where bookings come from. CSV and a database both implement it, so the engine is tested once and used with either.

## Conflict rules

**6. Why is same teacher, same room, same slot a merge, not a conflict?**
It is one teacher giving one combined lecture to several sections. Real timetables do this for electives. Treating it as a clash would reject about 500 valid bookings.

**7. What happens to the teacher's load when a combined class merges?**
It counts once. The teacher gives one period, not several.

**8. Why is a rejected booking not allowed to change anything?**
A half-applied booking would leave a load count or room state that does not match the bookings. The checks run first, and the state changes only when all of them pass.

**9. Is a section clash checked?**
No. Section clashes exist in the source data, so enforcing the rule would reject real bookings. This is decision D1, with the count from the analysis.

## Optimizer

**10. Why greedy and not an exact method?**
Timetabling is NP-hard, so an exact method can take unbounded time as the problem grows. Greedy placement finishes in predictable time, and the course scope does not require optimal results.

**11. How does the greedy order work?**
Classes with the fewest valid options are placed first, because they are the hardest to fit later.

**12. What does the multi-pass retry add?**
A fixed slot order can block a placement that another order would allow. Shuffled passes can find one. The best pass is kept.

**13. Why is the retry seeded?**
So results can be reproduced. A report number that changes on every run cannot be checked.

**14. Can the optimizer produce a worse result with more passes?**
No. The best pass is kept, and pass one is always among those tried.

**15. Why does the optimizer not backtrack?**
Backtracking can take exponential time, and the course does not need optimal results. The limitation is stated in the report.

**16. Why is load the first criterion and room fit the second?**
Teacher load is the scarcer resource. A class can use a larger room, but a teacher cannot be in two places. The trade-off is explicit, and the code states it.

## Load balancing

**17. How is load spread measured?**
Standard deviation of weekly load across the teacher pool. Idle teachers count as zero, so leaving someone unused raises the spread.

**18. Does balancing reduce total load?**
No. It moves classes between teachers. Total load is fixed by the classes being placed.

**19. Where do the eligible teachers come from?**
In the comparison they are simulated. Real timetables do not record who could cover a class. The report says this explicitly.

## Data

**20. How were the rows counted?**
The parser reads every row of the nine sheets, and the loader reports how many were read, loaded and rejected. The counts come from the logged load summary.

**21. What happens to rows with unresolved course codes?**
They are skipped and logged. They are not guessed, because a guessed course could place a class that does not exist.

**22. Why are PG rows treated with caution?**
The PG sheet stores section names in the room and teacher field, so some of those bookings are misclassified. This is documented, not hidden.

**23. What does the mid-semester `first/second` notation mean for the model?**
The engine records the first value only. The second half of the term is under-represented, and the report says so.

## Verification

**24. What does the property test check that unit tests do not?**
Unit tests check specific cases. Property tests check invariants across 200 random scenarios, so an invariant that fails only on an unusual input still gets caught.

**25. How do you know results are reproducible?**
`reproduce.sh` regenerates every number from fixed seeds. The clean-clone script then confirms the build passes on a fresh copy of the repository.
