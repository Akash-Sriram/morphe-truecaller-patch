import zipfile, subprocess

with zipfile.ZipFile("com.truecaller_26.39.6.apkm") as z:
    with zipfile.ZipFile(z.open("base.apk")) as bz:
        with open("tmp_classes5.dex", "wb") as f:
            f.write(bz.read("classes5.dex"))
res = subprocess.run([".android_sdk/build-tools/35.0.0/dexdump", "-d", "tmp_classes5.dex"], capture_output=True)
stdout = res.stdout.decode("utf-8", errors="replace")
idx = stdout.find("Class descriptor  : 'Lkf8;'")
if idx != -1:
    print(stdout[idx:idx+2500])
