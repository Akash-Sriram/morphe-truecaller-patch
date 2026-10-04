import subprocess, re

res = subprocess.run([".android_sdk/build-tools/35.0.0/dexdump", "-d", "tmp_dex.dex"], capture_output=True)
stdout = res.stdout.decode("utf-8", errors="replace")

matches = [m.start() for m in re.finditer(r"INCOGNITO_MODE", stdout)]
print(f"Total INCOGNITO_MODE matches: {len(matches)}")
for idx in matches[:10]:
    start = max(0, idx - 400)
    end = min(len(stdout), idx + 400)
    print("--- SNIPPET ---")
    print(stdout[start:end])
