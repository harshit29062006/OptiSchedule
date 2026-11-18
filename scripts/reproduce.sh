#!/usr/bin/env bash
# Regenerates every number used in the report from fixed seeds.
# Usage: scripts/reproduce.sh [output-folder]
# Run from the project root. Output goes to results/ by default.
set -euo pipefail

OUT="${1:-results}"
mkdir -p "$OUT"

echo "[1/5] Compiling"
mvn -q -DskipTests compile

echo "[2/5] Running the test suite"
mvn test > "$OUT/tests.log" 2>&1 || true
grep -E "Tests run:.*Fail" "$OUT/tests.log" | tail -1 > "$OUT/tests_summary.txt" || true

echo "[3/5] Optimization runs (three fixed seeds)"
for SEED in 42 7 2024; do
  mvn -q exec:java \
    -Dexec.mainClass=com.university.scheduler.engine.OptimizationRunDriver \
    -Dexec.args="src/main/resources/data/all_sheets_cleaned.csv 20 $SEED" \
    > "$OUT/optimization_seed_$SEED.log" 2>&1
  sed -n '/OPTIMIZATION RUN SUMMARY/,/=====*$/p' "$OUT/optimization_seed_$SEED.log" \
    > "$OUT/optimization_summary_$SEED.txt"
done

echo "[4/5] Faculty balance comparison (seed 42, 40 classes)"
mvn -q exec:java \
  -Dexec.mainClass=com.university.scheduler.engine.FacultyBalance \
  -Dexec.args="src/main/resources/data/all_sheets_cleaned.csv 40 42" \
  > "$OUT/balance.log" 2>&1
sed -n '/FACULTY LOAD BALANCE COMPARISON/,/=====*$/p' "$OUT/balance.log" \
  > "$OUT/balance_summary.txt"

echo "[5/5] Section-clash analysis"
mvn -q exec:java \
  -Dexec.mainClass=com.university.scheduler.engine.DecisionAnalysis \
  -Dexec.args="src/main/resources/data/all_sheets_cleaned.csv" \
  > "$OUT/decision_analysis.log" 2>&1
sed -n '/SECTION-CLASH ANALYSIS/,$p' "$OUT/decision_analysis.log" \
  > "$OUT/decision_summary.txt"

echo "[6/6] Capacity sensitivity (placeholder vs raised)"
mvn -q exec:java \
  -Dexec.mainClass=com.university.scheduler.engine.SensitivityRun \
  -Dexec.args="src/main/resources/data/all_sheets_cleaned.csv 20 42 200" \
  > "$OUT/sensitivity.log" 2>&1
sed -n '/CAPACITY SENSITIVITY/,/=====*$/p' "$OUT/sensitivity.log" \
  > "$OUT/sensitivity_summary.txt"

cat <<EOF

Done. Results in $OUT/:
  tests_summary.txt        test count and failures
  optimization_summary_*   one summary per seed (42, 7, 2024)
  balance_summary.txt      naive vs balanced comparison
  decision_summary.txt     section-clash evidence for D1

Copy the figures from these files into ENGINE_REPORT.md and WEEK4_STATUS.md.
EOF
