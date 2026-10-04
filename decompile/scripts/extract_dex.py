import zipfile
import subprocess
import os

# Let's extract classes5.dex, classes7.dex, classes8.dex, classes9.dex to scratch and run dexdump or analyze with python
scratch_dir = '/mnt/WindowsDrive/Users/akash/Downloads/Project/Morphe/scratch'
os.makedirs(scratch_dir, exist_ok=True)

with zipfile.ZipFile('com.truecaller_26.39.6.apkm') as apkm:
    with apkm.open('base.apk') as base_apk_file:
        with zipfile.ZipFile(base_apk_file) as base_apk:
            for dex_name in ['classes5.dex', 'classes6.dex', 'classes7.dex', 'classes8.dex', 'classes9.dex']:
                base_apk.extract(dex_name, scratch_dir)
                print(f"Extracted {dex_name} to {scratch_dir}")

print("Extraction complete.")
