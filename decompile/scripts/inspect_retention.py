import subprocess
import re

dexdump_bin = "/mnt/WindowsDrive/Users/akash/Downloads/Project/Morphe/.android_sdk/build-tools/35.0.0/dexdump"

def run_dexdump(dex_file):
    cmd = [dexdump_bin, "-d", f"/mnt/WindowsDrive/Users/akash/Downloads/Project/Morphe/scratch/{dex_file}"]
    proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    stdout, _ = proc.communicate()
    return stdout.decode('utf-8', errors='replace')

# Look for RetentionTracker in classes5.dex
txt5 = run_dexdump("classes5.dex")
for m in re.finditer(r"RetentionTracker", txt5):
    idx = m.start()
    print("RetentionTracker in classes5.dex:")
    print(txt5[max(0, idx-300):min(len(txt5), idx+500)])
    break

# Also inspect Lucm.b and Lucm.a implementation
m = re.search(r"Class descriptor\s+:\s+'Lucm;'.*?(?=Class descriptor|\Z)", txt5, re.DOTALL)
if m:
    print("\nLucm full class definition summary:")
    lines = m.group(0).split('\n')
    for line in lines[:60]:
        print(line)
