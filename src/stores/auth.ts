import { defineStore } from 'pinia';

import {
  changePassword as changePasswordApi,
  getMe,
  login as loginApi,
  logout as logoutApi
} from '@/api/auth';
import type { ChangePasswordPayload, LoginPayload } from '@/api/auth';
import type { Role, User } from '@/types/api';
import { clearMockUser, findMockUser, loadMockUser, saveMockUser } from '@/utils/mockAuth';

const enableMockAuth = import.meta.env.VITE_ENABLE_MOCK_AUTH === 'true';
const useMockApi = import.meta.env.VITE_USE_MOCK_API === 'true';

interface AuthState {
  user: User | null;
  loading: boolean;
  initialized: boolean;
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    user: null,
    loading: false,
    initialized: false
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.user),
    role: (state) => state.user?.role,
    isAdmin: (state) => state.user?.role === 'ADMIN',
    isLeader: (state) => state.user?.role === 'LEADER'
  },
  actions: {
    async login(payload: LoginPayload) {
      this.loading = true;
      try {
        const normalizedPayload = {
          ...payload,
          email: payload.email.trim().toLowerCase(),
          rememberMe: Boolean(payload.rememberMe)
        };
        const mockUser = enableMockAuth ? findMockUser(normalizedPayload) : null;
        if (enableMockAuth && mockUser) {
          this.user = mockUser;
          saveMockUser(mockUser);
          this.initialized = true;
          return this.user;
        }

        this.user = await loginApi(normalizedPayload);
        if (!useMockApi) {
          clearMockUser();
        }
        this.initialized = true;
        return this.user;
      } finally {
        this.loading = false;
      }
    },
    async fetchMe() {
      this.loading = true;
      try {
        const mockUser = enableMockAuth ? loadMockUser() : null;
        if (enableMockAuth && mockUser) {
          this.user = mockUser;
          return this.user;
        }

        this.user = await getMe();
        return this.user;
      } finally {
        this.loading = false;
        this.initialized = true;
      }
    },
    async logout() {
      const mockUser = enableMockAuth ? loadMockUser() : null;

      if (enableMockAuth && mockUser) {
        clearMockUser();
        this.user = null;
        this.initialized = true;
        return;
      }

      try {
        await logoutApi();
      } finally {
        this.user = null;
        this.initialized = true;
      }
    },
    async changePassword(payload: ChangePasswordPayload) {
      this.loading = true;
      try {
        const mockUser = enableMockAuth ? loadMockUser() : null;
        if (enableMockAuth && mockUser) {
          clearMockUser();
          this.user = null;
          this.initialized = true;
          return;
        }

        await changePasswordApi(payload);
        clearMockUser();
        this.user = null;
        this.initialized = true;
      } finally {
        this.loading = false;
      }
    },
    clear() {
      clearMockUser();
      this.user = null;
      this.initialized = true;
    },
    hasRole(roles?: Role[]) {
      if (!roles?.length) {
        return true;
      }

      return Boolean(this.user?.role && roles.includes(this.user.role));
    }
  }
});
