import subprocess
import re

dexdump_bin = "/mnt/WindowsDrive/Users/akash/Downloads/Project/Morphe/.android_sdk/build-tools/35.0.0/dexdump"

def inspect_methods(dex_file, class_name):
    cmd = [dexdump_bin, "-d", f"/mnt/WindowsDrive/Users/akash/Downloads/Project/Morphe/scratch/{dex_file}"]
    proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    pattern = rf"Class descriptor\s+:\s+'{re.escape(class_name)}'.*?(?=Class descriptor|\Z)"
    stdout, _ = proc.communicate()
    text = stdout.decode('utf-8', errors='replace')
    m = re.search(pattern, text, re.DOTALL)
    if not m:
        print(f"Not found {class_name}")
        return
    class_block = m.group(0)
    methods = re.findall(r"#\d+\s+:\s+\(in [^\)]+\)\s+name\s+:\s+'([^']+)'\s+type\s+:\s+'([^']+)'", class_block)
    print(f"=== {class_name} ({len(methods)} methods) ===")
    for name, mtype in methods:
        print(f"  {name} : {mtype}")

inspect_methods("classes6.dex", "Lcom/truecaller/background_work/TrackedCoroutineWorker;")
inspect_methods("classes6.dex", "Lcom/truecaller/background_work/TrackedWorker;")
inspect_methods("classes6.dex", "Lcom/truecaller/background_work/JointActionsWorker;")
inspect_methods("classes6.dex", "Lcom/truecaller/background_work/StandaloneActionWorker;")
inspect_methods("classes6.dex", "Lcom/truecaller/background_work/StartupActionsWorker;")
inspect_methods("classes6.dex", "Lcom/truecaller/backup/worker/BackupWorker;")
inspect_methods("classes7.dex", "Lcom/truecaller/notifications/AnalyticsNotificationReceiver;")
inspect_methods("classes5.dex", "Lucm;")
