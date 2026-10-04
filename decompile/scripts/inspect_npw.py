import subprocess, re

res = subprocess.run([".android_sdk/build-tools/35.0.0/dexdump", "-d", "tmp_dex.dex"], capture_output=True)
stdout = res.stdout.decode("utf-8", errors="replace")

# Search for Lnpw; methods
match = re.search(r"Class descriptor\s+:\s+'Lnpw;'.*?(?=Class descriptor|\Z)", stdout, re.DOTALL)
if match:
    print("Found Lnpw; definition:")
    print(match.group(0)[:2000])
else:
    print("Lnpw; not defined here, searching callers or interface...")
    for m in re.finditer(r"Lnpw;\.t:", stdout):
        idx = m.start()
        print(stdout[max(0, idx-200):min(len(stdout), idx+200)])
