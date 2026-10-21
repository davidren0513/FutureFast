#!/usr/bin/env python3
"""Verify packaging contracts; this is not a device/VPN runtime test."""
from pathlib import Path
import argparse
import json
import os
import struct
import subprocess
import tempfile
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument('hap', type=Path)
args = parser.parse_args()
readelf = Path(os.environ['OHOS_NDK_HOME']) / 'llvm/bin/llvm-readelf'
with zipfile.ZipFile(args.hap) as archive, tempfile.TemporaryDirectory() as folder:
    assert archive.testzip() is None, 'Corrupt HAP'
    names = set(archive.namelist())
    metadata = json.loads(archive.read('module.json'))
    assert metadata['app']['bundleName'] == 'tech.future.fast.harmony'
    assert metadata['app']['minAPIVersion'] == 20
    assert 'ets/modules.abc' in names
    extensions = metadata['module']['extensionAbilities']
    assert any(e['name'] == 'FutureFastVpnAbility' and e['type'] == 'vpn'
               and not e['exported'] for e in extensions)
    libraries = {name for name in names if name.endswith('.so')}
    expected = {f'libs/arm64-v8a/{name}' for name in
                ['libfuturefast.so', 'libffhev.so', 'libc++_shared.so']}
    assert libraries == expected, f'Unexpected or missing libraries: {libraries ^ expected}'
    assert any(name.endswith('THIRD_PARTY_LICENSES.txt') for name in names)
    assert any(name.endswith('COPYRIGHT-library.html') for name in names)
    for name in sorted(libraries):
        data = archive.read(name)
        assert data[:6] == b'\x7fELF\x02\x01', f'Expected ELF64 little endian: {name}'
        assert struct.unpack_from('<HH', data, 16) == (3, 183), f'Expected AArch64 DYN: {name}'
        file = Path(folder) / Path(name).name
        file.write_bytes(data)
        dynamic = subprocess.check_output([str(readelf), '-d', str(file)], text=True)
        assert '(RUNPATH)' not in dynamic and '(RPATH)' not in dynamic, f'Build-host path in {name}'
        print(f'Checked AArch64 shared library: {name}')
    print('HAP metadata, ArkTS bytecode, ARM64 dependencies and notices verified.')
    print('SDK type:', metadata['app']['compileSdkType'],
          'SDK release:', metadata['app']['apiReleaseType'],
          'device types:', metadata['module']['deviceTypes'])
    print('Signature and device VPN operation are not verified by this check.')
