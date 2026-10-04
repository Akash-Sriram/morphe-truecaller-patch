import subprocess

def adb(cmd):
    full_cmd = f"adb -s 192.168.1.2:5555 shell \"su -c '{cmd}'\""
    res = subprocess.run(full_cmd, shell=True, capture_output=True, text=True)
    return res.stdout.strip()

print("Event name distribution in persisted_event:")
print(adb("sqlite3 /data/data/com.truecaller/databases/analytics.db \\\"SELECT event_name, count(*) FROM persisted_event GROUP BY event_name;\\\""))

print("\nRecent 10 events:")
print(adb("sqlite3 /data/data/com.truecaller/databases/analytics.db \\\"SELECT id, event_name, datetime(created_at/1000, 'unixepoch', 'localtime') FROM persisted_event ORDER BY id DESC LIMIT 10;\\\""))
