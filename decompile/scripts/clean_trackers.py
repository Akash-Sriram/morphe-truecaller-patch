import subprocess

cmds = [
    # 1. Purge legacy third-party attribution XMLs
    "rm -f /data/data/com.truecaller/shared_prefs/appsflyer-data.xml",
    "rm -f /data/data/com.truecaller/shared_prefs/WizRocket.xml",
    "rm -f /data/data/com.truecaller/shared_prefs/analytics.xml",
    
    # 2. Purge Google DataTransport and Crashlytics local caches
    "rm -rf /data/data/com.truecaller/databases/com.google.android.datatransport.events*",
    "rm -rf /data/data/com.truecaller/files/.crashlytics.v3",
    
    # 3. Purge existing locally queued surveillance events from databases
    "sqlite3 /data/data/com.truecaller/databases/analytics.db 'DELETE FROM persisted_event;'",
    "sqlite3 /data/data/com.truecaller/databases/tc.db 'DELETE FROM profile_view_events;'",
    
    # 4. Check counts
    "sqlite3 /data/data/com.truecaller/databases/analytics.db 'SELECT COUNT(*) FROM persisted_event;'",
    "sqlite3 /data/data/com.truecaller/databases/tc.db 'SELECT COUNT(*) FROM profile_view_events;'",
]

for cmd in cmds:
    res = subprocess.run(["adb", "shell", f"su -c '{cmd}'"], capture_output=True, text=True)
    print(f"[{cmd[:40]}...] => {res.stdout.strip()} (err: {res.stderr.strip()})")

