"""Capture actual preview screens without importing credentials or sample servers."""
from pathlib import Path
import re
import base64
import shutil
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
    # Compact screenshots in CI logs let reviewers inspect pixels when artifact download
    # is unavailable. Full-resolution originals remain in the normal smoke artifact.
    if shutil.which("ffmpeg"):
        preview = subprocess.run(["ffmpeg", "-loglevel", "error", "-i", str(OUT / (name + ".png")),
                                  "-vf", "scale=432:-1", "-frames:v", "1", "-q:v", "4",
                                  "-f", "image2pipe", "-vcodec", "mjpeg", "pipe:1"],
                                 capture_output=True, timeout=25, check=True)
        print("FF_PREVIEW:" + name + ":" + base64.b64encode(preview.stdout).decode(), flush=True)
    return root

def tap_label(label):
    root = snapshot("navigation")
    for node in root.iter("node"):
        matches = any(label in (n.get("text", ""), n.get("content-desc", "")) for n in node.iter("node"))
        if matches and node.get("clickable") == "true":
            x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds", "")))
            adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
            time.sleep(1)
            return
    raise AssertionError("Missing clickable label: " + label)

def navigation(root, selected):
    tabs = {}
    for node in root.iter("node"):
        texts = [n.get("text") for n in node.iter("node") if n.get("text")]
        if node.get("clickable") == "true" and len(texts) == 1 and texts[0] in ("连接", "节点", "AI 分流"):
            previous = tabs.get(texts[0])
            top = int(re.findall(r"\d+", node.get("bounds", ""))[1])
            if previous is None or top > int(re.findall(r"\d+", previous.get("bounds", ""))[1]):
                tabs[texts[0]] = node
    assert set(tabs) == {"连接", "节点", "AI 分流"}, "Navigation disappeared: " + str(tabs.keys())
    assert len({re.findall(r"\d+", n.get("bounds", ""))[1] for n in tabs.values()}) == 1
    assert [label for label, node in tabs.items() if node.get("selected") == "true"] == [selected]
    return {label: tuple(map(int, re.findall(r"\d+", node.get("bounds", "")))) for label, node in tabs.items()}

def tap_bounds(bounds):
    x1, y1, x2, y2 = bounds
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))

def focus_tab(label, direction, activation):
    # Exercise actual input focus, not accessibility focus or coordinate taps.
    for attempt in range(24):
        root = snapshot("focus-" + direction)
        focused = [n for n in root.iter("node") if n.get("focused") == "true"]
        if any(label in " ".join(c.get("text", "") for c in n.iter("node")) for n in focused):
            adb("shell", "input", "keyevent", activation)
            time.sleep(1)
            navigation(snapshot("activated-" + direction), label)
            return
        adb("shell", "input", "keyevent", direction)
    raise AssertionError("Keyboard/D-pad cannot focus navigation tab: " + label)

adb("shell", "svc", "power", "stayon", "true")
adb("shell", "input", "keyevent", "224")
adb("shell", "wm", "dismiss-keyguard")
adb("shell", "cmd", "locale", "set-app-locales", "tech.future.fast", "--user", "0", "--locales", "zh-CN")
adb("shell", "am", "force-stop", "tech.future.fast")
adb("shell", "am", "start", "-W", "-n", "tech.future.fast/com.v2ray.ang.ui.main.MainActivity")
time.sleep(8)
nav_bounds = navigation(capture("future-fast-home"), "连接")
tap_label("节点")
assert navigation(capture("future-fast-nodes"), "节点") == nav_bounds
tap_label("AI 分流")
assert navigation(capture("future-fast-ai"), "AI 分流") == nav_bounds
tap_label("连接")
assert navigation(snapshot("roundtrip-home"), "连接") == nav_bounds

# Keep system animations enabled and record real lens movement, including drag/release.
recording = subprocess.Popen(["adb", "shell", "screenrecord", "--time-limit", "22",
                              "--bit-rate", "4000000", "/sdcard/future-fast-navigation.mp4"])
time.sleep(1)
for label in ("节点", "AI 分流", "连接", "AI 分流", "节点", "连接"):
    tap_bounds(nav_bounds[label])
    time.sleep(0.8)
home = nav_bounds["连接"]
ai = nav_bounds["AI 分流"]
adb("shell", "input", "swipe", str((home[0]+home[2])//2), str((home[1]+home[3])//2),
    str((ai[0]+ai[2])//2), str((ai[1]+ai[3])//2), "900")
time.sleep(1)
navigation(snapshot("drag-to-ai"), "AI 分流")
recording.wait(timeout=30)
(OUT / "future-fast-navigation.mp4").write_bytes(adb("exec-out", "cat", "/sdcard/future-fast-navigation.mp4"))
tap_label("连接")

# A theme configuration change recreates the Activity. Restore the selected destination.
tap_label("节点")
adb("shell", "cmd", "uimode", "night", "yes")
time.sleep(2)
navigation(capture("future-fast-nodes-dark"), "节点")
adb("shell", "cmd", "uimode", "night", "no")
time.sleep(2)
navigation(snapshot("nodes-restored-light"), "节点")
focus_tab("AI 分流", "KEYCODE_TAB", "KEYCODE_ENTER")
focus_tab("连接", "KEYCODE_DPAD_LEFT", "KEYCODE_DPAD_CENTER")
focus_tab("节点", "KEYCODE_TAB", "KEYCODE_SPACE")
tap_label("连接")
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
