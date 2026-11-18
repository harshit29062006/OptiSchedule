"""Build student-facing weekly timetable entries from the actual Excel layout.

Each timetable panel has DAY / SR.NO / HOURS columns. A weekday is printed
vertically (e.g. M O N D A Y) alongside the 14 period rows; it is one day,
not six different weekdays. Course and room/faculty annotations occupy paired
rows. This parser detects each panel independently, including panels placed
side-by-side on the senior-year sheets.
"""
from openpyxl import load_workbook
from openpyxl.utils import get_column_letter
from pathlib import Path
import csv, re
from functools import lru_cache

BASE = Path(__file__).resolve().parents[1]
BOOK = BASE / 'TIME TABLE JULYTODEC2026.xlsx'
OUT = BASE / 'src/main/resources/data/weekly_timetable.csv'
DAY_NAMES = {'MONDAY':'Monday','TUESDAY':'Tuesday','WEDNESDAY':'Wednesday',
             'THURSDAY':'Thursday','FRIDAY':'Friday','SATURDAY':'Saturday'}
COURSE_RE = re.compile(r'[A-Z]{2,5}\d{2,4}[LTP]', re.I)

def txt(v):
    if v is None: return ''
    if hasattr(v, 'strftime'): return v.strftime('%H:%M')
    return str(v).strip().replace('\n',' ')

def time_text(v):
    # Preserve the source clock time, including AM/PM notation. The old parser
    # dropped the suffix and turned 6:00PM into 06:00 instead of 18:00.
    if v is None:
        return ''
    if hasattr(v, 'hour') and hasattr(v, 'minute'):
        return f'{v.hour:02d}:{v.minute:02d}'
    s = txt(v).strip().upper().replace('.', '')
    m = re.search(r'(\d{1,2}):(\d{2}):?\s*(AM|PM)?', s)
    if not m:
        return ''
    hour, minute = int(m.group(1)), int(m.group(2))
    suffix = m.group(3)
    if suffix == 'PM' and hour != 12:
        hour += 12
    elif suffix == 'AM' and hour == 12:
        hour = 0
    return f'{hour:02d}:{minute:02d}'

@lru_cache(maxsize=None)
def merged_end(ws,row,col):
    for rg in ws.merged_cells.ranges:
        if rg.min_row <= row <= rg.max_row and rg.min_col <= col <= rg.max_col:
            return rg.max_col
    return col

def header_panels(ws):
    """Find every DAY / serial / time triplet in the worksheet."""
    panels=[]
    for r in range(1,min(ws.max_row,15)+1):
        vals={c:txt(ws.cell(r,c).value).upper().replace('.','').replace(' ','') for c in range(1,ws.max_column+1)}
        days=[c for c,v in vals.items() if v=='DAY']
        for dc in days:
            serial=next((c for c in range(dc+1,min(dc+5,ws.max_column+1)) if vals.get(c,'').replace(' ','')=='SRNO'),None)
            hour=next((c for c in range((serial or dc)+1,min((serial or dc)+6,ws.max_column+1)) if vals.get(c,'') in ('HOUR','HOURS')),None)
            # Several official panels omit the 'SR NO' label but still put
            # period numbers in the column immediately after DAY.
            if hour and not serial: serial=dc+1
            if serial and hour:
                panels.append((r,dc,serial,hour))
    return panels

@lru_cache(maxsize=None)
def header_group(ws, header_row, col, suffix):
    # L/T/P headings are normally the three rows immediately above/including
    # the row containing DAY / SR.NO / HOURS.
    offset={'L':-2,'T':-1,'P':0}[suffix]
    hr=header_row+1 if ws.title.strip() == 'PG TIME TABLE' else header_row+offset
    if hr < 1: return ''
    # A group header is the merged header cell whose span contains this column.
    for c in range(1,ws.max_column+1):
        value=txt(ws.cell(hr,c).value)
        if value and c <= col <= merged_end(ws,hr,c):
            if value.upper() not in {'LECTURE','TUTORIAL','PRACTICAL','DAY','SR NO','SR.NO','HOURS','HOUR'}:
                return value
    # Fallback: closest non-empty group label to the left, but do not cross
    # the panel's time column (the caller only asks for course columns).
    candidates=[]
    for c in range(1,col+1):
        value=txt(ws.cell(hr,c).value)
        if value and value.upper() not in {'LECTURE','TUTORIAL','PRACTICAL','DAY','SR NO','SR.NO','HOURS','HOUR'}:
            candidates.append((c,value))
    return candidates[-1][1] if candidates else ''

def find_day(ws, day_col, period_rows):
    letters=''
    # The weekday is printed vertically in the DAY column, but the letter can
    # occur on either the period row or its annotation row depending on sheet.
    for rr in period_rows[:10]:
        for row in (rr, rr+1):
            letter=''
            for c in range(max(1,day_col),min(ws.max_column,day_col+3)+1):
                v=txt(ws.cell(row,c).value).upper()
                if len(v)==1 and v.isalpha():
                    letter=v
                    break
            if letter:
                letters += letter
                if letters in DAY_NAMES: return DAY_NAMES[letters]
    return ''

def main():
    wb=load_workbook(BOOK,data_only=True)
    records=[]
    for ws in wb.worksheets:
        for header_row,day_col,serial_col,hour_col in header_panels(ws):
            # Period rows are rows with a numeric serial and a recognizable time.
            period_rows=[]
            for r in range(header_row+1,ws.max_row+1):
                n=txt(ws.cell(r,serial_col).value)
                tm=time_text(ws.cell(r,hour_col).value)
                if n.isdigit() and tm:
                    period_rows.append((r,int(n),tm))
            # Partition at every period 1; each block is one weekday.
            starts=[i for i,(_,p,_) in enumerate(period_rows) if p==1]
            for si,start in enumerate(starts):
                end=starts[si+1] if si+1<len(starts) else len(period_rows)
                block=period_rows[start:end]
                if not block: continue
                day=find_day(ws,day_col,[x[0] for x in block])
                if not day:
                    # Some source panels have the day letters in an adjacent
                    # duplicate column; inspect the two columns before the serial.
                    for dc in range(max(1,day_col-2),day_col+1):
                        day=find_day(ws,dc,[x[0] for x in block])
                        if day: break
                if not day: continue
                for row,period,clock in block:
                    annotation_row=row+1
                    for col in range(hour_col+1,ws.max_column+1):
                        if ws.title.strip() == '4TH YEAR A' and 5 <= col < 23:
                            continue
                        raw_course=txt(ws.cell(row,col).value).upper().replace(' ','')
                        courses=COURSE_RE.findall(raw_course)
                        if not courses: continue
                        room=txt(ws.cell(annotation_row,col).value)
                        for course in courses:
                            suffix=course[-1]
                            group=header_group(ws,header_row,col,suffix)
                            if not group: continue
                            end_col=merged_end(ws, {'L':header_row-2,'T':header_row-1,'P':header_row}[suffix], col)
                            faculty=''
                            if end_col>col:
                                candidate=txt(ws.cell(annotation_row,end_col).value)
                                if candidate and candidate.upper() not in {'LAB','BREAK'}:
                                    faculty=candidate
                            shown_room='' if room.upper() in {'LAB','BREAK'} else room
                            records.append({'sheet_name':ws.title.strip(),'day':day,'period':period,
                                'time':clock,'group_name':group,'course':course,'room':shown_room,
                                'faculty':faculty,'source_cell':f'{get_column_letter(col)}{row}'})
    # Remove exact duplicates only; simultaneous subgroup sessions are retained.
    seen=set(); unique=[]
    for rec in records:
        key=tuple(rec.values())
        if key not in seen: seen.add(key); unique.append(rec)
    OUT.parent.mkdir(parents=True,exist_ok=True)
    fields=['sheet_name','day','period','time','group_name','course','room','faculty','source_cell']
    with OUT.open('w',newline='',encoding='utf-8-sig') as f:
        writer=csv.DictWriter(f,fieldnames=fields); writer.writeheader(); writer.writerows(unique)
    print(f'Panels detected: {sum(len(header_panels(s)) for s in wb.worksheets)}')
    print(f'Class entries extracted: {len(unique)}')
    for name in wb.sheetnames:
        print(f'{name.strip()}: {sum(x["sheet_name"]==name.strip() for x in unique)}')

if __name__=='__main__': main()
