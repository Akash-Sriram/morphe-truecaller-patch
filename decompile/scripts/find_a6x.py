import zipfile, subprocess, re

with zipfile.ZipFile("com.truecaller_26.39.6.apkm") as z:
    with zipfile.ZipFile(z.open("base.apk")) as bz:
        for name in bz.namelist():
            if name.endswith(".dex"):
                with open("tmp_a6x.dex", "wb") as f:
                    f.write(bz.read(name))
                res = subprocess.run([".android_sdk/build-tools/35.0.0/dexdump", "-d", "tmp_a6x.dex"], capture_output=True)
                stdout = res.stdout.decode("utf-8", errors="replace")
                if "Class descriptor  : 'La6x;'" in stdout:
                    print(f"Found La6x; in {name}!")
                    match = re.search(r"Class descriptor\s+:\s+'La6x;'.*?(?=Class descriptor|\Z)", stdout, re.DOTALL)
                    if match:
                        section = match.group(0)
                        # find method I
                        for m in re.finditer(r"name\s+:\s+'I'.*?(?=name\s+:|\Z)", section, re.DOTALL):
                            print(m.group(0)[:1000])
                    break
