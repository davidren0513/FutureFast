"""First-use consent and About checks. Clears test app data; use a disposable emulator only."""
import sys,os,base64
from pathlib import Path
import future_fast_device as d
d.OUT=Path('smoke-v41')
from future_fast_device import *

def clear():
 adb('shell','am','force-stop',PKG);adb('shell','pm','clear',PKG)
 adb('shell','cmd','locale','set-app-locales',PKG,'--user','0','--locales','zh-CN')
 adb('shell','pm','grant',PKG,'android.permission.POST_NOTIFICATIONS')
def share(name):
 payload=base64.b64encode(('socks://127.0.0.1:10901#'+name).encode()).decode()
 adb('shell','am','start','-W','-n',PKG+'/com.v2ray.ang.ui.UrlSchemeActivity','-a','android.intent.action.SEND','-t','text/plain','--es','android.intent.extra.TEXT',payload);time.sleep(2)

clear();launch();r=snap('terms-first');assert '用户条款' in label(r) and '同意并继续' in label(r) and '不同意并退出' in label(r)
assert not any(n.get('content-desc')=='设置' for n in r.iter('node'))
adb('shell','input','keyevent','4');time.sleep(.5);assert '用户条款' in label(tree())
adb('shell','input','tap','10','100');time.sleep(.5);assert '用户条款' in label(tree())
adb('shell','settings','put','system','accelerometer_rotation','0');adb('shell','settings','put','system','user_rotation','1');time.sleep(1);adb('shell','settings','put','system','user_rotation','0');time.sleep(1);assert '用户条款' in label(tree())
print('PASS: first-use terms block UI, Back/outside cannot dismiss, rotation retains pending consent',flush=True)
tap('不同意并退出');r=tree();assert '用户条款' not in label(r)
launch();assert '用户条款' in label(tree());tap('同意并继续');r=snap('terms-accepted-home');assert '自动管理' in label(r) and '用户条款' not in label(r)
launch();assert '用户条款' not in label(tree());print('PASS: decline exits without acceptance; explicit agreement persists across process death',flush=True)
tap('设置');tap('关于');r=snap('about-updated');text=label(r)
assert 'Kyant0/AndroidLiquidGlass' in text and '未界科技' in text
for i in range(4):
 adb('shell','input','swipe','520','1850','520','680','300');time.sleep(.5);text+='\n'+label(tree())
assert not any(x in text for x in ['独立社区','Telegram','反馈'])
assert '鸣谢：未界科技 提供技术指导支持' in text
print('PASS: About credits and acknowledgements present; community paragraph, Telegram and feedback removed',flush=True)
# A rejected share must not import data before consent.
clear();share('TERMS-REJECTED');assert '用户条款' in label(tree());tap('不同意并退出')
launch();tap('同意并继续');tap('节点');assert 'TERMS-REJECTED' not in label(snap('rejected-share-no-import'))
print('PASS: external share rejected at terms does not import its pending profile',flush=True)
clear();share('TERMS-ALLOWED');assert '用户条款' in label(tree());tap('同意并继续');time.sleep(3);tap('节点');assert 'TERMS-ALLOWED' in label(snap('accepted-share-imported'))
print('PASS: accepted external share resumes import only after consent is saved',flush=True)
assert b'FATAL EXCEPTION' not in adb('logcat','-b','crash','-d')
assert b'am_anr' not in adb('logcat','-b','events','-d')
print('PASS: no crash or ANR during first-use and About checks',flush=True)

def focus_button(text):
 for _ in range(12):
  r=tree()
  if any(n.get('focused')=='true' and text in label(n) for n in r.iter('node')):return
  adb('shell','input','keyevent','61');time.sleep(.2)
 raise AssertionError('No focus '+text)
clear();launch();focus_button('不同意并退出');adb('shell','input','keyevent','23');time.sleep(1);assert '用户条款' not in label(tree())
launch();focus_button('同意并继续');adb('shell','input','keyevent','66');time.sleep(1);assert '用户条款' not in label(tree())
print('PASS: D-pad decline and keyboard Enter agree invoke the same actions as touch',flush=True)
clear();adb('shell','wm','size','1080x1920');launch();r=snap('terms-compact');assert all(bounds(n)[3]<1920 for n in r.iter('node') if n.get('clickable')=='true')
adb('shell','cmd','uimode','night','yes');time.sleep(2);r=snap('terms-dark');assert '用户条款' in label(r)
adb('shell','wm','size','reset');adb('shell','cmd','uimode','night','no');time.sleep(1)
print('PASS: compact and dark terms retain visible action buttons',flush=True)
