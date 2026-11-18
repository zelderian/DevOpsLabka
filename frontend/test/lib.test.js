import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  canDelete, canWrite, decodeJwt, formatDate, getRoles, isExpired, NAME_MAX_LENGTH, validateItem,
} from '../src/lib.js';

function makeToken(payload) {
  const enc = (obj) => Buffer.from(JSON.stringify(obj)).toString('base64url');
  return `${enc({ alg: 'HS384' })}.${enc(payload)}.signature`;
}

test('decodeJwt returns the payload of a token', () => {
  const payload = { username: 'admin', fullName: 'Олена Адмін', authorities: ['ADMIN'], exp: 2000000000 };
  assert.deepEqual(decodeJwt(makeToken(payload)), payload);
});

test('decodeJwt rejects malformed tokens', () => {
  assert.throws(() => decodeJwt('not-a-token'), /3 parts/);
  assert.throws(() => decodeJwt(null), /string/);
});

test('getRoles reads the authorities claim and strips ROLE_ prefix', () => {
  assert.deepEqual(getRoles({ authorities: ['ADMIN'] }), ['ADMIN']);
  assert.deepEqual(getRoles({ authorities: ['ROLE_USER'] }), ['USER']);
  assert.deepEqual(getRoles({}), []);
  assert.deepEqual(getRoles(null), []);
});

test('isExpired compares exp (seconds) with current time', () => {
  const now = 1_700_000_000_000;
  assert.equal(isExpired({ exp: now / 1000 + 60 }, now), false);
  assert.equal(isExpired({ exp: now / 1000 - 60 }, now), true);
  assert.equal(isExpired({}, now), true);
});

test('role permissions match the backend SecurityConfig', () => {
  assert.equal(canWrite(['ADMIN']), true);
  assert.equal(canWrite(['MANAGER']), true);
  assert.equal(canWrite(['USER']), false);
  assert.equal(canDelete(['ADMIN']), true);
  assert.equal(canDelete(['MANAGER']), false);
  assert.equal(canDelete(['USER']), false);
});

test('validateItem requires a non-empty name of limited length', () => {
  assert.deepEqual(validateItem({ name: 'Laptop' }), []);
  assert.deepEqual(validateItem({ name: '   ' }), ['Name is required']);
  assert.deepEqual(validateItem({}), ['Name is required']);
  assert.equal(validateItem({ name: 'x'.repeat(NAME_MAX_LENGTH + 1) }).length, 1);
});

test('formatDate formats ISO dates and ignores invalid ones', () => {
  assert.equal(formatDate('2026-11-11T18:30:45.123Z'), '2026-11-11 18:30');
  assert.equal(formatDate('garbage'), '');
  assert.equal(formatDate(undefined), '');
});
