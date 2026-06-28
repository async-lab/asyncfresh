import { defineStore } from 'pinia';

import { getCurrentPeriod, getDirections } from '@/api/meta';
import type { CurrentPeriod, Direction, PeriodType } from '@/types/api';

interface MetaState {
  currentPeriod: CurrentPeriod | null;
  directions: Direction[];
  loading: boolean;
  initialized: boolean;
}

let bootstrapPromise: Promise<void> | null = null;

export const useMetaStore = defineStore('meta', {
  state: (): MetaState => ({
    currentPeriod: null,
    directions: [],
    loading: false,
    initialized: false
  }),
  getters: {
    period: (state): PeriodType | undefined => state.currentPeriod?.currentPeriod,
    isRegistration: (state) => state.currentPeriod?.currentPeriod === 'REGISTRATION',
    isSelection: (state) => state.currentPeriod?.currentPeriod === 'SELECTION',
    isPeriodActive: (state) => (periods?: PeriodType[]) => {
      if (!periods?.length) {
        return true;
      }

      return Boolean(state.currentPeriod?.currentPeriod && periods.includes(state.currentPeriod.currentPeriod));
    }
  },
  actions: {
    async fetchCurrentPeriod(force = false) {
      if (this.currentPeriod && !force) {
        return this.currentPeriod;
      }

      this.currentPeriod = await getCurrentPeriod();
      return this.currentPeriod;
    },
    async fetchDirections(force = false) {
      if (this.directions.length && !force) {
        return this.directions;
      }

      this.directions = await getDirections(true);
      return this.directions;
    },
    async bootstrap(force = false) {
      if (this.initialized && !force) {
        return;
      }

      if (bootstrapPromise && !force) {
        return bootstrapPromise;
      }

      this.loading = true;

      bootstrapPromise = (async () => {
        await Promise.all([this.fetchCurrentPeriod(), this.fetchDirections()]);
        this.initialized = true;
      })().finally(() => {
        this.loading = false;
        bootstrapPromise = null;
      });

      return bootstrapPromise;
    }
  }
});
