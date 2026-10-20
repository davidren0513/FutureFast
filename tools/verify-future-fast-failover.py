"""Run on a disposable emulator only; uses loopback fixtures and changes its test data."""
from future_fast_device import *
import socket,socketserver,threading,json
healthy={'US':True,'JP':True,'SG':True};events=[]
def exact(s,n):
 out=b''
 while len(out)<n:
  data=s.recv(n-len(out))
  if not data:raise ConnectionError('EOF')
  out+=data
 return out
class Proxy(socketserver.BaseRequestHandler):
 def handle(self):
  s=self.request;s.settimeout(15)
  try:
   if not healthy[self.server.name]:return
   v,n=exact(s,2);assert v==5;exact(s,n);s.sendall(b'\x05\x00')
   v,cmd,rsv,typ=exact(s,4)
   if typ==3:host=exact(s,exact(s,1)[0]).decode()
   elif typ==1:host=socket.inet_ntop(socket.AF_INET,exact(s,4))
   else:host=socket.inet_ntop(socket.AF_INET6,exact(s,16))
   port=int.from_bytes(exact(s,2),'big');events.append({'proxy':self.server.name,'host':host,'port':port})
   s.sendall(b'\x05\x00\x00\x01\x7f\x00\x00\x01\x00\x00');s.recv(8192)
   if host=='probe.fixture':s.sendall(b'HTTP/1.1 204 No Content\r\nConnection: close\r\n\r\n')
   else:
    body=('FutureFast fixture '+self.server.name).encode();s.sendall(b'HTTP/1.1 200 OK\r\nConnection: close\r\nContent-Length: '+str(len(body)).encode()+b'\r\n\r\n'+body)
  except (ConnectionError,TimeoutError,OSError):pass
class Server(socketserver.ThreadingTCPServer):allow_reuse_address=True;daemon_threads=True
def probe(host,wanted):
 with socket.create_connection(('127.0.0.1',11080),timeout=10) as s:
  s.settimeout(15);s.sendall(b'\x05\x01\x00');assert exact(s,2)==b'\x05\x00'
  h=host.encode();s.sendall(b'\x05\x01\x00\x03'+bytes([len(h)])+h+b'\x00\x50');r=exact(s,4);assert r[1]==0,(host,r)
  exact(s,6 if r[3]==1 else 18)
  s.sendall(b'GET / HTTP/1.1\r\nHost: '+h+b'\r\nConnection: close\r\n\r\n');response=b''
  while True:
   b=s.recv(8192)
   if not b:break
   response+=b
  assert ('FutureFast fixture '+wanted).encode() in response,(host,wanted,response)
  print('PASS:',host,'->',wanted,flush=True)
servers=[]
try:
 for port,name in [(10901,'US'),(10902,'JP'),(10903,'SG')]:
  server=Server(('127.0.0.1',port),Proxy);server.name=name;servers.append(server);threading.Thread(target=server.serve_forever,daemon=True).start();adb('reverse',f'tcp:{port}',f'tcp:{port}')
 launch();tap('AI 分流');tap('Gemini');tap('TEST-SG');back();tap('连接')
 r=tree()
 if 'com.android.vpndialogs' in E.tostring(r,encoding='unicode'):tap('OK')
 time.sleep(6);assert '点击断开' in label(snap('ai-connected'))
 adb('forward','tcp:11080','tcp:10808')
 probe('chatgpt.com','US');probe('api.openai.com','US');probe('claude.ai','JP');probe('gemini.google.com','SG')
 healthy['US']=False;time.sleep(15)
 probe('chatgpt.com','JP');probe('api.openai.com','JP');probe('gemini.google.com','SG')
 print('PASS: unreachable dedicated ChatGPT node falls back to default without reconnecting',flush=True)
 healthy['US']=True;time.sleep(15);probe('chatgpt.com','US')
 print('PASS: recovered ChatGPT node is used again after health detection',flush=True)
 tap('AI 分流');tap('Gemini');tap('跟随默认节点');tap('重新连接并应用');time.sleep(5)
 probe('gemini.google.com','JP');probe('chatgpt.com','US');probe('claude.ai','JP')
 snap('ai-fallback-settings');print('PASS: manual default reset preserves other service routes',flush=True)
 tap('节点');r=tree()
 rows=[n for n in r.iter('node') if 'TEST-US' in label(n) and '更多选项' in label(n)]
 row=min(rows,key=lambda n:len(label(n)))
 menu=min([n for n in row.iter('node') if n.get('clickable')=='true' and '更多选项' in label(n)],key=lambda n:len(label(n)))
 a,b,c,d=bounds(menu);adb('shell','input','tap',str((a+c)//2),str((b+d)//2));time.sleep(1);tap('删除');time.sleep(1)
 r=tree()
 if '确定' in label(r):tap('确定')
 tap('AI 分流');r=snap('ai-node-deleted');assert '指定节点不可用' in label(r)
 tap('重新连接并应用');time.sleep(4);probe('chatgpt.com','JP');probe('claude.ai','JP')
 print('PASS: deleted assigned node also routes to default rather than blocked outbound',flush=True)
finally:
 (OUT/'health-routing.json').write_text(json.dumps(events,indent=2))
 for s in servers:s.shutdown();s.server_close()
 for port in [10901,10902,10903]:subprocess.run([ADB,'-s',SERIAL,'reverse','--remove',f'tcp:{port}'],capture_output=True)
 subprocess.run([ADB,'-s',SERIAL,'forward','--remove','tcp:11080'],capture_output=True)
