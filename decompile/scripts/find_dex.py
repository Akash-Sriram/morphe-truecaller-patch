import zipfile, subprocess, re

with zipfile.ZipFile("com.truecaller_26.39.6.apkm") as z:
    with zipfile.ZipFile(z.open("base.apk")) as bz:
        for name in bz.namelist():
            if name.endswith(".dex"):
                with open("tmp_dex.dex", "wb") as f:
                    f.write(bz.read(name))
                res = subprocess.run([".android_sdk/build-tools/35.0.0/dexdump", "tmp_dex.dex"], capture_output=True)
                stdout = res.stdout.decode("utf-8", errors="replace")
                if "Class descriptor  : 'Lcom/truecaller/premium/data/feature/PremiumFeature;'" in stdout:
                    print(f"Found PremiumFeature in {name}!")
                    # find enum fields
                    match = re.search(r"Class descriptor\s+:\s+'Lcom/truecaller/premium/data/feature/PremiumFeature;'.*?(?=Class descriptor|\Z)", stdout, re.DOTALL)
                    if match:
                        section = match.group(0)
                        fields = re.findall(r"name\s+:\s+'([^']+)'", section)
                        print("Fields/Enums:", fields)
                    break
