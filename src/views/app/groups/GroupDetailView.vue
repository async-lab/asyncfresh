<template>
  <div class="page">
    <PageHeader :title="group?.name || '分组详情'" description="查看分组方向、容量、负责人和成员。">
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
            <el-descriptions-item label="容量">{{ members.length }} / {{ group.maxSize }}</el-descriptions-item>
            <el-descriptions-item label="负责人">{{ leaderName }}</el-descriptions-item>
          </el-descriptions>
        </template>
        <el-empty v-else-if="!loading" description="分组不存在或已不可访问" />
      </section>

      <section class="page-section">
        <h2>成员概览</h2>
        <div class="metric-grid compact">
          <div class="metric-card">
            <span class="muted">成员数</span>
            <strong>{{ members.length }}</strong>
          </div>
          <div class="metric-card">
            <span class="muted">容量</span>
            <strong>{{ group?.maxSize || 0 }}</strong>
          </div>
        </div>
      </section>
    </div>

    <section class="page-section">
      <div class="page-toolbar">
        <h2>分组成员</h2>
        <el-button :icon="Refresh" :loading="loading" @click="loadDetail">刷新</el-button>
      </div>
      <el-table :data="members" empty-text="暂无成员">
        <el-table-column prop="realName" label="姓名" min-width="120" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column label="方向" min-width="160">
          <template #default="{ row }">{{ row.directionLevel1Name }} / {{ row.directionLevel2Name }}</template>
        </el-table-column>
        <el-table-column label="年级" width="100">
          <template #default="{ row }">{{ getGradeLabel(row.grade) }}</template>
        </el-table-column>
        <el-table-column prop="admissionYear" label="入学年份" width="110" />
        <el-table-column label="报名状态" width="120">
          <template #default="{ row }">
            <StatusTag :value="row.applicationStatus" />
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { Back, Refresh } from '@element-plus/icons-vue';
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';

import { getGroup, getGroupMembers } from '@/api/groups';
import PageHeader from '@/components/common/PageHeader.vue';
import StatusTag from '@/components/common/StatusTag.vue';
import { useMetaStore } from '@/stores/meta';
import type { Grade, Group, GroupMember } from '@/types/api';
import { gradeLabels } from '@/utils/labels';

const route = useRoute();
const metaStore = useMetaStore();
const loading = ref(false);
const group = ref<Group | null>(null);
const members = ref<GroupMember[]>([]);
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
  const leader = members.value.find((member) => member.userId === group.value?.leaderUserId);
  return leader ? `${leader.realName}（${leader.username}）` : `#${group.value.leaderUserId}`;
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
    const [groupData, memberData] = await Promise.all([getGroup(id), getGroupMembers(id)]);
    group.value = groupData;
    members.value = memberData;
  } catch {
    group.value = null;
    members.value = [];
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

@media (max-width: 860px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
