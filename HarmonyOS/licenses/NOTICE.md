# Third-party components

- `shadowsocks-service` 1.25.0 and its Rust dependency graph: upstream shadowsocks/shadowsocks-rust, MIT license for the Shadowsocks components. Exact versions and checksums are in `native/shadowsocks/Cargo.lock`. Preserve the licenses of all linked transitive crates when distributing a binary; the lockfile is not a replacement for those licenses.
- `hev-socks5-tunnel`: source pinned by the repository's `hev-socks5-tunnel` gitlink. Its main license is reproduced in `hev-socks5-tunnel.txt`. Its bundled task system, YAML parser and lwIP have their own notices in the original submodule and must accompany binary distribution.
- `scripts/build-hev.sh` appends a first-party readiness-query function to a temporary build copy. It does not replace the upstream license or modify the Android dependency checkout.
- The first-party HarmonyOS application and native adapters follow this repository's GPL-3.0 license. No signing credentials or SDK binaries are distributed in this source directory.

`scripts/collect-licenses.py` collects the locked ARM64 normal dependency graph notices, HEV notices, repository GPL text and LLVM/libc++ notice into `resources/rawfile/THIRD_PARTY_LICENSES.txt`. Rust standard library copyright and license files are included alongside it. The published shadowsocks-service crate omits its root MIT file; `shadowsocks-rust.txt` is copied from its exact source commit `ab388c7466d21f979430e33cc9ef10e22fb05955`.

An unsigned experimental HAP has been built with these notices. It is not a signed HarmonyOS phone release. Source is available in this directory with the lockfile, build scripts and pinned HEV submodule in the parent repository. Platform compatibility, signing and device testing remain required.
