import struct

def parse_axml_strings(data):
    # AXML starts with 0x00080003
    # Followed by file size, then StringPool chunk (0x001C0001)
    chunk_type, chunk_size = struct.unpack('<II', data[8:16])
    if chunk_type != 0x001C0001:
        return []
    string_count, style_count, flags, strings_start, styles_start = struct.unpack('<IIIII', data[16:36])
    offsets = [struct.unpack('<I', data[36+i*4:40+i*4])[0] for i in range(string_count)]
    strings = []
    pool_start = 8 + strings_start
    is_utf8 = bool(flags & (1 << 8))
    for off in offsets:
        pos = pool_start + off
        if is_utf8:
            # skip length bytes
            while data[pos] & 0x80: pos += 1
            pos += 1
            while data[pos] & 0x80: pos += 1
            pos += 1
            end = data.find(b'\x00', pos)
            strings.append(data[pos:end].decode('utf-8', errors='replace'))
        else:
            length = struct.unpack('<H', data[pos:pos+2])[0]
            pos += 2
            strings.append(data[pos:pos+length*2].decode('utf-16le', errors='replace'))
    return strings

with open('/mnt/WindowsDrive/Users/akash/Downloads/Project/Morphe/scratch/AndroidManifest.xml', 'rb') as f:
    data = f.read()

strings = parse_axml_strings(data)
print(f"Total strings in manifest: {len(strings)}")

receivers = [s for s in strings if 'Receiver' in s or 'receiver' in s]
print("\n=== Receivers in manifest ===")
for r in sorted(set(receivers)):
    print(" ", r)

services = [s for s in strings if 'Service' in s or 'service' in s]
print("\n=== Services in manifest (sample) ===")
for s in sorted(set(services))[:25]:
    print(" ", s)
