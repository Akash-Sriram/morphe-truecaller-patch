import zipfile

receivers_to_check = [
    "Lcom/truecaller/familyprotect/domain/status/activity/receiver/UserActivityTransitionBroadcastReceiver;",
    "Lcom/truecaller/presence/domain/worker/PresenceSchedulerReceiver;",
    "Lcom/truecaller/util/ScreenUnlockedBroadcastReceiver;",
    "Lcom/truecaller/util/RingerModeChangedReceiver;",
    "Lcom/truecaller/stats/IntentChooserReceiver;",
    "Lcom/truecaller/analytics/ChosenComponentReceiverViewActionEvent;",
]

with zipfile.ZipFile('com.truecaller_26.39.6.apkm') as apkm:
    with apkm.open('base.apk') as b_apk:
        with zipfile.ZipFile(b_apk) as apk:
            for f in apk.filelist:
                if f.filename.endswith('.dex'):
                    data = apk.read(f.filename)
                    for r in receivers_to_check:
                        if r.encode('ascii') in data:
                            print(f"{r} referenced/defined in {f.filename}")
