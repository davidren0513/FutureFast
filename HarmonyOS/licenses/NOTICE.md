# Third-party components

- `shadowsocks-service` 1.25.0 and its Rust dependency graph: upstream shadowsocks/shadowsocks-rust, MIT license for the Shadowsocks components. Exact versions and checksums are in `native/shadowsocks/Cargo.lock`. Preserve the licenses of all linked transitive crates when distributing a binary; the lockfile is not a replacement for those licenses.
- `hev-socks5-tunnel`: source pinned by the repository's `hev-socks5-tunnel` gitlink. Its main license is reproduced in `hev-socks5-tunnel.txt`. Its bundled task system, YAML parser and lwIP have their own notices in the original submodule and must accompany binary distribution.
- `scripts/build-hev.sh` appends a first-party readiness-query function to a temporary build copy. It does not replace the upstream license or modify the Android dependency checkout.
- The first-party HarmonyOS application and native adapters follow this repository's GPL-3.0 license. No signing credentials or SDK binaries are distributed in this source directory.

A distributable HAP has not yet been produced. Before distribution, collect the complete licenses of the exact linked Rust and HEV dependency versions into the package and verify platform signing requirements.
