import subprocess, re

res = subprocess.run([".android_sdk/build-tools/35.0.0/dexdump", "-d", "tmp_find.dex"], capture_output=True)
stdout = res.stdout.decode("utf-8", errors="replace")
idx = stdout.find("PremiumFriendUpgradedPromoView")
if idx != -1:
    class_idx = stdout.rfind("Class descriptor", 0, idx)
    end = stdout.find("Class descriptor", class_idx+1)
    section = stdout[class_idx:end] if end != -1 else stdout[class_idx:]
    for m in re.finditer(r"name\s+:\s+'([^']+)'.*?type\s+:\s+'([^']+)'", section, re.DOTALL):
        print(f"  {m.group(1)}: {m.group(2)}")
