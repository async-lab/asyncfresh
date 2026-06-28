<template>
  <div class="page">
    <PageHeader title="我的分组" description="查看本人所在分组和分组详情入口。" />

    <section class="page-section">
      <el-table v-loading="loading" :data="groups" empty-text="暂无分组">
        <el-table-column prop="name" label="分组名称" min-width="180" />
        <el-table-column label="方向" min-width="160">
          <template #default="{ row }">{{ getDirectionLabel(row) }}</template>
        </el-table-column>
        <el-table-column label="年级" width="100">
          <template #default="{ row }">{{ getGradeLabel(row.grade) }}</template>
        </el-table-column>
        <el-table-column prop="admissionYear" label="入学年份" width="110" />
        <el-table-column prop="maxSize" label="容量" width="90" />
        <el-table-column label="负责人" width="120">
          <template #default="{ row }">
            <span>{{ row.leaderUserId ? `#${row.leaderUserId}` : '暂未任命' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" :icon="View" @click="$router.push(`/app/groups/${row.id}`)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup lang="ts">
import { View } from '@element-plus/icons-vue';
import { computed, onMounted, ref } from 'vue';

import { getMyGroups } from '@/api/groups';
import PageHeader from '@/components/common/PageHeader.vue';
import { useMetaStore } from '@/stores/meta';
import type { Grade, Group } from '@/types/api';
import { gradeLabels } from '@/utils/labels';

const metaStore = useMetaStore();
const loading = ref(false);
const groups = ref<Group[]>([]);
const directionNameMap = computed(() => {
  const map = new Map<number, string>();
  metaStore.directions.forEach((level1) => {
    map.set(level1.id, level1.name);
    level1.children?.forEach((level2) => map.set(level2.id, level2.name));
  });
  return map;
});

onMounted(loadGroups);

async function loadGroups() {
  loading.value = true;
  try {
    groups.value = await getMyGroups();
  } finally {
    loading.value = false;
  }
}

function getDirectionLabel(group: Group) {
  const level1 = directionNameMap.value.get(group.directionLevel1Id);
  const level2 = directionNameMap.value.get(group.directionLevel2Id);
  return [level1, level2].filter(Boolean).join(' / ') || '未知方向';
}

function getGradeLabel(grade?: Grade) {
  return grade ? gradeLabels[grade] : '未知';
}
</script>
