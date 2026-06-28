import type { LoginPayload } from '@/api/auth';
import type { User } from '@/types/api';

const MOCK_SESSION_KEY = 'lab_recruit_mock_user';
const MOCK_REGISTERED_ACCOUNTS_KEY = 'lab_recruit_mock_registered_accounts';
const MOCK_PASSWORD_OVERRIDES_KEY = 'lab_recruit_mock_password_overrides';

export interface MockAccount {
  label: string;
  email: string;
  password: string;
  user: User;
}

export const mockAccounts: MockAccount[] = [
  {
    label: '新生',
    email: 'student@example.com',
    password: 'Pass1234',
    user: {
      id: 1,
      username: 'student',
      email: 'student@example.com',
      role: 'FRESHMAN',
      status: 'ACTIVE',
      emailVerified: true,
      groups: [{ id: 10, name: '后端-Java-1组' }]
    }
  },
  {
    label: '负责人',
    email: 'leader@example.com',
    password: 'Pass1234',
    user: {
      id: 2,
      username: 'leader',
      email: 'leader@example.com',
      role: 'LEADER',
      status: 'ACTIVE',
      emailVerified: true,
      leaderGroupId: 10,
      groups: [{ id: 10, name: '后端-Java-1组' }]
    }
  },
  {
    label: '管理员',
    email: 'admin@example.com',
    password: 'Admin1234',
    user: {
      id: 3,
      username: 'admin',
      email: 'admin@example.com',
      role: 'ADMIN',
      status: 'ACTIVE',
      emailVerified: true,
      groups: []
    }
  }
];

export function findMockUser(payload: LoginPayload): User | null {
  const email = normalizeMockEmail(payload.email);
  const account = getAllMockAccounts().find((item) => item.email === email);

  if (!account) {
    return null;
  }

  const password = getMockPassword(email) || account.password;
  return password === payload.password ? cloneUser(account.user) : null;
}

export function hasMockAccount(email: string) {
  const normalizedEmail = normalizeMockEmail(email);
  return getAllMockAccounts().some((item) => item.email === normalizedEmail);
}

export function registerMockAccount(user: User, password: string) {
  const normalizedEmail = normalizeMockEmail(user.email);
  const account: MockAccount = {
    label: user.username,
    email: normalizedEmail,
    password,
    user: cloneUser({ ...user, email: normalizedEmail })
  };

  const accounts = loadRegisteredAccounts().filter((item) => item.email !== normalizedEmail);
  accounts.push(account);
  localStorage.setItem(MOCK_REGISTERED_ACCOUNTS_KEY, JSON.stringify(accounts));
  saveMockPassword(normalizedEmail, password);
}

export function updateMockPassword(email: string, password: string) {
  const normalizedEmail = normalizeMockEmail(email);
  const accounts = loadRegisteredAccounts();
  const account = accounts.find((item) => item.email === normalizedEmail);

  if (account) {
    account.password = password;
    localStorage.setItem(MOCK_REGISTERED_ACCOUNTS_KEY, JSON.stringify(accounts));
  }

  saveMockPassword(normalizedEmail, password);
}

export function verifyMockPassword(email: string, password: string) {
  const normalizedEmail = normalizeMockEmail(email);
  const account = getAllMockAccounts().find((item) => item.email === normalizedEmail);

  if (!account) {
    return false;
  }

  return (getMockPassword(normalizedEmail) || account.password) === password;
}

export function saveMockUser(user: User) {
  localStorage.setItem(MOCK_SESSION_KEY, JSON.stringify(cloneUser(user)));
}

export function loadMockUser(): User | null {
  return readJson<User>(MOCK_SESSION_KEY);
}

export function clearMockUser() {
  localStorage.removeItem(MOCK_SESSION_KEY);
}

export function getAllMockAccounts() {
  return [...mockAccounts, ...loadRegisteredAccounts()].map((item) => ({
    ...item,
    email: normalizeMockEmail(item.email),
    user: cloneUser(item.user)
  }));
}

function loadRegisteredAccounts() {
  return readJson<MockAccount[]>(MOCK_REGISTERED_ACCOUNTS_KEY) || [];
}

function getMockPassword(email: string) {
  const passwords = readJson<Record<string, string>>(MOCK_PASSWORD_OVERRIDES_KEY) || {};
  return passwords[normalizeMockEmail(email)];
}

function saveMockPassword(email: string, password: string) {
  const passwords = readJson<Record<string, string>>(MOCK_PASSWORD_OVERRIDES_KEY) || {};
  passwords[normalizeMockEmail(email)] = password;
  localStorage.setItem(MOCK_PASSWORD_OVERRIDES_KEY, JSON.stringify(passwords));
}

function readJson<T>(key: string): T | null {
  const raw = localStorage.getItem(key);

  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as T;
  } catch {
    localStorage.removeItem(key);
    return null;
  }
}

function normalizeMockEmail(email: string) {
  return email.trim().toLowerCase();
}

function cloneUser(user: User): User {
  return {
    ...user,
    groups: user.groups?.map((group) => ({ ...group }))
  };
}