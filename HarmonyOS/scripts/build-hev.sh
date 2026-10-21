#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
: "${OHOS_NDK_HOME:?Set OHOS_NDK_HOME to the public SDK native directory}"
SOURCE="$ROOT/hev-socks5-tunnel"
WORK="$ROOT/.toolchain/harmony-hev-source"
EXPECTED="$(git -C "$ROOT" ls-tree HEAD hev-socks5-tunnel | awk '{print $3}')"
test "$(git -C "$SOURCE" rev-parse HEAD)" = "$EXPECTED"
python3 - "$SOURCE" "$WORK" <<'PY'
from pathlib import Path
import shutil,sys
source,work=map(Path,sys.argv[1:])
if work.exists():shutil.rmtree(work)
shutil.copytree(source,work,ignore=shutil.ignore_patterns('.git','build','bin','*.o','*.a','*.so','*.dep'))
# SDK port seam: expose readiness after HEV initializes the TUN and stop event.
# This avoids guessing readiness with a fixed delay. Android sources stay unchanged.
p=work/'src/hev-socks5-tunnel.c'
with p.open('a') as f:f.write('\nint ff_hev_ready(void) { return (atomic_load_explicit(&tsync, memory_order_acquire) & SYNC_SEND) != 0; }\n')
PY
CC="$OHOS_NDK_HOME/llvm/bin/aarch64-unknown-linux-ohos-clang"
AR="$OHOS_NDK_HOME/llvm/bin/llvm-ar"
OUT="$ROOT/HarmonyOS/entry/libs/arm64-v8a"
mkdir -p "$OUT"
make -C "$WORK" -j4 static CC="$CC" PP="$CC -E" AR="$AR" \
  RANLIB="$OHOS_NDK_HOME/llvm/bin/llvm-ranlib" CFLAGS='-fPIC -O2'
cat > "$WORK/exports.map" <<'MAP'
{ global: hev_socks5_tunnel_main_from_str; hev_socks5_tunnel_quit; hev_socks5_tunnel_stats; ff_hev_ready; local: *; };
MAP
python3 - "$CC" "$WORK" "$OUT/libffhev.so" <<'PY'
from pathlib import Path
import subprocess,sys
cc,work,out=sys.argv[1:]
archives=sorted(str(p) for p in Path(work).rglob('*.a'))
assert len(archives)>=4,archives
main=next(p for p in archives if p.endswith('/bin/libhev-socks5-tunnel.a'))
others=[p for p in archives if p!=main]
subprocess.run([cc,'-shared','-Wl,--no-undefined','-Wl,-soname,libffhev.so',
 '-Wl,--version-script='+work+'/exports.map','-Wl,--whole-archive',main,
 '-Wl,--no-whole-archive','-Wl,--start-group',*others,'-Wl,--end-group','-lpthread','-ldl','-lm','-o',out],check=True)
PY
"$OHOS_NDK_HOME/llvm/bin/llvm-readelf" -h "$OUT/libffhev.so" | head -15
