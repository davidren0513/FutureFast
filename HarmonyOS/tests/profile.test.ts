import { test } from 'node:test';
import assert from 'node:assert/strict';
import { emptyProfile, validateProfile, importShadowsocks, coreConfig, hevConfig, validHost } from '../entry/src/main/ets/model/Profile.ts';

const auth = (value: string) => Buffer.from(value).toString('base64url');
test('SIP002 imports retain password colons, Unicode label and stable identity', () => {
  const p = importShadowsocks(`ss://${auth('aes-256-gcm:p:a:ss')}@proxy.example:443#%E6%B5%8B%E8%AF%95`);
  assert.equal(p.password, 'p:a:ss'); assert.equal(p.name, '测试'); assert.equal(p.id, 'primary');
  assert.equal(validateProfile(p), '');
});
test('legacy links and bracketed IPv6 work', () => {
  assert.equal(importShadowsocks('ss://' + auth('aes-128-gcm:secret@proxy.example:8388')).port, 8388);
  assert.equal(importShadowsocks(`ss://${auth('aes-256-gcm:secret')}@[2001:db8::1]:443/`).host, '2001:db8::1');
});
test('plugins and other protocols fail rather than silently losing configuration', () => {
  for (const value of ['vless://node', `ss://${auth('aes-256-gcm:p')}@example.com:443?plugin=obfs-local`,
    `ss://${auth('rc4-md5:p')}@example.com:443`, 'ss://%%%']) { assert.throws(() => importShadowsocks(value)); }
});
test('bad endpoints, ambiguous ports and empty passwords are rejected', () => {
  for (const endpoint of ['host:0', 'host:65536', 'host:4.5', 'host:1e2', 'a/b:443', '[::1:443', '999.1.1.1:443']) {
    assert.throws(() => importShadowsocks(`ss://${auth('aes-256-gcm:p')}@${endpoint}`));
  }
  const p = emptyProfile(); assert.equal(validateProfile(p), 'host');
  p.host = 'example.com'; assert.equal(validateProfile(p), 'password');
  p.password = 'secret'; p.port = 1.5; assert.equal(validateProfile(p), 'port');
});
test('JSON serialization preserves special characters without injecting extra fields', () => {
  const p = importShadowsocks(`ss://${auth('chacha20-ietf-poly1305:p"a\\ss')}@example.com:443`);
  const config = JSON.parse(coreConfig(p, '192.0.2.1'));
  assert.equal(config.password, p.password); assert.equal(config.server, '192.0.2.1');
  assert.equal(config.local_address, '127.0.0.1'); assert.equal(config.mode, 'tcp_and_udp');
  assert.equal(Object.keys(config).length, 7);
});
test('host and HEV configuration reject injection and keep dual-stack local routing', () => {
  for (const host of ['host\ncommand', 'http://example.com', '-host', 'a..b', '1::2::3', '']) { assert.equal(validHost(host), false); }
  assert.equal(validHost('2001:db8::1'), true);
  assert.match(hevConfig(), /mapdns:/); assert.match(hevConfig(), /ipv6:/);
  assert.match(hevConfig(), /address: 127\.0\.0\.1/);
});
