"""Run on a disposable emulator only; uses loopback fixtures and changes its test data."""
from future_fast_device import *
import http.server,threading,base64
class Handler(http.server.BaseHTTPRequestHandler):
 def do_GET(self):
  data=b'<html>invalid subscription</html>' if self.path=='/bad' else base64.b64encode(b'socks://127.0.0.1:10904#SUB-IMPORTED')
  self.send_response(200);self.end_headers();self.wfile.write(data)
 def log_message(self,*a):pass
server=http.server.ThreadingHTTPServer(('127.0.0.1',18080),Handler);threading.Thread(target=server.serve_forever,daemon=True).start();adb('reverse','tcp:18080','tcp:18080')
try:
 launch();copy_fixture_url('http://127.0.0.1:18080/sub');tap('节点');tap('添加服务');tap('从剪贴板导入');time.sleep(3)
 r=snap('default-imported');assert 'SUB-IMPORTED' in label(r) and 'Default' in label(r) and 'import sub' not in label(r)
 print('PASS: real clipboard URL import populates Default without import sub',flush=True)
 # The failed candidate must leave the saved subscription metadata and existing nodes intact.
 copy_fixture_url('http://127.0.0.1:18080/bad');tap('添加服务');tap('从剪贴板导入');time.sleep(3)
 r=snap('default-bad-import');assert all(x in label(r) for x in ['SUB-IMPORTED','TEST-US','TEST-JP','TEST-SG'])
 tap('设置');tap('管理订阅');r=snap('default-subscription-metadata');assert 'http://127.0.0.1:18080/sub' in label(r) and '/bad' not in label(r) and 'import sub' not in label(r)
 print('PASS: invalid response preserves Default nodes and saved subscription URL',flush=True)
 back();back();delay_dialog();field('http://probe.fixture/generate_204');back();tap('确定');back()
 launch();tap('节点');assert 'SUB-IMPORTED' in label(snap('default-import-persisted'))
 print('PASS: imported nodes survive process death; local health-probe URL configured',flush=True)
finally:
 server.shutdown();adb('reverse','--remove','tcp:18080')
