import zipfile

# Dex class def parser in pure python
def parse_dex_classes(dex_bytes):
    # Dex header: magic (8), checksum (4), signature (20), file_size (4), header_size (4), endian_tag (4),
    # link_size (4), link_off (4), map_off (4), string_ids_size (4), string_ids_off (4),
    # type_ids_size (4), type_ids_off (4), proto_ids_size (4), proto_ids_off (4),
    # field_ids_size (4), field_ids_off (4), method_ids_size (4), method_ids_off (4),
    # class_defs_size (4), class_defs_off (4)
    import struct
    string_ids_size, string_ids_off = struct.unpack('<II', dex_bytes[56:64])
    type_ids_size, type_ids_off = struct.unpack('<II', dex_bytes[64:72])
    class_defs_size, class_defs_off = struct.unpack('<II', dex_bytes[96:104])

    string_offsets = [struct.unpack('<I', dex_bytes[string_ids_off + i*4 : string_ids_off + (i+1)*4])[0] for i in range(string_ids_size)]

    def get_string(idx):
        off = string_offsets[idx]
        # skip uleb128 utf16_size
        while dex_bytes[off] & 0x80:
            off += 1
        off += 1
        end = dex_bytes.find(b'\x00', off)
        return dex_bytes[off:end].decode('utf-8', errors='replace')

    type_string_indices = [struct.unpack('<I', dex_bytes[type_ids_off + i*4 : type_ids_off + (i+1)*4])[0] for i in range(type_ids_size)]

    classes = []
    for i in range(class_defs_size):
        c_off = class_defs_off + i * 32
        class_idx = struct.unpack('<I', dex_bytes[c_off:c_off+4])[0]
        descriptor = get_string(type_string_indices[class_idx])
        classes.append(descriptor)
    return classes

with zipfile.ZipFile('com.truecaller_26.39.6.apkm') as apkm:
    with apkm.open('base.apk') as b_apk:
        with zipfile.ZipFile(b_apk) as apk:
            for f in apk.filelist:
                if f.filename.endswith('.dex') and f.filename.startswith('classes'):
                    dex_data = apk.read(f.filename)
                    defined_classes = set(parse_dex_classes(dex_data))
                    for target in [
                        "Lcom/truecaller/notifications/AnalyticsNotificationReceiver;",
                        "Lucm;",
                        "Lcom/truecaller/background_work/JointActionsWorker;",
                        "Lcom/truecaller/background_work/StandaloneActionWorker;",
                        "Lcom/truecaller/background_work/StartupActionsWorker;",
                        "Lcom/truecaller/backup/worker/BackupWorker;",
                        "Lcom/truecaller/background_work/TrackedCoroutineWorker;",
                        "Lcom/truecaller/background_work/TrackedWorker;"
                    ]:
                        if target in defined_classes:
                            print(f"{target} is DEFINED in {f.filename}")
