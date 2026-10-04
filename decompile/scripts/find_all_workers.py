import zipfile
import re

def analyze_workers():
    with zipfile.ZipFile('com.truecaller_26.39.6.apkm') as apkm:
        with apkm.open('base.apk') as base_apk_file:
            with zipfile.ZipFile(base_apk_file) as base_apk:
                for f in base_apk.filelist:
                    if f.filename.startswith('classes') and f.filename.endswith('.dex'):
                        data = base_apk.read(f.filename)
                        # Find class names matching Worker
                        # Dalvik class names are Lcom/truecaller/...Worker;
                        workers = set(re.findall(rb'Lcom/truecaller/[a-zA-Z0-9_/$]*Worker;', data))
                        if workers:
                            print(f"=== {f.filename} ({len(workers)} workers) ===")
                            for w in sorted(workers):
                                print(" ", w.decode('ascii'))

if __name__ == '__main__':
    analyze_workers()
