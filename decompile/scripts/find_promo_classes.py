import zipfile, subprocess, re

for dex, target, hex_str in [("classes7.dex", "0x7f0d00cf", "#7f0d00cf"), 
                             ("classes5.dex", "0x7f0d01b1", "#7f0d01b1"), 
                             ("classes7.dex", "0x7f0d0a61", "#7f0d0a61")]:
    with zipfile.ZipFile("com.truecaller_26.39.6.apkm") as z:
        with zipfile.ZipFile(z.open("base.apk")) as bz:
            with open("tmp_find.dex", "wb") as f:
                f.write(bz.read(dex))
    res = subprocess.run([".android_sdk/build-tools/35.0.0/dexdump", "-d", "tmp_find.dex"], capture_output=True)
    stdout = res.stdout.decode("utf-8", errors="replace")
    for m in re.finditer(re.escape(hex_str), stdout, re.IGNORECASE):
        idx = m.start()
        print(f"=== {dex} - {target} ===")
        # search backward for Class descriptor
        class_idx = stdout.rfind("Class descriptor", 0, idx)
        method_idx = stdout.rfind("name          :", 0, idx)
        print(stdout[class_idx:class_idx+100])
        print(stdout[method_idx:method_idx+60])
        print(stdout[max(0, idx-100):min(len(stdout), idx+200)])
