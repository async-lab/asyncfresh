<template>
  <div class="page-header">
    <div class="page-header__content">
      <h1>{{ title }}</h1>
      <p v-if="description">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="page-header__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string;
  description?: string;
}>();
</script>

<style scoped>
.page-header {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  justify-content: space-between;
  flex-wrap: wrap;
}

.page-header__content {
  flex: 1 1 320px;
  min-width: 0;
}

.page-header__actions {
  display: flex;
  flex: 0 1 auto;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  justify-content: flex-end;
  min-width: 0;
}

h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
}

p {
  margin: 8px 0 0;
  color: var(--app-muted);
}

@media (max-width: 960px) {
  .page-header__actions {
    width: 100%;
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .page-header {
    gap: 12px;
  }

  h1 {
    font-size: 22px;
  }
}
</style>
