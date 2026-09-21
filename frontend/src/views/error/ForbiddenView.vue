<template>
  <main class="error-page">
    <h1>403</h1>
    <p>你没有权限访问该页面。</p>
    <el-button type="primary" @click="$router.push('/app')">返回首页</el-button>
  </main>
</template>

<style scoped>
.error-page {
  display: grid;
  min-height: 100vh;
  place-items: center;
  align-content: center;
  gap: 12px;
  background: var(--app-bg);
}

h1 {
  margin: 0;
  font-size: 48px;
}

p {
  margin: 0 0 8px;
  color: var(--app-muted);
}
</style>
