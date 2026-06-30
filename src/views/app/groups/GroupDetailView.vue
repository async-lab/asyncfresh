<template>
  <div class="page">
    <PageHeader :title="group?.name || '分组详情'" description="查看分组方向、容量和负责人。">
      <template #actions>
        <el-button :icon="Back" @click="$router.push('/app/groups')">返回分组</el-button>
      </template>
    </PageHeader>

    <div class="detail-grid">
      <section class="page-section" v-loading="loading">
        <template v-if="group">
          <h2>分组信息</h2>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="分组名称">{{ group.name }}</el-descriptions-item>
            <el-descriptions-item label="方向">{{ directionLabel }}</el-descriptions-item>
            <el-descriptions-item label="年级">{{ getGradeLabel(group.grade) }}</el-descriptions-item>
            <el-descriptions-item label="入学年份">{{ group.admissionYear }}</el-descriptions-item>
            <el-descriptions-item label="容量">{{ group.maxSize }}</el-descriptions-item>
            <el-descriptions-item label="负责人">{{ leaderName }}</el-descriptions-item>
          </el-descriptions>
        </template>
        <el-empty v-else-if="!loading" description="分组不存在或已不可访问" />
      </section>

      <section class="page-section">
        <h2>成员概览</h2>
        <div class="metric-grid compact">
          <div class="metric-card">
            <span class="muted">当前账号</span>
            <strong>已加入</strong>
          </div>
          <div class="metric-card">
            <span class="muted">容量</span>
            <strong>{{ group?.maxSize || 0 }}</strong>
          </div>
        </div>
        <p class="muted note">组成员列表仅负责人和管理员可查看。</p>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Back } from '@element-plus/icons-vue';
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';

import { getGroup } from '@/api/groups';
import PageHeader from '@/components/common/PageHeader.vue';
import { useMetaStore } from '@/stores/meta';
import type { Grade, Group } from '@/types/api';
import { gradeLabels } from '@/utils/labels';

const route = useRoute();
const metaStore = useMetaStore();
const loading = ref(false);
const group = ref<Group | null>(null);
const directionNameMap = computed(() => {
  const map = new Map<number, string>();
  metaStore.directions.forEach((level1) => {
    map.set(level1.id, level1.name);
    level1.children?.forEach((level2) => map.set(level2.id, level2.name));
  });
  return map;
});
const directionLabel = computed(() => {
  if (!group.value) return '未知方向';
  const level1 = directionNameMap.value.get(group.value.directionLevel1Id);
  const level2 = directionNameMap.value.get(group.value.directionLevel2Id);
  return [level1, level2].filter(Boolean).join(' / ') || '未知方向';
});
const leaderName = computed(() => {
  if (!group.value?.leaderUserId) return '暂未任命';
  return `#${group.value.leaderUserId}`;
});

onMounted(loadDetail);

watch(
  () => route.params.id,
  () => {
    void loadDetail();
  }
);

async function loadDetail() {
  loading.value = true;
  try {
    const id = String(route.params.id);
    group.value = await getGroup(id);
  } catch {
    group.value = null;
  } finally {
    loading.value = false;
  }
}

function getGradeLabel(grade?: Grade) {
  return grade ? gradeLabels[grade] : '未知';
}
</script>

<style scoped>
.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 360px);
  gap: 16px;
}

h2 {
  margin: 0 0 14px;
  font-size: 18px;
}

.compact {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.note {
  margin: 12px 0 0;
  line-height: 1.6;
}

@media (max-width: 860px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
