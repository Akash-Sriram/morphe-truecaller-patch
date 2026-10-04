import subprocess, re

res = subprocess.run([".android_sdk/build-tools/35.0.0/dexdump", "-d", "tmp_find.dex"], capture_output=True)
stdout = res.stdout.decode("utf-8", errors="replace")

matches = [m.start() for m in re.finditer(r"new-instance [^,]+, Ladn;", stdout)]
print(f"Total new-instance Ladn; matches: {len(matches)}")
for idx in matches:
    start = max(0, idx - 200)
    end = min(len(stdout), idx + 300)
    print("--- CALLER ---")
    print(stdout[start:end])
