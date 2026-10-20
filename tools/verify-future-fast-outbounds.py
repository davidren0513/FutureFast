"""Loopback-only Xray outbound probe; run after verify-future-fast-routing-ui.py."""
from pathlib import Path
import json, os, re, socket, socketserver, subprocess, threading, time
import xml.etree.ElementTree as ET

assert os.environ.get('ANDROID_SERIAL', '').startswith('emulator-'), 'Use a disposable emulator'
OUT=Path('smoke'); events=[]
def adb(*args):
    return subprocess.check_output(['adb',*args],timeout=35)
def tree():
    adb('shell','uiautomator','dump','/sdcard/ff-probe.xml')
    return ET.fromstring(adb('shell','cat','/sdcard/ff-probe.xml'))
def label(n):
    return '\n'.join(x.get('text','')+'\n'+x.get('content-desc','') for x in n.iter('node'))
def tap(text, largest=False):
    ns=[n for n in tree().iter('node') if text in label(n) and n.get('clickable')=='true']
    assert ns, 'Action missing: '+text
    def area(n):
        a,b,c,d=map(int,re.findall(r'\d+',n.get('bounds',''))); return (c-a)*(d-b)
    n=max(ns,key=area) if largest else min(ns,key=lambda x:len(label(x)))
    a,b,c,d=map(int,re.findall(r'\d+',n.get('bounds','')))
    adb('shell','input','tap',str((a+c)//2),str((b+d)//2)); time.sleep(3)
def exact(s,n):
    b=b''
    while len(b)<n:
        v=s.recv(n-len(b))
        if not v: raise ConnectionError('EOF')
        b+=v
    return b
class Proxy(socketserver.BaseRequestHandler):
    def handle(self):
        s=self.request; s.settimeout(15)
        try:
            v,n=exact(s,2); assert v==5; exact(s,n); s.sendall(b'\x05\x00')
            v,cmd,rsv,typ=exact(s,4); assert v==5 and cmd==1
            if typ==3: host=exact(s,exact(s,1)[0]).decode()
            elif typ==1: host=socket.inet_ntop(socket.AF_INET,exact(s,4))
            elif typ==4: host=socket.inet_ntop(socket.AF_INET6,exact(s,16))
            else: raise ValueError(typ)
            port=int.from_bytes(exact(s,2),'big'); events.append({'proxy':self.server.name,'host':host,'port':port})
            s.sendall(b'\x05\x00\x00\x01\x7f\x00\x00\x01\x00\x00')
            s.recv(8192)
            body=('FutureFast fixture '+self.server.name).encode()
            s.sendall(b'HTTP/1.1 200 OK\r\nConnection: close\r\nContent-Length: '+str(len(body)).encode()+b'\r\n\r\n'+body)
        except (ConnectionError,TimeoutError,OSError): pass
class Server(socketserver.ThreadingTCPServer):
    allow_reuse_address=True; daemon_threads=True

def probe(host,wanted):
    with socket.create_connection(('127.0.0.1',11080),timeout=10) as s:
        s.settimeout(15); s.sendall(b'\x05\x01\x00'); assert exact(s,2)==b'\x05\x00'
        h=host.encode(); s.sendall(b'\x05\x01\x00\x03'+bytes([len(h)])+h+b'\x00\x50')
        reply=exact(s,4); assert reply[1]==0,(host,reply)
        if reply[3]==1: exact(s,6)
        elif reply[3]==4: exact(s,18)
        else: exact(s,exact(s,1)[0]+2)
        s.sendall(b'GET / HTTP/1.1\r\nHost: '+h+b'\r\nConnection: close\r\n\r\n')
        response=b''
        while True:
            b=s.recv(8192)
            if not b: break
            response+=b
        assert ('FutureFast fixture '+wanted).encode() in response,(host,wanted,response)
        print('PASS:',host,'->',wanted,flush=True)

servers=[]
try:
    for port,name in [(10901,'US'),(10902,'JP'),(10903,'SG')]:
        s=Server(('127.0.0.1',port),Proxy); s.name=name; servers.append(s)
        threading.Thread(target=s.serve_forever,daemon=True).start()
        adb('reverse',f'tcp:{port}',f'tcp:{port}')
    tap('Gemini'); tap('TEST-SG')
    adb('shell','input','keyevent','4'); time.sleep(2)
    if '点击断开' in label(tree()): tap('点击断开')
    tap('连接',largest=True)
    root=tree()
    if any(n.get('package')=='com.android.vpndialogs' for n in root.iter('node')):
        tap('OK')
    time.sleep(5)
    assert '点击断开' in label(tree()),'VPN did not start'
    (OUT/'home-connected.png').write_bytes(adb('exec-out','screencap','-p'))
    adb('forward','tcp:11080','tcp:10808')
    probe('chatgpt.com','US'); probe('api.openai.com','US')
    probe('claude.ai','JP'); probe('api.anthropic.com','JP')
    probe('gemini.google.com','SG'); probe('generativelanguage.googleapis.com','SG')
    tap('AI 分流'); tap('Gemini'); tap('跟随默认节点')
    # Apply the changed configuration to the running native core.
    adb('shell','input','swipe','500','1700','500','700','350'); time.sleep(1)
    tap('重新连接并应用'); time.sleep(4)
    probe('gemini.google.com','JP'); probe('chatgpt.com','US'); probe('claude.ai','JP')
    (OUT/'outbound-routing.json').write_text(json.dumps(events,ensure_ascii=False,indent=2))
    print('PASS: real Xray SOCKS outbound selection and reconnect apply, using only loopback fixture servers')
finally:
    (OUT/'outbound-routing.json').write_text(json.dumps(events,ensure_ascii=False,indent=2))
    for s in servers: s.shutdown(); s.server_close()
    for port in (10901,10902,10903):
        subprocess.run(['adb','reverse','--remove',f'tcp:{port}'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
    subprocess.run(['adb','forward','--remove','tcp:11080'],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
