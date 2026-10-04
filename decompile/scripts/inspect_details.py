import subprocess

def adb(cmd):
    full_cmd = f"adb -s 192.168.1.2:5555 shell \"su -c '{cmd}'\""
    res = subprocess.run(full_cmd, shell=True, capture_output=True, text=True)
    return res.stdout.strip()

print("=== 1. ANALYTICS.DB: Event Types in persisted_event ===")
# extract event names from protobuf / table
print(adb("sqlite3 /data/data/com.truecaller/databases/analytics.db \\\"SELECT id, name, timestamp FROM persisted_event LIMIT 15;\\\""))

print("\n=== 2. TC.DB: Profile View Events ===")
print(adb("sqlite3 /data/data/com.truecaller/databases/tc.db \\\"SELECT * FROM profile_view_events LIMIT 5;\\\""))

print("\n=== 3. TC.DB: Aggregated Contacts (Source of 7 contacts) ===")
print(adb("sqlite3 /data/data/com.truecaller/databases/tc.db \\\"SELECT _id, display_name, phone_number, tc_flag FROM aggregated_contact LIMIT 10;\\\""))

print("\n=== 4. SHARED_PREFS: appsflyer-data.xml ===")
print(adb("cat /data/data/com.truecaller/shared_prefs/appsflyer-data.xml"))

print("\n=== 5. SHARED_PREFS: attribution_settings.xml ===")
print(adb("cat /data/data/com.truecaller/shared_prefs/attribution_settings.xml"))

print("\n=== 6. SHARED_PREFS: analytics.xml ===")
print(adb("cat /data/data/com.truecaller/shared_prefs/analytics.xml"))

print("\n=== 7. SHARED_PREFS: WizRocket.xml (CleverTap) ===")
print(adb("cat /data/data/com.truecaller/shared_prefs/WizRocket.xml"))

