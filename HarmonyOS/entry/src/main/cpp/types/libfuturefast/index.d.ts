export const start: (fd: number, shadowsocksJson: string, hevYaml: string) => Promise<number>;
export const stop: () => Promise<number>;
export const isRunning: () => boolean;
