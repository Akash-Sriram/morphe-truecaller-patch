import subprocess
import re

dexdump_bin = "/mnt/WindowsDrive/Users/akash/Downloads/Project/Morphe/.android_sdk/build-tools/35.0.0/dexdump"

cmd = [dexdump_bin, "-d", "/mnt/WindowsDrive/Users/akash/Downloads/Project/Morphe/scratch/classes6.dex"]
proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
stdout, _ = proc.communicate()
text = stdout.decode('utf-8', errors='replace')

for worker in ["JointActionsWorker", "StandaloneActionWorker", "StartupActionsWorker", "BackupWorker"]:
    m = re.search(rf"Class descriptor\s+:\s+'Lcom/truecaller/(?:background_work|backup/worker)/{worker};'.*?(?=Class descriptor|\Z)", text, re.DOTALL)
    if m:
        class_block = m.group(0)
        print(f"=== {worker} ===")
        for method_m in re.finditer(r"name\s+:\s+'([^']+)'\s+type\s+:\s+'([^']+)'", class_block):
            mname, mtype = method_m.group(1), method_m.group(2)
            if "(" in mtype:
                print(f"  {mname}{mtype}")
