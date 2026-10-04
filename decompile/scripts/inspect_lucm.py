import subprocess
import re

dexdump_bin = "/mnt/WindowsDrive/Users/akash/Downloads/Project/Morphe/.android_sdk/build-tools/35.0.0/dexdump"

cmd = [dexdump_bin, "-d", "/mnt/WindowsDrive/Users/akash/Downloads/Project/Morphe/scratch/classes5.dex"]
proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
pattern = r"Class descriptor\s+:\s+'Lucm;'.*?(?=Class descriptor|\Z)"
stdout, _ = proc.communicate()
text = stdout.decode('utf-8', errors='replace')
m = re.search(pattern, text, re.DOTALL)
if m:
    class_block = m.group(0)
    methods = re.findall(r"name\s+:\s+'([^']+)'\s+type\s+:\s+'([^']+)'", class_block)
    print("Lucm methods:")
    for n, t in methods:
        print(f"  {n}: {t}")
