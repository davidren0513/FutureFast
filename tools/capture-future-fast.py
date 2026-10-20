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
    # A locale change recreates the activity and temporarily detaches its accessibility root.
    for attempt in range(4):
        adb("shell", "input", "keyevent", "224")
        adb("shell", "rm", "-f", "/sdcard/ff-window.xml")
        adb("shell", "uiautomator", "dump", "/sdcard/ff-window.xml")
        result = subprocess.run(["adb", "shell", "cat", "/sdcard/ff-window.xml"], capture_output=True, timeout=10)
        if result.returncode == 0 and b"<hierarchy" in result.stdout:
            xml = result.stdout
            root = ET.fromstring(xml)
            # API 35 CI emulator: its Quickstep launcher may ANR during cold boot.
            # Close only that launcher dialog; never dismiss an application ANR.
            # Remove when the emulator image no longer exhibits this cold-boot failure.
            if any(n.get("text") == "Quickstep isn't responding" for n in root.iter("node")):
                (OUT / ("launcher-anr-" + str(attempt) + ".xml")).write_bytes(xml)
                close = next(n for n in root.iter("node") if n.get("resource-id") == "android:id/aerr_close")
                x1, y1, x2, y2 = map(int, re.findall(r"\d+", close.get("bounds", "")))
                adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
                time.sleep(2)
                continue
            if not any(n.get("package") == "tech.future.fast" for n in root.iter("node")):
                time.sleep(2)
                continue
            break
        time.sleep(2)
    else:
        (OUT / "failure-screen.png").write_bytes(adb("exec-out", "screencap", "-p"))
        (OUT / "failure-logcat.txt").write_bytes(adb("logcat", "-d", "-t", "1500"))
        (OUT / "failure-window.txt").write_bytes(adb("shell", "dumpsys", "window"))
        raise AssertionError("No accessibility root after locale restart")
    (OUT / (name + ".xml")).write_bytes(xml)
    return ET.fromstring(xml)

def capture(name):
    root = snapshot(name)
    (OUT / (name + ".png")).write_bytes(adb("exec-out", "screencap", "-p"))
    return root

def tap_label(label):
    root = snapshot("navigation")
    for node in root.iter("node"):
        text = "\n".join(n.get("text", "") + "\n" + n.get("content-desc", "") for n in node.iter("node"))
        if label in text and node.get("clickable") == "true":
            x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds", "")))
            adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
            time.sleep(1)
            return
    raise AssertionError("Missing clickable label: " + label)

adb("shell", "svc", "power", "stayon", "true")
adb("shell", "input", "keyevent", "224")
adb("shell", "wm", "dismiss-keyguard")
adb("shell", "cmd", "locale", "set-app-locales", "tech.future.fast", "--user", "0", "--locales", "zh-CN")
adb("shell", "am", "force-stop", "tech.future.fast")
adb("shell", "am", "start", "-W", "-n", "tech.future.fast/com.v2ray.ang.ui.main.MainActivity")
time.sleep(8)
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
