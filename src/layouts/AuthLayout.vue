<template>
  <main class="auth-layout">
    <section class="auth-panel">
      <div class="brand">
        <strong>实验室招新平台</strong>
        <PeriodBadge :period="metaStore.period" />
      </div>
      <router-view />
    </section>
  </main>
</template>

<script setup lang="ts">
import PeriodBadge from '@/components/common/PeriodBadge.vue';
import { useMetaStore } from '@/stores/meta';

const metaStore = useMetaStore();
</script>

<style scoped>
.auth-layout {
  display: grid;
  min-height: 100vh;
  min-height: -webkit-fill-available;
  min-height: 100dvh;
  place-items: center;
  padding: max(16px, env(safe-area-inset-top)) max(16px, env(safe-area-inset-right))
    max(16px, env(safe-area-inset-bottom)) max(16px, env(safe-area-inset-left));
  background:
    radial-gradient(circle at 18% 18%, rgba(198, 179, 141, 0.12), transparent 26%),
    radial-gradient(circle at 84% 18%, rgba(165, 155, 212, 0.1), transparent 24%),
    linear-gradient(180deg, #f6f1e8, #f2ebe1 52%, #ebe4d8),
    var(--app-bg);
}

.auth-panel {
  width: min(460px, 100%);
  padding: 30px;
  border: 1px solid rgba(126, 114, 97, 0.12);
  border-radius: 20px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(246, 240, 230, 0.86)),
    var(--app-surface-strong);
  box-shadow:
    14px 16px 32px rgba(145, 128, 106, 0.12),
    -10px -10px 22px rgba(255, 255, 255, 0.86);
}

.brand {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 24px;
}

.brand strong {
  font-size: 20px;
  color: var(--app-text);
}

@media (max-width: 640px) {
  .auth-layout {
    place-items: stretch;
  }

  .auth-panel {
    min-height: calc(100dvh - 32px);
    padding: 22px 18px;
    border-radius: 18px;
  }

  .brand {
    align-items: flex-start;
    flex-direction: column;
    margin-bottom: 18px;
  }

  .brand strong {
    font-size: 18px;
  }
}
</style>
