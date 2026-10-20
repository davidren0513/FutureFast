"""Capture actual preview screens without importing credentials or sample servers."""
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

OUT = Path("smoke")
OUT.mkdir(exist_ok=True)

def adb(*args):
    return subprocess.check_output(["adb", *args], timeout=35)

def snapshot(name):
    adb("shell", "uiautomator", "dump", "/sdcard/ff-window.xml")
    xml = adb("shell", "cat", "/sdcard/ff-window.xml")
    (OUT / (name + ".xml")).write_bytes(xml)
    return ET.fromstring(xml)

def capture(name):
    (OUT / (name + ".png")).write_bytes(adb("exec-out", "screencap", "-p"))
    return snapshot(name)

def tap_label(label):
    root = snapshot("navigation")
    for node in root.iter("node"):
        text = node.get("text", "") + "\n" + node.get("content-desc", "")
        if label in text and node.get("clickable") == "true":
            x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds", "")))
            adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
            time.sleep(1)
            return
    raise AssertionError("Missing clickable label: " + label)

adb("shell", "cmd", "locale", "set-app-locales", "tech.future.fast", "--user", "0", "--locales", "zh-CN")
adb("shell", "am", "force-stop", "tech.future.fast")
adb("shell", "am", "start", "-W", "-n", "tech.future.fast/com.v2ray.ang.ui.main.MainActivity")
time.sleep(3)
capture("future-fast-home")
tap_label("自动管理")
capture("future-fast-automation")
adb("shell", "input", "keyevent", "4")
time.sleep(1)
tap_label("AI 分流")
capture("future-fast-ai")
adb("shell", "input", "keyevent", "4")
time.sleep(1)
adb("shell", "cmd", "uimode", "night", "yes")
time.sleep(2)
capture("future-fast-home-dark")
adb("shell", "cmd", "uimode", "night", "no")
adb("shell", "pidof", "tech.future.fast")
crash = adb("logcat", "-b", "crash", "-d")
(OUT / "crash.log").write_bytes(crash)
assert b"FATAL EXCEPTION" not in crash, crash.decode(errors="replace")
