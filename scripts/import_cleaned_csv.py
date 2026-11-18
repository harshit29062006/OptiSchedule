#!/usr/bin/env python3
"""Load the cleaned timetable CSV into the auditable raw-import table.

This deliberately does NOT invent faculty names, room identities, or L/T/P meanings.
Run schema.sql first. Requires: pip install mysql-connector-python
"""
import argparse, csv, os
import mysql.connector

COLUMNS = ["sheet", "day", "period", "start_time", "section", "course_code", "room_faculty_raw"]

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("csv_path", nargs="?", default="src/main/resources/data/all_sheets_cleaned.csv")
    ap.add_argument("--host", default=os.getenv("OPTISCHEDULE_DB_HOST", "localhost"))
    ap.add_argument("--port", type=int, default=int(os.getenv("OPTISCHEDULE_DB_PORT", "3306")))
    ap.add_argument("--database", default=os.getenv("OPTISCHEDULE_DB_NAME", "scheduler_db"))
    ap.add_argument("--user", default=os.getenv("OPTISCHEDULE_DB_USERNAME", "root"))
    args = ap.parse_args()
    password = os.getenv("OPTISCHEDULE_DB_PASSWORD", "")
    cn = mysql.connector.connect(host=args.host, port=args.port, database=args.database, user=args.user, password=password)
    inserted = reviewed = 0
    try:
        cur = cn.cursor()
        with open(args.csv_path, newline="", encoding="utf-8-sig") as f:
            reader = csv.DictReader(f)
            missing = set(COLUMNS) - set(reader.fieldnames or [])
            if missing: raise SystemExit(f"CSV missing required columns: {sorted(missing)}")
            sql = ("INSERT INTO timetable_import_rows "
                   "(source_sheet,source_line,day_raw,period_raw,start_time_raw,section_raw,course_code_raw,room_faculty_raw,parse_status,parse_notes) "
                   "VALUES (%s,%s,%s,%s,%s,%s,%s,%s,'REVIEW',%s) "
                   "ON DUPLICATE KEY UPDATE day_raw=VALUES(day_raw),period_raw=VALUES(period_raw),start_time_raw=VALUES(start_time_raw), "
                   "section_raw=VALUES(section_raw),course_code_raw=VALUES(course_code_raw),room_faculty_raw=VALUES(room_faculty_raw),parse_status='REVIEW',parse_notes=VALUES(parse_notes)")
            for line, row in enumerate(reader, start=2):
                vals = [row.get(c, "").strip() for c in COLUMNS]
                if not vals[0]: continue
                # source_line is CSV row number; source sheet + line uniquely preserves provenance.
                notes = "Raw row retained. Review room/faculty split and course suffix before normalization."
                cur.execute(sql, (vals[0], line, vals[1], vals[2], vals[3], vals[4], vals[5], vals[6], notes))
                inserted += 1
        cn.commit()
        cur.execute("SELECT COUNT(*) FROM timetable_import_rows")
        total = cur.fetchone()[0]
        print(f"Import complete: {inserted} CSV rows processed; {total} raw rows currently stored in {args.database}.")
        print("Rows are marked REVIEW intentionally; raw source values are preserved and no faculty/room identities were guessed.")
    except Exception:
        cn.rollback(); raise
    finally:
        cn.close()

if __name__ == "__main__": main()
