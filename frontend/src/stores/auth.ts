import { defineStore } from 'pinia';

import {
  changePassword as changePasswordApi,
  getMe,
  login as loginApi,
  logout as logoutApi
} from '@/api/auth';
import type { ChangePasswordPayload, LoginPayload } from '@/api/auth';
import type { Role, User } from '@/types/api';

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
        this.user = await loginApi(normalizedPayload);
        this.initialized = true;
        return this.user;
      } finally {
        this.loading = false;
      }
    },
    async fetchMe() {
      this.loading = true;
      try {
        this.user = await getMe();
        return this.user;
      } finally {
        this.loading = false;
        this.initialized = true;
      }
    },
    async logout() {
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
        await changePasswordApi(payload);
        this.user = null;
        this.initialized = true;
      } finally {
        this.loading = false;
      }
    },
    clear() {
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
