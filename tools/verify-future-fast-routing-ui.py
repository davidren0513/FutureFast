"""Exercise routing persistence using loopback-only fixtures in a disposable emulator.

Run after the clean-install capture script, with adb on PATH and ANDROID_SERIAL set.
These fixtures are intentionally not working proxies and do not prove AI availability.
"""
from pathlib import Path
import re
import os
import base64
import subprocess
import time
import xml.etree.ElementTree as ET

assert os.environ.get("ANDROID_SERIAL", "").startswith("emulator-"), "Set ANDROID_SERIAL to a disposable emulator first"
PACKAGE = "tech.future.fast"
OUT = Path("smoke")
OUT.mkdir(exist_ok=True)

def adb(*args):
    return subprocess.check_output(["adb", *args], timeout=40)

def tree():
    adb("shell", "uiautomator", "dump", "/sdcard/ff-routing.xml")
    return ET.fromstring(adb("shell", "cat", "/sdcard/ff-routing.xml"))

def label(node):
    return "\n".join(n.get("text", "") + "\n" + n.get("content-desc", "") for n in node.iter("node"))

def tap(text):
    root = tree()
    candidates = [n for n in root.iter("node") if text in label(n) and n.get("clickable") == "true"]
    assert candidates, "Missing action: " + text
    target = min(candidates, key=lambda n: len(label(n)))
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", target.get("bounds", "")))
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
    time.sleep(2.5)

def capture(name):
    root = tree()
    (OUT / (name + ".xml")).write_bytes(ET.tostring(root, encoding="utf-8"))
    (OUT / (name + ".png")).write_bytes(adb("exec-out", "screencap", "-p"))
    return root

def verify_assignments(root, assignments):
    for service, name in assignments.items():
        assert any(service in label(n) and name in label(n) and n.get("clickable") == "true"
                   for n in root.iter("node")), f"{service} did not retain {name}"

# The app's standard share import path, not an injected private database.
fixtures = "\n".join(f"socks://127.0.0.1:{port}#{name}" for port, name in
    [(10901, "TEST-US"), (10902, "TEST-JP"), (10903, "TEST-SG")])
# Share import replaces its group; import all fixtures in one batch.
payload = base64.b64encode(fixtures.encode()).decode()
adb("shell", "am", "start", "-n", PACKAGE + "/com.v2ray.ang.ui.UrlSchemeActivity",
    "-a", "android.intent.action.SEND", "-t", "text/plain", "--es", "android.intent.extra.TEXT", payload)
time.sleep(3)
adb("shell", "am", "force-stop", PACKAGE)
adb("shell", "am", "start", "-W", "-n", PACKAGE + "/com.v2ray.ang.ui.main.MainActivity")
time.sleep(4)
tap("节点")
root = capture("nodes-populated")
assert all(name in label(root) for name in ("TEST-US", "TEST-JP", "TEST-SG"))
tap("TEST-JP")
root = capture("nodes-selected")
assert any("TEST-JP" in label(n) and (n.get("checked") == "true" or n.get("selected") == "true") for n in root.iter("node"))
tap("AI 分流")
tap("启用 AI 分流")
assignments = {"ChatGPT": "TEST-US", "Claude": "TEST-JP", "Gemini": "TEST-SG"}
for service, name in assignments.items():
    tap(service)
    tap(name)
verify_assignments(capture("ai-assigned"), assignments)
# Process death followed by a fresh ViewModel/database load.
adb("shell", "am", "force-stop", PACKAGE)
adb("shell", "am", "start", "-W", "-n", PACKAGE + "/com.v2ray.ang.ui.main.MainActivity")
time.sleep(4)
tap("AI 分流")
verify_assignments(capture("ai-persisted"), assignments)
# Resetting Gemini to the default must not change the other two assignments.
tap("Gemini")
tap("跟随默认节点")
verify_assignments(capture("ai-gemini-default"), {**assignments, "Gemini": "跟随默认节点"})
# Check focus navigation and activation on the current screen.
adb("shell", "input", "keyevent", "61")
adb("shell", "input", "keyevent", "20")
capture("ai-keyboard-focus")
crash = adb("logcat", "-b", "crash", "-d")
assert b"FATAL EXCEPTION" not in crash, crash.decode(errors="replace")
print("PASS: populated nodes, GUID selection, three independent AI assignments, process-death persistence, Gemini default reset")
