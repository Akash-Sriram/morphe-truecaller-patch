import zipfile
import subprocess
import tempfile
import os
import re

dexdump_path = "/usr/lib/android-sdk/build-tools/35.0.0/dexdump"

targets = [
    "Lcom/unity3d/ads/UnityAds;",
    "Lcom/unity3d/services/UnityServices;",
    "Lcom/amazon/device/ads/AdRegistration;",
    "Lcom/amazon/device/ads/DTBAdNetworkInfo;",
    "Lcom/criteo/mediation/google/CriteoAdapter;",
    "Lcom/google/firebase/installations/FirebaseInstallationsRegistrar;",
    "Lcom/google/firebase/installations/FirebaseInstallationsKtxRegistrar;",
    "Lcom/google/firebase/heartbeatinfo/HeartBeatInfoStorage;",
    "Lcom/google/android/gms/measurement/internal/AppMeasurementDynamiteService;",
    "Lcom/google/android/gms/measurement/AppMeasurement;",
]

temp_dir = tempfile.mkdtemp(prefix="dex_inspect_")

with zipfile.ZipFile("truecaller_patched.apk") as z:
    for name in z.namelist():
        if name.endswith(".dex"):
            dex_path = os.path.join(temp_dir, os.path.basename(name))
            with open(dex_path, "wb") as f:
                f.write(z.read(name))
            
            proc = subprocess.Popen([dexdump_path, "-d", dex_path], stdout=subprocess.PIPE, text=True, errors="replace")
            current_class = None
            capture = False
            lines = []
            for line in proc.stdout:
                if "Class descriptor" in line:
                    m = re.search(r"Class descriptor\s*:\s*'(.*)'", line)
                    if m:
                        current_class = m.group(1)
                        capture = current_class in targets
                        if capture:
                            print(f"\n=================== FOUND CLASS: {current_class} in {name} ===================")
                elif capture:
                    if line.strip().startswith("#") and "Direct methods" in line or "Virtual methods" in line:
                        pass
                    if "name" in line and "'" in line:
                        m = re.search(r"name\s*:\s*'(.*)'", line)
                        if m:
                            print(f"  Method name: {m.group(1)}")
                    if "type" in line and "'" in line:
                        m = re.search(r"type\s*:\s*'(.*)'", line)
                        if m:
                            print(f"    Type: {m.group(1)}")
            proc.wait()
            os.remove(dex_path)

os.rmdir(temp_dir)
