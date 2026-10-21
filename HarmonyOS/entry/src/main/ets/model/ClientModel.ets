import { common } from '@kit.AbilityKit';
import { vpnExtension } from '@kit.NetworkKit';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { BUNDLE_NAME, emptyProfile, importShadowsocks, ProxyProfile, validateProfile } from './Profile';
import { SettingsStore } from '../services/SettingsStore';
import { visibleSession } from './SessionPolicy';

@Observed
export class ClientModel {
  draft: ProxyProfile = emptyProfile();
  portText: string = '443';
  agreed: boolean = false;
  state: string = 'idle';
  canDisconnect: boolean = false;
  feedback: string = '';
  ready: boolean = false;
  private context: common.Context | undefined = undefined;
  private timer: number = -1;
  private polling: boolean = false;
  private disposed: boolean = false;
  private actionBusy: boolean = false;

  async initialize(context: common.Context): Promise<void> {
    this.context = context;
    this.disposed = false;
    try {
      const settings = await SettingsStore.load(context);
      this.draft = settings.profile;
      this.portText = this.draft.port.toString();
      this.agreed = settings.termsVersion === 1;
      await this.refresh();
      if (!this.disposed) {
        this.ready = true;
        this.timer = setInterval(() => { this.refresh(); }, 1000);
      }
    } catch (_) { this.feedback = 'save_error'; this.ready = true; }
  }

  dispose(): void {
    this.disposed = true;
    if (this.timer >= 0) { clearInterval(this.timer); this.timer = -1; }
  }

  importLink(text: string): void {
    try {
      this.draft = importShadowsocks(text);
      this.portText = this.draft.port.toString();
      this.feedback = 'import_ok';
    } catch (_) { this.feedback = 'import_failed'; }
  }

  field(key: string, value: string): void {
    const old = this.draft;
    const next: ProxyProfile = { id: old.id, name: old.name, host: old.host,
      port: old.port, method: old.method, password: old.password };
    if (key === 'name') { next.name = value; }
    if (key === 'host') { next.host = value.trim(); }
    if (key === 'password') { next.password = value; }
    if (key === 'method') { next.method = value; }
    if (key === 'port') {
      this.portText = value;
      next.port = /^\d+$/.test(value) ? Number(value) : 0;
    }
    this.draft = next;
  }

  busy(): boolean { return this.state === 'starting' || this.state === 'stopping' || this.actionBusy; }

  async toggle(): Promise<void> {
    if (this.context === undefined || this.actionBusy || !this.ready) { return; }
    this.actionBusy = true;
    const context = this.context;
    const want = { bundleName: BUNDLE_NAME, abilityName: 'FutureFastVpnAbility' };
    try {
      if (this.canDisconnect) {
        this.state = 'stopping';
        const current = await SettingsStore.session(context);
        await SettingsStore.setSession(context, { requestId: current.requestId, state: 'stopping', updatedAt: Date.now() });
        await vpnExtension.stopVpnExtensionAbility(want);
        await SettingsStore.setSession(context, { requestId: current.requestId, state: 'idle', updatedAt: Date.now() });
        this.state = 'idle';
        this.canDisconnect = false;
      } else {
        if (!this.agreed) { this.feedback = 'terms_required'; return; }
        if (validateProfile(this.draft).length !== 0) { this.feedback = 'field_error'; return; }
        await SettingsStore.save(context, this.draft, 1);
        this.feedback = '';
        this.state = 'starting';
        this.canDisconnect = true;
        const requestId = Date.now().toString();
        await SettingsStore.setSession(context, { requestId: requestId, state: 'starting', updatedAt: Date.now() });
        // API 20 can discard Want parameters on the first authorization flow.
        // The extension reads the committed profile from its storage owner.
        await vpnExtension.startVpnExtensionAbility(want);
      }
    } catch (_) {
      this.state = 'error'; this.feedback = 'permission_error';
      hilog.error(0xF001, 'FutureFast', 'VPN control action failed');
    } finally { this.actionBusy = false; }
  }

  private async refresh(): Promise<void> {
    if (this.context === undefined || this.polling || this.disposed) { return; }
    this.polling = true;
    try {
      const record = await SettingsStore.session(this.context);
      if (!this.disposed && !this.actionBusy) {
        this.state = visibleSession(record, Date.now());
        this.canDisconnect = record.state === 'tunnel' || record.state === 'starting' || record.state === 'stopping';
        if (this.state === 'permission_timeout') {
          await SettingsStore.setSession(this.context, { requestId: record.requestId, state: 'error', updatedAt: Date.now() });
          try { await vpnExtension.stopVpnExtensionAbility({ bundleName: BUNDLE_NAME, abilityName: 'FutureFastVpnAbility' }); }
          catch (_) { hilog.warn(0xF001, 'FutureFast', 'Unconfirmed VPN start cleanup requested'); }
          this.state = 'error'; this.canDisconnect = false; this.feedback = 'permission_error';
        }
        if (this.state === 'error') { this.feedback = 'core_error'; }
      }
    } catch (_) {
      hilog.error(0xF001, 'FutureFast', 'Session status read failed');
    } finally { this.polling = false; }
  }
}
