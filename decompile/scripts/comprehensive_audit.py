import subprocess
import json

def run_adb(cmd):
    full_cmd = ["adb", "-s", "192.168.1.2:5555", "shell", f"su -c '{cmd}'"]
    res = subprocess.run(full_cmd, capture_output=True, text=True)
    return res.stdout.strip()

def run_adb_raw(cmd):
    full_cmd = ["adb", "-s", "192.168.1.2:5555"] + cmd
    res = subprocess.run(full_cmd, capture_output=True, text=True)
    return res.stdout.strip()

print("=" * 60)
print("1. RUNNING PROCESS & NETWORK SOCKET AUDIT")
print("=" * 60)
pids = run_adb("pidof com.truecaller com.truecaller:pushservice")
print("PIDs:", pids)

# Check active network sockets for Truecaller UID/PID
uid = run_adb("dumpsys package com.truecaller | grep userId= | head -n 1").split("=")[-1]
print(f"UID: {uid}")

netstat = run_adb(f"ss -tupn | grep {uid} || netstat -tupn | grep {uid} || echo 'No active sockets'")
print("Active sockets for UID:")
print(netstat)

print("\n" + "=" * 60)
print("2. RUNNING BACKGROUND SERVICES")
print("=" * 60)
services = run_adb("dumpsys activity services com.truecaller | grep -E 'ServiceRecord|app=ProcessRecord' || echo 'No active services'")
print(services)

print("\n" + "=" * 60)
print("3. JOBSCHEDULER & ALARMS AUDIT")
print("=" * 60)
jobs = run_adb("dumpsys jobscheduler | grep -A 2 'JOB #.*com.truecaller' || echo 'No scheduled jobs'")
print("JobScheduler jobs:")
print(jobs)

alarms = run_adb("dumpsys alarm | grep -E 'com.truecaller' | head -n 25 || echo 'No alarms'")
print("Alarms:")
print(alarms)

print("\n" + "=" * 60)
print("4. ALL SQLITE DATABASES & ROW COUNTS AUDIT")
print("=" * 60)
db_list = run_adb("ls /data/data/com.truecaller/databases/ | grep -v -E '(-journal|-shm|-wal)'").splitlines()
for db in db_list:
    db_path = f"/data/data/com.truecaller/databases/{db}"
    tables = run_adb(f"sqlite3 {db_path} '.tables' 2>/dev/null").split()
    print(f"\nDB: {db} ({len(tables)} tables)")
    for t in tables:
        count = run_adb(f"sqlite3 {db_path} 'SELECT count(*) FROM {t};' 2>/dev/null")
        if count != "0" and count != "":
            # Check if this table has sensitive names
            is_suspicious = any(w in t.lower() for w in ['event', 'log', 'analytic', 'survey', 'track', 'metric', 'stat', 'profile', 'history', 'sync', 'presence', 'push', 'ad', 'campaign'])
            prefix = "[ALERT] " if is_suspicious else "        "
            print(f"{prefix}{t}: {count} rows")
        else:
            if any(w in t.lower() for w in ['event', 'log', 'analytic', 'survey', 'track', 'metric', 'profile_view']):
                print(f"  [OK:0] {t}: 0 rows")

print("\n" + "=" * 60)
print("5. FILESYSTEM STORAGE AUDIT (/data/data/com.truecaller/files/)")
print("=" * 60)
files_out = run_adb("ls -la /data/data/com.truecaller/files/")
print(files_out)

print("\n" + "=" * 60)
print("6. SHARED PREFERENCES AUDIT")
print("=" * 60)
prefs = run_adb("ls /data/data/com.truecaller/shared_prefs/").splitlines()
print(f"Total preference XML files: {len(prefs)}")
for p in sorted(prefs):
    if any(w in p.lower() for w in ['analytic', 'track', 'session', 'ad', 'survey', 'heartbeat', 'flash', 'clevertap', 'facebook', 'google', 'attribution', 'campaign', 'promo', 'experiment', 'variant']):
        cat_out = run_adb(f"cat /data/data/com.truecaller/shared_prefs/{p}")
        print(f"\n--- {p} ---")
        print(cat_out[:400])
