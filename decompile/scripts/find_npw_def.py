import zipfile, subprocess, re

with zipfile.ZipFile("com.truecaller_26.39.6.apkm") as z:
    with zipfile.ZipFile(z.open("base.apk")) as bz:
        for name in bz.namelist():
            if name.endswith(".dex"):
                with open("tmp_dex.dex", "wb") as f:
                    f.write(bz.read(name))
                res = subprocess.run([".android_sdk/build-tools/35.0.0/dexdump", "-d", "tmp_dex.dex"], capture_output=True)
                stdout = res.stdout.decode("utf-8", errors="replace")
                if "Class descriptor  : 'Lnpw;'" in stdout:
                    print(f"Found Lnpw; in {name}!")
                    match = re.search(r"Class descriptor\s+:\s+'Lnpw;'.*?(?=Class descriptor|\Z)", stdout, re.DOTALL)
                    if match:
                        section = match.group(0)
                        # find method t
                        t_match = re.search(r"name\s+:\s+'t'.*?(?=name\s+:|\Z)", section, re.DOTALL)
                        if t_match:
                            print(t_match.group(0)[:1500])
                    break
