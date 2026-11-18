
import openpyxl
import csv
import os
from collections import Counter

EXCEL_FILE = "TIME TABLE JULYTODEC2026.xlsx"
OUTPUT_FILE = "timetable_all_cells.csv"
REPORT_FILE = "timetable_inspection_report.txt"

if not os.path.exists(EXCEL_FILE):
    raise FileNotFoundError(
        f"Cannot find {EXCEL_FILE}. "
        "Put the Excel workbook in the project root folder."
    )

print("Reading Excel workbook...")

workbook = openpyxl.load_workbook(
    EXCEL_FILE,
    data_only=True
)

total_cells = 0
sheet_reports = []

with open(OUTPUT_FILE, "w", newline="", encoding="utf-8-sig") as csvfile:

    writer = csv.writer(csvfile)

    writer.writerow([
        "source_sheet",
        "cell_address",
        "row_number",
        "column_number",
        "cell_value",
        "merged_range"
    ])

    for sheet in workbook.worksheets:

        print(f"Processing: {sheet.title}")

        count = 0
        merged_ranges = list(sheet.merged_cells.ranges)

        # Map each non-empty cell to its merged range, if any.
        merged_map = {}

        for merged_range in merged_ranges:
            for row in sheet[merged_range.coord]:
                for cell in row:
                    merged_map[cell.coordinate] = merged_range.coord

        for row in sheet.iter_rows():

            for cell in row:

                if cell.value is None:
                    continue

                value = str(cell.value).strip()

                if not value:
                    continue

                writer.writerow([
                    sheet.title,
                    cell.coordinate,
                    cell.row,
                    cell.column,
                    value,
                    merged_map.get(cell.coordinate, "")
                ])

                count += 1
                total_cells += 1

        sheet_reports.append({
            "sheet": sheet.title,
            "rows": sheet.max_row,
            "columns": sheet.max_column,
            "merged_ranges": len(merged_ranges),
            "non_empty_cells": count
        })

with open(REPORT_FILE, "w", encoding="utf-8") as report:

    report.write("OPTISCHEDULE TIMETABLE INSPECTION REPORT\n")
    report.write("=" * 55 + "\n\n")

    report.write(f"Total sheets: {len(workbook.worksheets)}\n")
    report.write(f"Total non-empty cells: {total_cells}\n\n")

    for item in sheet_reports:

        report.write(f"Sheet: {item['sheet']}\n")
        report.write(f"Rows: {item['rows']}\n")
        report.write(f"Columns: {item['columns']}\n")
        report.write(f"Merged ranges: {item['merged_ranges']}\n")
        report.write(
            f"Non-empty cells: {item['non_empty_cells']}\n"
        )
        report.write("-" * 40 + "\n")

print("\nINSPECTION COMPLETE!")
print(f"Sheets processed: {len(workbook.worksheets)}")
print(f"Non-empty cells: {total_cells}")
print(f"CSV generated: {OUTPUT_FILE}")
print(f"Report generated: {REPORT_FILE}")
