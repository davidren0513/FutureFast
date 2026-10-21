export interface SessionRecord {
  requestId: string;
  state: string;
  updatedAt: number;
}

export function visibleSession(record: SessionRecord, now: number): string {
  if (['idle', 'starting', 'tunnel', 'stopping', 'error'].indexOf(record.state) < 0) { return 'error'; }
  if (record.state === 'tunnel' && now - record.updatedAt > 10000) { return 'unknown'; }
  if (record.state === 'starting' && now - record.updatedAt > 60000) { return 'permission_timeout'; }
  return record.state;
}

export function ownsRequest(record: SessionRecord, requestId: string): boolean {
  return requestId.length > 0 && record.requestId === requestId;
}
