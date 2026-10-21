import { Want } from '@kit.AbilityKit';
import { vpnExtension, VpnExtensionAbility, connection } from '@kit.NetworkKit';
import { hilog } from '@kit.PerformanceAnalysisKit';
import * as native from 'libfuturefast.so';
import { BUNDLE_NAME, coreConfig, hevConfig, validateProfile } from '../model/Profile';
import { SettingsStore } from '../services/SettingsStore';
import { ownsRequest, SessionRecord } from '../model/SessionPolicy';

export default class FutureFastVpnAbility extends VpnExtensionAbility {
  private connection: vpnExtension.VpnConnection | undefined = undefined;
  private cancelled: boolean = false;
  private startTask: Promise<void> = Promise.resolve();
  private cleanupTask: Promise<void> | undefined = undefined;
  private requestId: string = '';
  private timer: number = -1;
  private heartbeatBusy: boolean = false;

  onCreate(_: Want): void { this.startTask = this.startSession(); }
  onRequest(_: Want, _startId: number): void { /* Single-session owner: no duplicate start. */ }
  onDestroy(): void {
    this.cancelled = true;
    this.startTask.then(() => { this.cleanup(); });
  }

  private checkCancelled(): void { if (this.cancelled) { throw new Error('cancelled'); } }

  private async publish(state: string): Promise<void> {
    const current = await SettingsStore.session(this.context);
    if (!ownsRequest(current, this.requestId)) { return; }
    if (state === 'tunnel' && current.state !== 'starting' && current.state !== 'tunnel') {
      this.cancelled = true;
      throw new Error('cancelled');
    }
    const record: SessionRecord = { requestId: this.requestId, state: state, updatedAt: Date.now() };
    await SettingsStore.setSession(this.context, record);
  }

  private async startSession(): Promise<void> {
    try {
      const request = await SettingsStore.session(this.context);
      this.requestId = request.requestId;
      if (request.state !== 'starting' || this.requestId.length === 0) { return; }
      const saved = await SettingsStore.load(this.context);
      if (saved.termsVersion !== 1 || validateProfile(saved.profile).length !== 0) { throw new Error('profile'); }
      // Resolve the server before installing VPN DNS/routes. The core receives
      // an IP address and cannot recurse into the tunnel while finding its server.
      const addresses = await connection.getAddressesByName(saved.profile.host);
      if (addresses.length === 0) { throw new Error('dns'); }
      const json = coreConfig(saved.profile, addresses[0].address);
      this.checkCancelled();
      this.connection = vpnExtension.createVpnConnection(this.context);
      const config: vpnExtension.VpnConfig = {
        addresses: [
          { address: { address: '198.18.0.1', family: 1 }, prefixLength: 30 },
          { address: { address: 'fdfe:dcba:9876::1', family: 2 }, prefixLength: 64 }
        ],
        routes: [
          { interface: 'vpn-tun', destination: { address: { address: '0.0.0.0', family: 1 }, prefixLength: 0 },
            gateway: { address: '0.0.0.0', family: 1 }, hasGateway: false, isDefaultRoute: true },
          { interface: 'vpn-tun', destination: { address: { address: '::', family: 2 }, prefixLength: 0 },
            gateway: { address: '::', family: 2 }, hasGateway: false, isDefaultRoute: true }
        ],
        dnsAddresses: ['198.18.0.2'], mtu: 1500, isIPv4Accepted: true, isIPv6Accepted: true,
        // API 20 supports bundle exclusion. It prevents the native core's own
        // transport sockets from entering the VPN without relying on API 22's
        // protectProcessNet. Future Fast itself intentionally bypasses the VPN.
        blockedApplications: [BUNDLE_NAME]
      };
      const fd = await this.connection.create(config);
      this.checkCancelled();
      const result = await native.start(fd, json, hevConfig());
      this.checkCancelled();
      if (result !== 0) { throw new Error('native'); }
      await this.publish('tunnel');
      this.timer = setInterval(() => { this.heartbeat(); }, 1000);
    } catch (_) {
      hilog.error(0xF001, 'FutureFast', 'VPN session startup failed');
      await this.cleanup();
      if (!this.cancelled) { await this.publishError(); }
    }
  }

  private async publishError(): Promise<void> {
    try { await this.publish('error'); }
    catch (_) { hilog.error(0xF001, 'FutureFast', 'Session failure status save failed'); }
  }

  private async heartbeat(): Promise<void> {
    if (this.heartbeatBusy || this.cancelled) { return; }
    this.heartbeatBusy = true;
    try {
      if (!native.isRunning()) {
        this.cancelled = true;
        await this.cleanup();
        await this.publishError();
      } else {
        await this.publish('tunnel');
      }
    } catch (_) {
      hilog.error(0xF001, 'FutureFast', 'VPN heartbeat failed');
      this.cancelled = true;
      await this.cleanup();
    } finally { this.heartbeatBusy = false; }
  }

  private cleanup(): Promise<void> {
    if (this.cleanupTask !== undefined) { return this.cleanupTask; }
    this.cleanupTask = this.cleanupInner();
    return this.cleanupTask;
  }

  private async cleanupInner(): Promise<void> {
    if (this.timer >= 0) { clearInterval(this.timer); this.timer = -1; }
    try { await native.stop(); }
    catch (_) { hilog.error(0xF001, 'FutureFast', 'Native cleanup failed'); }
    if (this.connection !== undefined) {
      try { await this.connection.destroy(); }
      catch (_) { hilog.error(0xF001, 'FutureFast', 'VPN interface cleanup failed'); }
      this.connection = undefined;
    }
    try { await this.publish('idle'); }
    catch (_) { hilog.error(0xF001, 'FutureFast', 'Session cleanup status save failed'); }
  }
}
