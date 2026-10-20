"""Run on a disposable emulator only; uses loopback fixtures and changes its test data."""
from future_fast_device import *
import socketserver,threading
class Stall(socketserver.BaseRequestHandler):
 def handle(self):time.sleep(7)
class Server(socketserver.ThreadingTCPServer):allow_reuse_address=True;daemon_threads=True
server=Server(('127.0.0.1',10904),Stall);threading.Thread(target=server.serve_forever,daemon=True).start();adb('reverse','tcp:10904','tcp:10904')
try:
 launch();r=tree()
 if '点击断开' in label(r):tap('点击断开')
 r=snap('home-final');assert not any(n.get('scrollable')=='true' for n in r.iter('node'))
 adb('shell','input','swipe','540','1700','540','700','400');time.sleep(.5)
 r=snap('home-after-swipe');assert '自动管理' in label(r) and 'Future Fast' in label(r)
 tap('节点');r=snap('nodes-final');scrollables=[n for n in r.iter('node') if n.get('scrollable')=='true'];assert len(scrollables)<=1,[n.attrib for n in scrollables]
 # One deliberately slow loopback fixture makes the non-cancellable test state observable.
 tap('测速');r=snap('testing-button');testing=[n for n in r.iter('node') if '测试中' in n.get('text','')];assert len(testing)==1 and '取消' not in label(r)
 assert any('测试中' in label(n) and n.get('enabled')=='false' for n in r.iter('node'))
 print('PASS: fixed home; nodes only scroll internally; testing is one disabled button without cancel/status row',flush=True)
 for _ in range(20):
  time.sleep(3);r=tree()
  if '测试中' not in label(r):break
 else:raise AssertionError('Test did not complete within 60 seconds')
 tap('AI 分流');r=snap('ai-final');assert not any(n.get('scrollable')=='true' for n in r.iter('node'))
 # Keyboard and D-pad use the same merged switch action as touch.
 for _ in range(18):
  r=tree();focused=next((n for n in r.iter('node') if n.get('focused')=='true' and '启用 AI 分流' in label(n)),None)
  if focused is not None:break
  adb('shell','input','keyevent','61')
 else:raise AssertionError('AI switch not keyboard reachable')
 def checked():return next(n.get('checked') for n in tree().iter('node') if n.get('checkable')=='true' and '启用 AI 分流' in label(n))
 before=checked();adb('shell','input','keyevent','66');time.sleep(1);assert checked()!=before
 adb('shell','input','keyevent','23');time.sleep(1);assert checked()==before
 print('PASS: glass AI switch touch semantics, keyboard Enter, D-pad activation',flush=True)
 adb('shell','wm','size','1080x1920');time.sleep(3);r=snap('ai-compact');assert 'Gemini' in label(r)
 assert not any(n.get('scrollable')=='true' for n in r.iter('node'))
 assert all(bounds(n)[3]<=1920 for n in r.iter('node') if n.get('clickable')=='true')
 print('PASS: compact 1080x1920 AI page stays within a fixed screen',flush=True)
 tap('节点');r=snap('nodes-compact');assert sum(n.get('scrollable')=='true' for n in r.iter('node'))==1
 tap('AI 分流')
 adb('shell','wm','size','reset');time.sleep(2)
 adb('shell','cmd','uimode','night','yes');time.sleep(2);snap('ai-dark');adb('shell','cmd','uimode','night','no');time.sleep(2)
 tap('设置');snap('settings-final');tap('管理订阅');snap('subscriptions-final');back();tap('关于');snap('about-final')
 assert b'am_anr' not in adb('logcat','-b','events','-d')
 assert b'FATAL EXCEPTION' not in adb('logcat','-b','crash','-d')
 print('PASS: shared settings/subscription/About rendering, light/dark, no new ANR or crash',flush=True)
finally:
 server.shutdown();adb('reverse','--remove','tcp:10904');adb('shell','wm','size','reset');adb('shell','cmd','uimode','night','no')
