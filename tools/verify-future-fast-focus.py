"""Keyboard, D-pad and rotation checks after the routing fixture tests."""
from pathlib import Path
import os,subprocess,time,xml.etree.ElementTree as E
assert os.environ.get("ANDROID_SERIAL", "").startswith("emulator-"), "Use a disposable emulator"
out=Path('smoke')
def adb(*a): return subprocess.check_output(['adb',*a],timeout=35)
def tree(name):
    adb('shell','uiautomator','dump','/sdcard/ff-focus.xml')
    data=adb('shell','cat','/sdcard/ff-focus.xml');(out/(name+'.xml')).write_bytes(data);return E.fromstring(data)
def label(n): return '\n'.join(x.get('text','') for x in n.iter('node'))
def switch(r): return next(n for n in r.iter('node') if '启用 AI 分流' in label(n) and n.get('checkable')=='true')
for i in range(18):
    r=tree('focus-search')
    if any(n.get('focused')=='true' and '启用 AI 分流' in label(n) for n in r.iter('node')): break
    adb('shell','input','keyevent','61')
else: raise AssertionError('AI switch was not reachable by keyboard')
assert switch(r).get('checked')=='true'
adb('shell','input','keyevent','66');time.sleep(3)
assert switch(tree('keyboard-enter-off')).get('checked')=='false'
adb('shell','input','keyevent','23');time.sleep(3)
assert switch(tree('dpad-center-on')).get('checked')=='true'
print('PASS: keyboard Tab/Enter and D-pad center activate the same AI switch',flush=True)
adb('shell','settings','put','system','accelerometer_rotation','0')
adb('shell','settings','put','system','user_rotation','1');time.sleep(3)
adb('shell','settings','put','system','user_rotation','0');time.sleep(3)
r=tree('rotation-restored-ai')
for service,name in [('ChatGPT','TEST-US'),('Claude','TEST-JP'),('Gemini','跟随默认节点')]:
    assert any(service in label(n) and name in label(n) and n.get('clickable')=='true' for n in r.iter('node')),(service,name)
print('PASS: AI screen and service assignments survive activity recreation by rotation',flush=True)
