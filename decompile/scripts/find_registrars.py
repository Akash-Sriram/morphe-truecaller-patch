import zipfile
import subprocess
import tempfile
import os
import re

dexdump_path = "/usr/lib/android-sdk/build-tools/35.0.0/dexdump"
registrars = set()

with zipfile.ZipFile("truecaller_patched.apk") as z:
    for name in z.namelist():
        if name.endswith(".dex"):
            with tempfile.NamedTemporaryFile(suffix=".dex", delete=False) as f:
                f.write(z.read(name))
                dex_path = f.name
            proc = subprocess.Popen([dexdump_path, "-f", dex_path], stdout=subprocess.PIPE, text=True, errors="replace")
            current_class = ""
            for line in proc.stdout:
                if "Class descriptor" in line:
                    m = re.search(r"Class descriptor\s*:\s*'(.*)'", line)
                    if m:
                        current_class = m.group(1)
                elif "interfaces" in line or "interface" in line:
                    if "ComponentRegistrar" in line and current_class != "Lcom/google/firebase/components/ComponentRegistrar;":
                        registrars.add((current_class, name))
            proc.wait()
            os.remove(dex_path)

print("Found Registrars:")
for r, dex in sorted(registrars):
    print(f"  {r} in {dex}")
