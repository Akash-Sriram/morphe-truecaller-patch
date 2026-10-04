import subprocess
import re

dexdump_bin = "/mnt/WindowsDrive/Users/akash/Downloads/Project/Morphe/.android_sdk/build-tools/35.0.0/dexdump"

def dump_class_info(dex_path, class_descriptor):
    cmd = [dexdump_bin, "-d", dex_path]
    proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    pattern = rf"Class descriptor\s+:\s+'{re.escape(class_descriptor)}'.*?(?=Class descriptor|\Z)"
    stdout, _ = proc.communicate()
    text = stdout.decode('utf-8', errors='replace')
    m = re.search(pattern, text, re.DOTALL)
    if m:
        print(f"=== Found {class_descriptor} in {dex_path} ===")
        print(m.group(0)[:4000])
    else:
        print(f"NOT found {class_descriptor} in {dex_path}")

print("1. AnalyticsNotificationReceiver:")
dump_class_info("/mnt/WindowsDrive/Users/akash/Downloads/Project/Morphe/scratch/classes9.dex", "Lcom/truecaller/notifications/AnalyticsNotificationReceiver;")

print("\n2. Lucm (RetentionTracker):")
dump_class_info("/mnt/WindowsDrive/Users/akash/Downloads/Project/Morphe/scratch/classes5.dex", "Lucm;")

print("\n3. JointActionsWorker:")
dump_class_info("/mnt/WindowsDrive/Users/akash/Downloads/Project/Morphe/scratch/classes7.dex", "Lcom/truecaller/background_work/JointActionsWorker;")

print("\n4. BackupWorker:")
dump_class_info("/mnt/WindowsDrive/Users/akash/Downloads/Project/Morphe/scratch/classes6.dex", "Lcom/truecaller/backup/worker/BackupWorker;")
