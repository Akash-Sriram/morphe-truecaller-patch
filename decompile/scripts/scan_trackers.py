import zipfile
import subprocess
import tempfile
import os
import re

dexdump_path = "/usr/lib/android-sdk/build-tools/35.0.0/dexdump"

print("Extracting DEX files from truecaller_patched.apk...")
temp_dir = tempfile.mkdtemp(prefix="dex_scan_")

patterns = {
    "Google Measurement / Analytics": [r'Lcom/google/android/gms/measurement', r'Lcom/google/firebase/analytics'],
    "Google Mobile Ads (AdMob)": [r'Lcom/google/android/gms/ads'],
    "Firebase HeartBeat": [r'Lcom/google/firebase/heartbeatinfo'],
    "Firebase Crashlytics": [r'Lcom/google/firebase/crashlytics'],
    "Firebase Sessions": [r'Lcom/google/firebase/sessions'],
    "Firebase Installations": [r'Lcom/google/firebase/installations'],
    "Firebase Perf": [r'Lcom/google/firebase/perf'],
    "Meta / Facebook": [r'Lcom/facebook/ads', r'Lcom/facebook/appevents'],
    "CleverTap": [r'Lcom/clevertap'],
    "AppsFlyer": [r'Lcom/appsflyer'],
    "Microsoft Clarity": [r'Lcom/microsoft/clarity'],
    "Adjust": [r'Lcom/adjust'],
    "Kochava": [r'Lcom/kochava'],
    "Branch": [r'Lio/branch'],
    "Criteo": [r'Lcom/criteo'],
    "InMobi": [r'Lcom/inmobi'],
    "AppLovin": [r'Lcom/applovin'],
    "Unity Ads": [r'Lcom/unity3d/ads'],
    "IronSource": [r'Lcom/ironsource'],
    "Mintegral": [r'Lcom/mbridge', r'Lcom/mintegral'],
    "Vungle": [r'Lcom/vungle'],
    "Pangle / Bytedance": [r'Lbytedance', r'Lcom/bytedance'],
    "Smaato": [r'Lcom/smaato'],
    "PubMatic": [r'Lcom/pubmatic'],
    "PubNative / HyBid": [r'Lnet/pubnative'],
    "Moloco": [r'Lcom/moloco'],
    "SmartAdServer": [r'Lcom/smartadserver'],
    "BidMachine": [r'Lio/bidmachine'],
    "Amazon Ads": [r'Lcom/amazon/device/ads'],
    "Freshchat": [r'Lcom/freshchat'],
    "Huawei HMS": [r'Lcom/huawei/hms'],
    "Truecaller Analytics": [r'Lcom/truecaller/analytics'],
    "Truecaller Ads": [r'Lcom/truecaller/ads'],
    "Truecaller Surveys": [r'Lcom/truecaller/surveys'],
    "Truecaller Tracking": [r'Lcom/truecaller/tracking'],
    "Truecaller Insights": [r'Lcom/truecaller/insights'],
}

results = {category: set() for category in patterns}

with zipfile.ZipFile("truecaller_patched.apk") as z:
    for name in z.namelist():
        if name.endswith(".dex"):
            dex_path = os.path.join(temp_dir, os.path.basename(name))
            with open(dex_path, "wb") as f:
                f.write(z.read(name))
            
            proc = subprocess.Popen([dexdump_path, "-f", dex_path], stdout=subprocess.PIPE, text=True, errors="replace")
            for line in proc.stdout:
                if "Class descriptor" in line:
                    m = re.search(r"Class descriptor\s*:\s*'(.*)'", line)
                    if m:
                        cls = m.group(1)
                        for cat, pats in patterns.items():
                            for p in pats:
                                if re.search(p, cls):
                                    results[cat].add(cls)
            proc.wait()
            os.remove(dex_path)

os.rmdir(temp_dir)

print("\n===== SCAN RESULTS FOR truecaller_patched.apk =====")
for cat, classes in results.items():
    if classes:
        print(f"\n[+] {cat}: {len(classes)} classes found")
        for c in sorted(list(classes))[:3]:
            print(f"    - {c}")
        if len(classes) > 3:
            print(f"    ... and {len(classes)-3} more")
    else:
        print(f"[-] {cat}: NONE found")
