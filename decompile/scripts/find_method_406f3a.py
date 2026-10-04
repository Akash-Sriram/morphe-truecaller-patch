import subprocess, re

res = subprocess.run([".android_sdk/build-tools/35.0.0/dexdump", "-d", "tmp_find.dex"], capture_output=True)
stdout = res.stdout.decode("utf-8", errors="replace")

idx = stdout.find("406f3a:")
if idx != -1:
    class_idx = stdout.rfind("Class descriptor", 0, idx)
    method_idx = stdout.rfind("name          :", 0, idx)
    print(stdout[class_idx:class_idx+80])
    print(stdout[method_idx:method_idx+60])
    print(stdout[idx-200:idx+400])
