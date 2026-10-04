import subprocess
import json

def sql_exec(db_name, sql):
    db_path = f"/data/data/com.truecaller/databases/{db_name}"
    p = subprocess.Popen(
        ["adb", "shell", f"su -c 'sqlite3 {db_path}'"],
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True
    )
    out, err = p.communicate(sql + "\n")
    return out.strip(), err.strip()

def run_cmd(cmd):
    p = subprocess.run(f"adb shell \"su -c '{cmd}'\"", shell=True, capture_output=True, text=True)
    return p.stdout.strip()

print("=======================================================")
print("=== 1. DEFAULT DIALER & TELECOM STATUS ===")
print("=======================================================")
print("Default Dialer:", run_cmd("cmd role get-role-holders android.app.role.DIALER"))
print("Default Call Screening:", run_cmd("cmd role get-role-holders android.app.role.CALL_SCREENING"))
print("Telecom InCallService bindings:")
telecom_incall = run_cmd("dumpsys telecom | grep -E -A 5 'mInCallServices'")
print(telecom_incall if telecom_incall else "None")

print("\n=======================================================")
print("=== 2. LOCAL CALL LOG & CONTACTS ACCESS ===")
print("=======================================================")
out, _ = sql_exec("tc.db", "SELECT count(*) FROM history;")
print(f"tc.db [history] row count: {out}")

out, _ = sql_exec("tc.db", "SELECT _id, tc_id, normalized_number, type, action, duration, timestamp FROM history ORDER BY timestamp DESC LIMIT 5;")
print(f"tc.db [history] recent entries:\n{out}")

out, _ = sql_exec("tc.db", "SELECT count(*) FROM aggregated_contact;")
print(f"tc.db [aggregated_contact] row count: {out}")

out, _ = sql_exec("tc.db", "SELECT count(*) FROM contact_phones;")
print(f"tc.db [contact_phones] row count: {out}")

out, _ = sql_exec("tc.db", "SELECT count(*) FROM msg_messages;")
print(f"tc.db [msg_messages] row count: {out}")

print("\n=======================================================")
print("=== 3. PRIVACY & SURVEILLANCE AUDIT (ZERO-TOLERANCE) ===")
print("=======================================================")
checks = [
    ("analytics.db", "persisted_event"),
    ("tc.db", "profile_view_events"),
    ("tc.db", "search_warnings"),
    ("tc.db", "search_history"),
    ("insights.db", "analytics_events"),
    ("insights.db", "parser_analytics_events"),
    ("insights.db", "sms_backup"),
    ("surveysDb", "surveys"),
    ("google_app_measurement_local.db", "messages"),
]

for db, tbl in checks:
    out, err = sql_exec(db, f"SELECT count(*) FROM {tbl};")
    print(f"[{db}] table '{tbl}': {out} rows (err: {err})")

print("\n=======================================================")
print("=== 4. WORKMANAGER QUEUE (BACKGROUND TASKS) ===")
print("=======================================================")
out, _ = sql_exec("androidx.work.workdb", "SELECT id, worker_class_name, state, run_attempt_count FROM workspec ORDER BY period_start_time DESC LIMIT 15;")
print(f"WorkSpec entries:\n{out}")

print("\n=======================================================")
print("=== 5. ACTIVE NETWORK SOCKETS FOR TRUECALLER ===")
print("=======================================================")
pid_out = run_cmd("pidof com.truecaller")
print(f"Truecaller PID: {pid_out}")
if pid_out:
    ss_out = run_cmd(f"ss -tupn | grep {pid_out}")
    print(f"Active sockets for PID {pid_out}:\n{ss_out if ss_out else 'NO ACTIVE NETWORK SOCKETS'}")

    lsof_net = run_cmd(f"lsof -p {pid_out} | grep -E 'TCP|UDP'")
    print(f"Lsof network for PID {pid_out}:\n{lsof_net if lsof_net else 'NO ACTIVE TCP/UDP SOCKETS'}")

print("\n=======================================================")
print("=== 6. LOGCAT TELEMETRY & UPLOAD TRACES ===")
print("=======================================================")
tc_logs = run_cmd(f"logcat -d --pid={pid_out} -t 50 | grep -iE 'upload|telemetry|analytics|sync|contact|calllog|tracker'")
print(tc_logs if tc_logs else "No telemetry/upload/tracker log events found")
