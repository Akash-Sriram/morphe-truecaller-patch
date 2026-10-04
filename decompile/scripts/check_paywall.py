import subprocess, re

res = subprocess.run([".android_sdk/build-tools/35.0.0/dexdump", "-d", "tmp_find.dex"], capture_output=True)
stdout = res.stdout.decode("utf-8", errors="replace")

idx = stdout.find("Class descriptor  : 'Lcom/truecaller/premium/FullScreenPaywallActivity;'")
if idx != -1:
    end = stdout.find("Class descriptor", idx+1)
    section = stdout[idx:end] if end != -1 else stdout[idx:]
    print("Methods in FullScreenPaywallActivity:")
    for m in re.finditer(r"name\s+:\s+'([^']+)'.*?type\s+:\s+'([^']+)'", section, re.DOTALL):
        print(f"  {m.group(1)}: {m.group(2)}")
