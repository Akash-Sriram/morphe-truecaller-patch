import subprocess, re

res = subprocess.run([".android_sdk/build-tools/35.0.0/dexdump", "-d", "tmp_find.dex"], capture_output=True)
stdout = res.stdout.decode("utf-8", errors="replace")

idx = stdout.find("Class descriptor  : 'Lcdn;'")
if idx != -1:
    print(stdout[idx:idx+3500])
