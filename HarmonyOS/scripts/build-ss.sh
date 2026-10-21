#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
: "${OHOS_NDK_HOME:?Set OHOS_NDK_HOME to an SDK native directory whose license you have accepted}"
command -v cargo >/dev/null
CC="$OHOS_NDK_HOME/llvm/bin/aarch64-unknown-linux-ohos-clang"
export CC_aarch64_unknown_linux_ohos="$CC"
export AR_aarch64_unknown_linux_ohos="$OHOS_NDK_HOME/llvm/bin/llvm-ar"
export CARGO_TARGET_AARCH64_UNKNOWN_LINUX_OHOS_LINKER="$CC"
export CARGO_TARGET_AARCH64_UNKNOWN_LINUX_OHOS_RUSTFLAGS='-C relocation-model=pic'
cargo build --manifest-path "$ROOT/HarmonyOS/native/shadowsocks/Cargo.toml" \
  --locked --release --target aarch64-unknown-linux-ohos
mkdir -p "$ROOT/HarmonyOS/native/out/arm64-v8a"
cp "$ROOT/HarmonyOS/native/shadowsocks/target/aarch64-unknown-linux-ohos/release/libfuturefast_ss.a" \
  "$ROOT/HarmonyOS/native/out/arm64-v8a/libfuturefast_ss.a"
