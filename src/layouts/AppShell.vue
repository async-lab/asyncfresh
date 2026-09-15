<template>
  <el-container class="shell" :class="{ 'is-mobile': isMobile }">
    <el-drawer
      v-if="isMobile"
      v-model="navOpen"
      class="nav-drawer"
      direction="ltr"
      size="min(88vw, 320px)"
      :with-header="false"
      append-to-body
    >
      <div class="side drawer-side">
        <div class="side-title">{{ title }}</div>
        <div class="side-menu">
          <slot name="side" />
        </div>
      </div>
    </el-drawer>

    <el-aside v-else width="248px" class="side">
      <div class="side-title">{{ title }}</div>
      <div class="side-menu">
        <slot name="side" />
      </div>
    </el-aside>

    <el-container class="workspace">
      <el-header class="topbar">
        <div class="topbar-left">
          <button
            v-if="isMobile"
            class="nav-toggle"
            type="button"
            aria-label="打开导航菜单"
            @click="navOpen = true"
          >
            <el-icon :size="20"><Menu /></el-icon>
          </button>
          <slot name="brand" />
        </div>
        <div class="topbar-right">
          <NotificationBell />
          <slot name="actions" />
        </div>
      </el-header>
      <el-main class="main">
        <slot />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { Menu } from '@element-plus/icons-vue';
import { ref, watch } from 'vue';
import { useRoute } from 'vue-router';

import { NotificationBell } from '@/components/common';
import { useIsMobile } from '@/composables/useMediaQuery';

defineProps<{
  title: string;
}>();

const route = useRoute();
const isMobile = useIsMobile();
const navOpen = ref(false);

watch(
  () => route.fullPath,
  () => {
    navOpen.value = false;
  }
);

watch(isMobile, (mobile) => {
  if (!mobile) {
    navOpen.value = false;
  }
});
</script>

<style scoped>
.shell {
  min-height: 100vh;
  min-height: -webkit-fill-available;
  min-height: 100dvh;
  padding: max(12px, env(safe-area-inset-top)) max(18px, env(safe-area-inset-right))
    max(12px, env(safe-area-inset-bottom)) max(18px, env(safe-area-inset-left));
  gap: 18px;
}

.side {
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  height: auto;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 20px;
  background: linear-gradient(180deg, #2f2b27 0%, #26221f 100%);
  box-shadow: 14px 16px 32px rgba(145, 128, 106, 0.14);
  color: #f5f1ea;
}

.drawer-side {
  height: 100%;
  border-radius: 0;
  box-shadow: none;
}

.side-title {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  height: 56px;
  padding: 0 20px;
  font-weight: 700;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  color: #fffaf4;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.06), transparent);
}

.side-menu {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 8px 0 16px;
  -webkit-overflow-scrolling: touch;
}

.workspace {
  flex: 1;
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
  border: 1px solid rgba(126, 114, 97, 0.12);
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(247, 242, 234, 0.94));
  box-shadow: var(--app-shadow-raised);
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  height: auto !important;
  min-height: 64px;
  padding: 10px 20px;
  border-bottom: 1px solid rgba(126, 114, 97, 0.1);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.82), rgba(247, 242, 234, 0.72));
}

.topbar-left,
.topbar-right {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
}

.topbar-right {
  justify-content: flex-end;
}

.nav-toggle {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  padding: 0;
  border: 1px solid rgba(126, 114, 97, 0.14);
  border-radius: 14px;
  background: linear-gradient(180deg, #fbf8f3, #f2ece2);
  color: var(--app-text);
  box-shadow: var(--app-shadow-press);
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.nav-toggle:focus-visible {
  outline: 2px solid rgba(165, 155, 212, 0.7);
  outline-offset: 2px;
}

.main {
  min-width: 0;
  max-width: 100%;
  overflow-x: hidden;
  padding: 24px;
  background:
    radial-gradient(circle at 90% 0%, rgba(198, 179, 141, 0.05), transparent 24%),
    radial-gradient(circle at 8% 14%, rgba(165, 155, 212, 0.06), transparent 22%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.66), rgba(249, 244, 235, 0.94));
}

.shell.is-mobile {
  padding: 0;
  gap: 0;
}

.shell.is-mobile .workspace {
  min-height: 100vh;
  min-height: 100dvh;
  border: 0;
  border-radius: 0;
  box-shadow: none;
}

.shell.is-mobile .topbar {
  position: sticky;
  top: 0;
  z-index: 20;
  min-height: 56px;
  padding: max(8px, env(safe-area-inset-top)) 12px 8px;
}

.shell.is-mobile .main {
  padding: 16px 12px calc(20px + env(safe-area-inset-bottom));
}

@media (max-width: 960px) {
  .topbar {
    padding-inline: 12px;
  }

  .main {
    padding: 16px 12px;
  }
}
</style>

<style>
.nav-drawer.el-drawer,
.el-drawer.nav-drawer {
  width: min(88vw, 320px) !important;
  background: #26221f;
  padding-top: env(safe-area-inset-top);
  padding-bottom: env(safe-area-inset-bottom);
}

.nav-drawer .el-drawer__body {
  height: 100%;
  padding: 0 !important;
  background: #26221f;
}

.side .el-menu {
  --el-menu-bg-color: transparent;
  --el-menu-text-color: #d7d2cb;
  --el-menu-hover-bg-color: transparent;
  --el-menu-active-color: #ffffff;
  border-right: 0;
  background: transparent;
}

.side .el-menu-item,
.side .el-sub-menu__title {
  margin: 4px 10px;
  border-radius: 12px;
}

.side .el-sub-menu__title {
  height: 46px;
  color: #f5f1ea;
  background: rgba(255, 255, 255, 0.03);
}

.side .el-sub-menu__title:hover {
  background: rgba(165, 155, 212, 0.12);
}

.side .el-sub-menu__icon-arrow {
  color: rgba(245, 241, 234, 0.72);
}

.side .menu-group-title {
  display: flex;
  gap: 10px;
  align-items: center;
  font-weight: 700;
}

.side .menu-item {
  padding-left: 36px !important;
}

.side .el-menu-item.is-active {
  background: rgba(165, 155, 212, 0.22);
  box-shadow:
    inset 1px 1px 0 rgba(255, 255, 255, 0.1),
    inset -1px -1px 0 rgba(0, 0, 0, 0.22);
}

.side .el-menu-item:not(.is-active):hover {
  background: rgba(165, 155, 212, 0.12);
}

.side .el-menu-item .el-icon,
.side .el-sub-menu__title .el-icon {
  color: inherit;
}
</style>
