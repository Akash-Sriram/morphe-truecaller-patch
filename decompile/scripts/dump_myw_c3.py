import subprocess, re

res = subprocess.run([".android_sdk/build-tools/35.0.0/dexdump", "-d", "tmp_myw.dex"], capture_output=True)
stdout = res.stdout.decode("utf-8", errors="replace")

idx = stdout.find("myw.c:(Lcom/truecaller/premium/data/feature/PremiumFeature;Z)Z")
if idx != -1:
    print(stdout[idx+4000:idx+9000])
