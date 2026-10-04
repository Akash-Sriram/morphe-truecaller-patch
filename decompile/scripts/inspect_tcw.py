import subprocess
import re

dexdump_bin = "/mnt/WindowsDrive/Users/akash/Downloads/Project/Morphe/.android_sdk/build-tools/35.0.0/dexdump"

cmd = [dexdump_bin, "-d", "/mnt/WindowsDrive/Users/akash/Downloads/Project/Morphe/scratch/classes6.dex"]
proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
stdout, _ = proc.communicate()
text = stdout.decode('utf-8', errors='replace')

m = re.search(r"Class descriptor\s+:\s+'Lcom/truecaller/background_work/TrackedCoroutineWorker;'.*?(?=Class descriptor|\Z)", text, re.DOTALL)
if m:
    class_block = m.group(0)
    print(class_block[:3500])
