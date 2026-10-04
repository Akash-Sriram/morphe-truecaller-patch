import subprocess, re

res = subprocess.run([".android_sdk/build-tools/35.0.0/dexdump", "-d", "tmp_dex.dex"], capture_output=True)
stdout = res.stdout.decode("utf-8", errors="replace")

match = re.search(r"Class descriptor\s+:\s+'Lmyw;'.*?(?=Class descriptor|\Z)", stdout, re.DOTALL)
if match:
    section = match.group(0)
    print("Found Lmyw; definition:")
    # print methods
    c_match = re.search(r"name\s+:\s+'c'.*?(?=name\s+:|\Z)", section, re.DOTALL)
    if c_match:
        print(c_match.group(0)[:1500])
else:
    print("Lmyw; not in classes.dex")
