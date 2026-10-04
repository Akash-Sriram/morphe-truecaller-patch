import zipfile
import subprocess
import tempfile
import os
import re

dexdump_path = "/usr/lib/android-sdk/build-tools/35.0.0/dexdump"
targets = set()
with zipfile.ZipFile("truecaller_patched.apk") as z:
    for name in z.namelist():
        if name.endswith(".dex"):
            with tempfile.NamedTemporaryFile(suffix=".dex", delete=False) as f:
                f.write(z.read(name))
                dex_path = f.name
            proc = subprocess.Popen([dexdump_path, "-f", dex_path], stdout=subprocess.PIPE, text=True, errors="replace")
            for line in proc.stdout:
                if "Class descriptor" in line and "Lcom/truecaller/surveys/" in line:
                    m = re.search(r"Class descriptor\s*:\s*'(.*)'", line)
                    if m and not any(x in m.group(1) for x in ["R$", "R;"]):
                        targets.add(m.group(1))
            proc.wait()
            os.remove(dex_path)

print("Found Survey classes:")
for t in sorted(targets):
    print(f"  {t}")
