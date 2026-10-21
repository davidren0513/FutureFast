import { test } from 'node:test';
import assert from 'node:assert/strict';
import { visibleSession, ownsRequest } from '../entry/src/main/ets/model/SessionPolicy.ts';

test('a stale heartbeat cannot pretend the tunnel is still known to be running', () => {
  assert.equal(visibleSession({ requestId: 'one', state: 'tunnel', updatedAt: 1000 }, 2000), 'tunnel');
  assert.equal(visibleSession({ requestId: 'one', state: 'tunnel', updatedAt: 1000 }, 12000), 'unknown');
});
test('a dismissed authorization flow does not leave the UI starting forever', () => {
  assert.equal(visibleSession({ requestId: 'one', state: 'starting', updatedAt: 1000 }, 2000), 'starting');
  assert.equal(visibleSession({ requestId: 'one', state: 'starting', updatedAt: 1000 }, 62000), 'permission_timeout');
});
test('old-session callbacks cannot own new sessions, including empty initial state', () => {
  assert.equal(ownsRequest({ requestId: 'new', state: 'tunnel', updatedAt: 1 }, 'old'), false);
  assert.equal(ownsRequest({ requestId: '', state: 'idle', updatedAt: 0 }, ''), false);
  assert.equal(ownsRequest({ requestId: 'new', state: 'error', updatedAt: 1 }, 'new'), true);
  assert.equal(visibleSession({ requestId: 'new', state: 'corrupt', updatedAt: 1 }, 2), 'error');
});
