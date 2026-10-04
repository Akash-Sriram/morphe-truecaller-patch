import subprocess, re

res = subprocess.run([".android_sdk/build-tools/35.0.0/dexdump", "-d", "tmp_find.dex"], capture_output=True)
stdout = res.stdout.decode("utf-8", errors="replace")

match = re.search(r"Class descriptor\s+:\s+'Ladn;'.*?(?=Class descriptor|\Z)", stdout, re.DOTALL)
if match:
    section = match.group(0)
    print("Class descriptor: Ladn;")
    for m in re.finditer(r"name\s+:\s+'([^']+)'.*?type\s+:\s+'([^']+)'", section, re.DOTALL):
        print(f"  {m.group(1)}: {m.group(2)}")
