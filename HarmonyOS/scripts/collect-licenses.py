#!/usr/bin/env python3
"""Collect notices from the exact locked Cargo source graph and HEV checkout.

Requires populated CARGO_HOME, cargo on PATH and OHOS_NDK_HOME. Does not download
or accept SDK licenses. Keep generated notices in the HAP rawfile resources.
"""
from pathlib import Path
import os
import re
import subprocess
import shutil
import tomllib

root = Path(__file__).resolve().parents[2]
project = root / 'HarmonyOS'
cargo_home = Path(os.environ.get('CARGO_HOME', Path.home() / '.cargo'))
ndk = Path(os.environ['OHOS_NDK_HOME'])
tree = subprocess.check_output([
    'cargo', 'tree', '--locked', '--offline', '--manifest-path',
    str(project / 'native/shadowsocks/Cargo.toml'), '--target',
    'aarch64-unknown-linux-ohos', '-e', 'normal', '--prefix', 'none', '--format', '{p}'
], text=True)
text = ['Third-party source notices for the ARM64 prototype.\n']
crates = sorted(set(re.findall(r'^([\w-]+) v([\w.+-]+)', tree, re.M)))
for name, version in crates:
    if name == 'futurefast-ss':
        continue
    source = next((cargo_home / 'registry/src').glob(f'*/{name}-{version}'))
    meta = tomllib.loads((source / 'Cargo.toml').read_text())['package']
    text.append(f'\n===== {name} {version} =====\nSPDX: {meta.get("license", "see license-file")}\nSource: {meta.get("repository", "crates.io: " + name)}\n')
    files = sorted(f for f in source.iterdir() if f.is_file() and
                   re.match(r'(?i)^(license|licence|copying|notice|copyright)', f.name))
    if meta.get('license-file'):
        file = source / meta['license-file']
        if file.is_file() and file not in files:
            files.append(file)
    if name == 'shadowsocks-service' and not files:
        # Published crate omits the root MIT file. This copy is from its exact
        # .cargo_vcs_info commit ab388c7466d21f979430e33cc9ef10e22fb05955.
        files.append(project / 'licenses/shadowsocks-rust.txt')
    if not files:
        raise RuntimeError(f'Missing license text for {name} {version}')
    for file in files:
        text.append(f'--- {file.name} ---\n' + file.read_text(errors='replace'))
for relative in ['LICENSE', 'hev-socks5-tunnel/LICENSE', 'hev-socks5-tunnel/src/core/LICENSE',
                 'hev-socks5-tunnel/third-part/hev-task-system/LICENSE',
                 'hev-socks5-tunnel/third-part/lwip/LICENSE',
                 'hev-socks5-tunnel/third-part/yaml/License']:
    text.append(f'\n===== {relative} =====\n' + (root / relative).read_text())
text.append('\n===== LLVM / libc++ runtime =====\n' + (ndk / 'llvm/NOTICE').read_text())
output = project / 'entry/src/main/resources/rawfile/THIRD_PARTY_LICENSES.txt'
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text('\n'.join(text))
sysroot = Path(subprocess.check_output(['rustc', '--print', 'sysroot'], text=True).strip())
rust_notices = sysroot / 'share/doc/rust'
rust_output = output.parent / 'rust-stdlib'
rust_output.mkdir(exist_ok=True)
shutil.copy2(rust_notices / 'COPYRIGHT-library.html', rust_output)
shutil.copytree(rust_notices / 'licenses', rust_output / 'licenses', dirs_exist_ok=True)
for notice in [output, *rust_output.rglob('*')]:
    if notice.is_file():
        # Preserve the license wording while avoiding upstream whitespace noise.
        normalized = '\n'.join(line.rstrip() for line in notice.read_text().splitlines()).rstrip() + '\n'
        notice.write_text(normalized)
print(f'Collected {len(crates) - 1} Cargo packages, HEV and libc++ notices into {output}')
