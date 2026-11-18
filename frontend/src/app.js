import {
  canDelete, canWrite, decodeJwt, formatDate, getRoles, isExpired, validateItem,
} from './lib.js';

const API = '/api/v1';
const TOKEN_KEY = 'devopslabka.token';

const $ = (id) => document.getElementById(id);
const loginSection = $('login');
const itemsSection = $('items');
const message = $('message');

function getToken() {
  try {
    return sessionStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

function setToken(token) {
  try {
    if (token) {
      sessionStorage.setItem(TOKEN_KEY, token);
    } else {
      sessionStorage.removeItem(TOKEN_KEY);
    }
  } catch {
    showMessage('Session storage is unavailable', true);
  }
}

function showMessage(text, isError = false) {
  message.textContent = text;
  message.className = isError ? 'message error' : 'message';
}

async function api(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...options.headers };
  const token = getToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  const res = await fetch(`${API}${path}`, { ...options, headers });
  if (res.status === 401) {
    logout();
    throw new Error('Session expired, please log in again');
  }
  if (!res.ok) {
    throw new Error(`Request failed: ${res.status}`);
  }
  return res.status === 204 ? null : res.json();
}

function currentUser() {
  const token = getToken();
  if (!token) {
    return null;
  }
  try {
    const payload = decodeJwt(token);
    return isExpired(payload) ? null : { payload, roles: getRoles(payload) };
  } catch {
    return null;
  }
}

function renderItems(items, roles) {
  const tbody = $('items-body');
  tbody.replaceChildren();
  for (const item of items) {
    const tr = document.createElement('tr');
    for (const value of [item.name, item.description ?? '', item.createdBy ?? '', formatDate(item.createdDate)]) {
      const td = document.createElement('td');
      td.textContent = value;
      tr.append(td);
    }
    const actions = document.createElement('td');
    if (canDelete(roles)) {
      const btn = document.createElement('button');
      btn.textContent = 'Delete';
      btn.className = 'danger';
      btn.addEventListener('click', () => deleteItem(item.id));
      actions.append(btn);
    }
    tr.append(actions);
    tbody.append(tr);
  }
  $('empty').hidden = items.length > 0;
}

async function loadItems() {
  const user = currentUser();
  if (!user) {
    return;
  }
  try {
    renderItems(await api('/items'), user.roles);
  } catch (e) {
    showMessage(e.message, true);
  }
}

async function deleteItem(id) {
  try {
    await api(`/items/${encodeURIComponent(id)}`, { method: 'DELETE' });
    showMessage('Item deleted');
    await loadItems();
  } catch (e) {
    showMessage(e.message, true);
  }
}

function render() {
  const user = currentUser();
  loginSection.hidden = Boolean(user);
  itemsSection.hidden = !user;
  if (user) {
    $('who').textContent = `${user.payload.fullName ?? user.payload.username} (${user.roles.join(', ')})`;
    $('item-form').hidden = !canWrite(user.roles);
    loadItems();
  }
}

function logout() {
  setToken(null);
  render();
}

$('login-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  try {
    const { token } = await api('/auth/authenticate', {
      method: 'POST',
      body: JSON.stringify({ email: $('email').value, password: $('password').value }),
    });
    setToken(token);
    showMessage('');
    render();
  } catch {
    showMessage('Wrong email or password', true);
  }
});

$('item-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const item = { name: $('name').value, description: $('description').value };
  const errors = validateItem(item);
  if (errors.length > 0) {
    showMessage(errors.join('. '), true);
    return;
  }
  try {
    await api('/items', { method: 'POST', body: JSON.stringify(item) });
    event.target.reset();
    showMessage('Item added');
    await loadItems();
  } catch (e) {
    showMessage(e.message, true);
  }
});

$('logout').addEventListener('click', logout);
render();
