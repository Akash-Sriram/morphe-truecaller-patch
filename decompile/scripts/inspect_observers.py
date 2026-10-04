import subprocess
import re

dexdump_bin = "/mnt/WindowsDrive/Users/akash/Downloads/Project/Morphe/.android_sdk/build-tools/35.0.0/dexdump"

for dex, clist in [("scratch/classes6.dex", ['Lahc;', 'Ln7f;', 'Lh48;', 'Lupk;', 'Lzl3;']), ("scratch/classes7.dex", ['Le3t;', 'Llh9;'])]:
    cmd = [dexdump_bin, "-d", dex]
    proc = subprocess.Popen(cmd, stdout=subprocess.PIPE)
    text = proc.stdout.read().decode('utf-8', errors='replace')
    for c in clist:
        m = re.search(rf"Class descriptor\s+:\s+'{re.escape(c)}'.*?(?=Class descriptor|\Z)", text, re.DOTALL)
        if m:
            print(f"=== {c} in {dex} ===")
            block = m.group(0)
            # Find strings or method calls in onChange
            for on_change in re.finditer(r"onChange:.*?(?=\[[0-9a-f]+\]|\Z)", block, re.DOTALL):
                print(on_change.group(0)[:500])
