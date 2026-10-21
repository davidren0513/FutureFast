import { common } from '@kit.AbilityKit';
import { preferences } from '@kit.ArkData';
import { emptyProfile, ProxyProfile, validateProfile } from '../model/Profile';
import { SessionRecord } from '../model/SessionPolicy';

export interface SavedSettings {
  profile: ProxyProfile;
  termsVersion: number;
}
// HarmonyOS has a separate lifecycle and storage backend from the Android MMKV
// owner. Only this store accesses platform Preferences for the native prototype.
export class SettingsStore {
  static async load(context: common.Context): Promise<SavedSettings> {
    await preferences.removePreferencesFromCache(context, 'ff_settings');
    const store = await preferences.getPreferences(context, { name: 'ff_settings' });
    const text = await store.get('settings', '') as string;
    if (text.length === 0) { return { profile: emptyProfile(), termsVersion: 0 }; }
    const settings = JSON.parse(text) as SavedSettings;
    if (validateProfile(settings.profile).length !== 0 || settings.termsVersion !== 1) {
      throw new Error('saved_settings_invalid');
    }
    return settings;
  }

  static async save(context: common.Context, profile: ProxyProfile, termsVersion: number): Promise<void> {
    const store = await preferences.getPreferences(context, { name: 'ff_settings' });
    const settings: SavedSettings = { profile: profile, termsVersion: termsVersion };
    await store.put('settings', JSON.stringify(settings));
    await store.flush();
  }

  static async session(context: common.Context): Promise<SessionRecord> {
    // The VPN extension is a different process. Drop the process-local cache
    // before reads so UI polling cannot keep displaying a stale connection.
    await preferences.removePreferencesFromCache(context, 'ff_session');
    const store = await preferences.getPreferences(context, { name: 'ff_session' });
    const text = await store.get('session', '') as string;
    if (text.length === 0) { return { requestId: '', state: 'idle', updatedAt: 0 }; }
    return JSON.parse(text) as SessionRecord;
  }

  static async setSession(context: common.Context, record: SessionRecord): Promise<void> {
    const store = await preferences.getPreferences(context, { name: 'ff_session' });
    await store.put('session', JSON.stringify(record));
    await store.flush();
  }
}
