"""UI helpers for the disposable Future Fast regression emulator; no user credentials."""
from pathlib import Path
import subprocess,time,re,os,shutil,xml.etree.ElementTree as E
OUT=Path('smoke-v4');OUT.mkdir(exist_ok=True)
ADB=shutil.which('adb') or str(Path('.toolchain/sdk/platform-tools/adb').resolve())
SERIAL=os.environ.get('ANDROID_SERIAL','')
assert SERIAL.startswith('emulator-'), 'Use a disposable emulator and set ANDROID_SERIAL'
PKG='tech.future.fast'
def adb(*a): return subprocess.check_output([ADB,'-s',SERIAL,*a],timeout=40)
def tree():
 for attempt in range(4):
  out=adb('shell','uiautomator','dump','/sdcard/ff-v4.xml')
  if b'dumped to' in out:
   root=E.fromstring(adb('shell','cat','/sdcard/ff-v4.xml'))
   if any(n.get('clickable')=='true' for n in root.iter('node')):return root
  time.sleep(1)
 raise AssertionError('No accessibility root')
def label(n): return '\n'.join(x.get('text','')+'\n'+x.get('content-desc','') for x in n.iter('node'))
def bounds(n): return tuple(map(int,re.findall(r'\d+',n.get('bounds',''))))
def tap(text):
 r=tree();ns=[n for n in r.iter('node') if text in label(n) and n.get('clickable')=='true']
 assert ns, 'Missing '+text+' in '+label(r)[:300]
 n=min(ns,key=lambda n:len(label(n)));a,b,c,d=bounds(n);adb('shell','input','tap',str((a+c)//2),str((b+d)//2));time.sleep(1)
def snap(name):
 r=tree();(OUT/(name+'.xml')).write_bytes(E.tostring(r));(OUT/(name+'.png')).write_bytes(adb('exec-out','screencap','-p'));return r
def back():adb('shell','input','keyevent','4');time.sleep(1)
def launch():
 adb('shell','input','keyevent','224');adb('shell','wm','dismiss-keyguard');adb('shell','am','force-stop',PKG);adb('shell','am','start','-W','-n',PKG+'/com.v2ray.ang.ui.main.MainActivity');time.sleep(2)
def scroll_to(text):
 for _ in range(10):
  r=tree()
  if text in label(r): return
  adb('shell','input','swipe','520','1920','520','660','300');time.sleep(.5)
 raise AssertionError('Not found '+text)
def field(text):
 n=next(n for n in tree().iter('node') if n.get('class')=='android.widget.EditText');a,b,c,d=bounds(n)
 adb('shell','input','tap',str((a+c)//2),str((b+d)//2));adb('shell','input','keycombination','113','29');adb('shell','input','text',text);time.sleep(.5)
def delay_dialog():
 tap('设置');tap('用户界面设置');tap('VPN 设置');tap('核心设置');scroll_to('真连接延迟测试 URL');tap('真连接延迟测试 URL')
def copy_fixture_url(url):
 delay_dialog();field(url);adb('shell','input','keycombination','113','29');adb('shell','input','keycombination','113','52');back();tap('取消');back()
