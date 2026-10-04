import zipfile, subprocess, re

with zipfile.ZipFile("com.truecaller_26.39.6.apkm") as z:
    with zipfile.ZipFile(z.open("base.apk")) as bz:
        for name in ["classes5.dex", "classes6.dex", "classes7.dex", "classes8.dex"]:
            with open("tmp_myw.dex", "wb") as f:
                f.write(bz.read(name))
            res = subprocess.run([".android_sdk/build-tools/35.0.0/dexdump", "-d", "tmp_myw.dex"], capture_output=True)
            stdout = res.stdout.decode("utf-8", errors="replace")
            if "Class descriptor  : 'Lmyw;'" in stdout:
                print(f"Found Lmyw; in {name}!")
                match = re.search(r"Class descriptor\s+:\s+'Lmyw;'.*?(?=Class descriptor|\Z)", stdout, re.DOTALL)
                if match:
                    section = match.group(0)
                    for m in re.finditer(r"name\s+:\s+'c'.*?(?=name\s+:|\Z)", section, re.DOTALL):
                        print(m.group(0)[:1500])
                break
