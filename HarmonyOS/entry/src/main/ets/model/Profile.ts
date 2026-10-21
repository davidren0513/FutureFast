// Platform-independent configuration owner, shared by ArkTS and host regression tests.
export interface ProxyProfile {
  id: string;
  name: string;
  host: string;
  port: number;
  method: string;
  password: string;
}

export const METHODS: string[] = ['aes-256-gcm', 'aes-128-gcm', 'chacha20-ietf-poly1305'];
export const LOCAL_PORT: number = 10808;
export const BUNDLE_NAME: string = 'tech.future.fast.harmony';

export function emptyProfile(): ProxyProfile {
  return { id: 'primary', name: '', host: '', port: 443, method: METHODS[0], password: '' };
}

function ipv6(host: string): boolean {
  if (!/^[a-fA-F0-9:]+$/.test(host) || host.indexOf(':') < 0) { return false; }
  const halves = host.split('::');
  if (halves.length > 2) { return false; }
  const groups = host.split(':').filter((part: string) => part.length > 0);
  if (groups.some((part: string) => part.length > 4)) { return false; }
  return halves.length === 2 ? groups.length < 8 : groups.length === 8;
}

export function validHost(host: string): boolean {
  if (host.length === 0 || host.length > 253 || /[\s/\\@?#"']/.test(host)) { return false; }
  if (host.indexOf(':') >= 0) { return ipv6(host); }
  if (/^[0-9.]+$/.test(host)) {
    const parts = host.split('.');
    return parts.length === 4 && parts.every((part: string) => /^\d{1,3}$/.test(part) && Number(part) <= 255);
  }
  return host.split('.').every((part: string) => /^[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?$/.test(part));
}

export function validateProfile(profile: ProxyProfile): string {
  if (!validHost(profile.host)) { return 'host'; }
  if (!Number.isInteger(profile.port) || profile.port < 1 || profile.port > 65535) { return 'port'; }
  if (METHODS.indexOf(profile.method) < 0) { return 'method'; }
  if (profile.password.length === 0 || profile.password.length > 1024 || profile.password.indexOf('\0') >= 0) { return 'password'; }
  return '';
}

function base64Utf8(value: string): string {
  const alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
  const text = value.replace(/-/g, '+').replace(/_/g, '/').replace(/=+$/, '');
  if (!/^[A-Za-z0-9+/]+$/.test(text) || text.length % 4 === 1) { throw new Error('format'); }
  let bits = 0;
  let count = 0;
  let escaped = '';
  for (let i = 0; i < text.length; i++) {
    bits = (bits << 6) | alphabet.indexOf(text.charAt(i));
    count += 6;
    if (count >= 8) {
      count -= 8;
      const byte = (bits >> count) & 255;
      escaped += '%' + byte.toString(16).padStart(2, '0');
      bits &= (1 << count) - 1;
    }
  }
  if (bits !== 0) { throw new Error('format'); }
  return decodeURIComponent(escaped);
}

/** SIP002 user-info and legacy whole-payload links; unsupported plugins fail explicitly. */
export function importShadowsocks(value: string): ProxyProfile {
  const link = value.trim();
  if (!link.startsWith('ss://') || link.length > 16384) { throw new Error('format'); }
  let text = link.slice(5);
  const hash = text.indexOf('#');
  const name = hash < 0 ? '' : decodeURIComponent(text.slice(hash + 1));
  if (hash >= 0) { text = text.slice(0, hash); }
  const query = text.indexOf('?');
  if (query >= 0 && text.slice(query + 1).length > 0) { throw new Error('plugin'); }
  if (query >= 0) { text = text.slice(0, query); }
  if (text.indexOf('@') < 0) { text = base64Utf8(text); }
  const at = text.lastIndexOf('@');
  if (at <= 0) { throw new Error('format'); }
  let user = decodeURIComponent(text.slice(0, at));
  if (user.indexOf(':') < 0) { user = base64Utf8(user); }
  const colon = user.indexOf(':');
  if (colon <= 0) { throw new Error('format'); }
  const endpoint = text.slice(at + 1).replace(/\/$/, '');
  let host: string;
  let portText: string;
  if (endpoint.startsWith('[')) {
    const close = endpoint.indexOf(']:');
    if (close < 0) { throw new Error('format'); }
    host = endpoint.slice(1, close);
    portText = endpoint.slice(close + 2);
  } else {
    const split = endpoint.lastIndexOf(':');
    if (split < 1) { throw new Error('format'); }
    host = endpoint.slice(0, split);
    portText = endpoint.slice(split + 1);
    if (host.indexOf(':') >= 0) { throw new Error('format'); }
  }
  if (!/^\d+$/.test(portText)) { throw new Error('port'); }
  const profile: ProxyProfile = { id: 'primary', name: name, host: host, port: Number(portText),
    method: user.slice(0, colon), password: user.slice(colon + 1) };
  const error = validateProfile(profile);
  if (error.length > 0) { throw new Error(error); }
  return profile;
}

interface ShadowsocksConfig {
  server: string;
  server_port: number;
  method: string;
  password: string;
  local_address: string;
  local_port: number;
  mode: string;
}

export function coreConfig(profile: ProxyProfile, resolvedHost: string): string {
  const error = validateProfile(profile);
  if (error.length > 0 || !validHost(resolvedHost)) { throw new Error('profile'); }
  const config: ShadowsocksConfig = { server: resolvedHost, server_port: profile.port,
    method: profile.method, password: profile.password, local_address: '127.0.0.1',
    local_port: LOCAL_PORT, mode: 'tcp_and_udp' };
  return JSON.stringify(config);
}

export function hevConfig(): string {
  // Fake DNS maps domains locally and sends domain SOCKS requests to sslocal;
  // the actual destination is resolved on the Shadowsocks server.
  return `tunnel:\n  mtu: 1500\n  ipv4: 198.18.0.1\n  ipv6: 'fdfe:dcba:9876::1'\n` +
    `socks5:\n  address: 127.0.0.1\n  port: ${LOCAL_PORT}\n  udp: 'udp'\n` +
    `mapdns:\n  address: 198.18.0.2\n  port: 53\n  network: 198.19.0.0\n  netmask: 255.255.0.0\n` +
    `misc:\n  log-level: error\n  log-file: stderr\n  connect-timeout: 10000\n`;
}
