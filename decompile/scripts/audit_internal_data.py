import subprocess

def adb(cmd):
    full_cmd = f"adb -s 192.168.1.2:5555 shell \"su -c '{cmd}'\""
    res = subprocess.run(full_cmd, shell=True, capture_output=True, text=True)
    return res.stdout.strip()

print("--- DATABASES ---")
dbs = adb("ls /data/data/com.truecaller/databases").split()
for db in dbs:
    if db.endswith("-wal") or db.endswith("-shm") or db.endswith("-journal"):
        continue
    db_path = f"/data/data/com.truecaller/databases/{db}"
    tables = adb(f"sqlite3 {db_path} .tables").split()
    non_empty = []
    for t in tables:
        cnt = adb(f"sqlite3 {db_path} \\\"SELECT count(*) FROM {t};\\\"")
        if cnt and cnt.isdigit() and int(cnt) > 0:
            non_empty.append(f"{t} ({cnt})")
    if non_empty:
        print(f"\n[{db}]")
        for ne in non_empty:
            print(f"  - {ne}")

print("\n--- SHARED PREFS FILES ---")
prefs = adb("ls /data/data/com.truecaller/shared_prefs").split()
print("Total shared_prefs files:", len(prefs))
for p in prefs:
    lines = adb(f"wc -l /data/data/com.truecaller/shared_prefs/{p}").split()[0]
    print(f"  {p} ({lines} lines)")

