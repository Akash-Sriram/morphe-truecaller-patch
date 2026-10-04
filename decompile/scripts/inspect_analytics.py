import subprocess

def adb(cmd):
    full_cmd = f"adb -s 192.168.1.2:5555 shell \"su -c '{cmd}'\""
    res = subprocess.run(full_cmd, shell=True, capture_output=True, text=True)
    return res.stdout.strip()

print("Schema of persisted_event:")
print(adb("sqlite3 /data/data/com.truecaller/databases/analytics.db .schema"))

print("\nSample rows in persisted_event:")
print(adb("sqlite3 /data/data/com.truecaller/databases/analytics.db \\\"SELECT hex(event) FROM persisted_event LIMIT 3;\\\""))

