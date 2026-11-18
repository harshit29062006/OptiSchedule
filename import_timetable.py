
import openpyxl
import mysql.connector
from getpass import getpass
from pathlib import Path

EXCEL_FILE = "TIME TABLE JULYTODEC2026.xlsx"

if not Path(EXCEL_FILE).exists():
    raise FileNotFoundError(f"Missing workbook: {EXCEL_FILE}")

print("Reading all timetable sheets...")

wb = openpyxl.load_workbook(EXCEL_FILE, data_only=True)

records = []

for ws in wb.worksheets:
    merged_map = {}

    for merged in ws.merged_cells.ranges:
        for row in ws[merged.coord]:
            for cell in row:
                merged_map[cell.coordinate] = str(merged)

    for row in ws.iter_rows():
        for cell in row:
            if cell.value is None:
                continue

            value = str(cell.value).strip()
            if not value:
                continue

            records.append((
                ws.title,
                cell.row,
                cell.column,
                None,
                None,
                None,
                value[:150],
                merged_map.get(cell.coordinate, "")[:150],
                "PENDING",
                f"Excel cell: {cell.coordinate}"
            ))

    print(f"Read {ws.title}")

print(f"\nTotal cells to import: {len(records)}")

print("\nEnter your MySQL connection details.")

password = getpass("MySQL password: ")

connection = mysql.connector.connect(
    host="localhost",
    user="root",
    password=password,
    database="optischedule"
)

cursor = connection.cursor()

# Clear only the staging table so rerunning this script
# does not create duplicate raw imports.
cursor.execute("TRUNCATE TABLE timetable_staging")

sql = """
INSERT INTO timetable_staging (
    source_sheet,
    source_row,
    source_column,
    section_header,
    day_code,
    period_number,
    course_raw,
    detail_raw,
    import_status,
    import_notes
)
VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
"""

batch_size = 1000

for i in range(0, len(records), batch_size):
    cursor.executemany(sql, records[i:i + batch_size])
    connection.commit()
    print(f"Imported {min(i + batch_size, len(records))}/{len(records)}")

cursor.execute("SELECT COUNT(*) FROM timetable_staging")
count = cursor.fetchone()[0]

cursor.close()
connection.close()

print("\nIMPORT COMPLETE!")
print(f"Records stored in MySQL staging: {count}")
print("Original Excel workbook remains unchanged.")
