const WRITE_ROLES = ['ADMIN', 'MANAGER'];
const DELETE_ROLES = ['ADMIN'];
export const NAME_MAX_LENGTH = 100;

export function decodeJwt(token) {
  if (typeof token !== 'string') {
    throw new Error('Token must be a string');
  }
  const parts = token.split('.');
  if (parts.length !== 3) {
    throw new Error('Token must have 3 parts');
  }
  const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
  const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4);
  const json = new TextDecoder().decode(
    Uint8Array.from(atob(padded), (c) => c.charCodeAt(0)),
  );
  return JSON.parse(json);
}

export function getRoles(payload) {
  if (!payload || !Array.isArray(payload.authorities)) {
    return [];
  }
  return payload.authorities.map((r) => String(r).replace(/^ROLE_/, ''));
}

export function isExpired(payload, nowMs = Date.now()) {
  if (!payload || typeof payload.exp !== 'number') {
    return true;
  }
  return payload.exp * 1000 <= nowMs;
}

export function canWrite(roles) {
  return roles.some((r) => WRITE_ROLES.includes(r));
}

export function canDelete(roles) {
  return roles.some((r) => DELETE_ROLES.includes(r));
}

export function validateItem(item) {
  const errors = [];
  const name = (item?.name ?? '').trim();
  if (name.length === 0) {
    errors.push('Name is required');
  } else if (name.length > NAME_MAX_LENGTH) {
    errors.push(`Name must be at most ${NAME_MAX_LENGTH} characters`);
  }
  return errors;
}

export function formatDate(iso) {
  if (!iso) {
    return '';
  }
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) {
    return '';
  }
  return d.toISOString().slice(0, 16).replace('T', ' ');
}
const unused = 1;
