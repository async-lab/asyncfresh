<template>
  <div class="page">
    <PageHeader title="方向管理" description="维护两级学习方向、启用状态和排序。">
      <template #actions>
        <el-button :icon="Plus" type="primary" @click="openCreateDialog()">新增一级方向</el-button>
      </template>
    </PageHeader>

    <section class="page-section">
      <el-table
        v-loading="loading"
        :data="directions"
        row-key="id"
        default-expand-all
        :tree-props="{ children: 'children' }"
        empty-text="暂无方向"
      >
        <el-table-column prop="name" label="方向名称" min-width="180" />
        <el-table-column label="层级" width="90">
          <template #default="{ row }">{{ row.level === 1 ? '一级' : '二级' }}</template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled === false ? 'info' : 'success'" effect="light">
              {{ row.enabled === false ? '停用' : '启用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.level === 1" text type="primary" :icon="Plus" @click="openCreateDialog(row.id)">
              新增子方向
            </el-button>
            <el-button text :icon="EditPen" @click="openEditDialog(row)">编辑</el-button>
            <ConfirmAction title="确认删除该方向？" @confirm="handleDelete(row)">
              <el-button text type="danger" :icon="Delete">删除</el-button>
            </ConfirmAction>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑方向' : '新增方向'" width="520px" :close-on-click-modal="false">
      <el-form label-position="top" :model="form">
        <el-form-item label="父级方向">
          <el-select v-model="form.parentId" class="full" clearable :disabled="Boolean(editingId)">
            <el-option label="无，作为一级方向" :value="null" />
            <el-option v-for="item in directions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="方向名称">
          <el-input v-model="form.name" maxlength="40" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" class="full" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveDirection">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { Delete, EditPen, Plus } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';

import {
  createDirection,
  deleteDirection,
  getAdminDirections,
  updateDirection,
  type DirectionPayload
} from '@/api/admin';
import ConfirmAction from '@/components/common/ConfirmAction.vue';
import PageHeader from '@/components/common/PageHeader.vue';
import type { Direction } from '@/types/api';

const directions = ref<Direction[]>([]);
const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const editingId = ref<number | null>(null);
const form = reactive<DirectionPayload>(createEmptyForm());

onMounted(loadDirections);

async function loadDirections() {
  loading.value = true;
  try {
    directions.value = await getAdminDirections();
  } finally {
    loading.value = false;
  }
}

function openCreateDialog(parentId?: number) {
  editingId.value = null;
  Object.assign(form, createEmptyForm(parentId));
  dialogVisible.value = true;
}

function openEditDialog(direction: Direction) {
  editingId.value = direction.id;
  Object.assign(form, {
    parentId: direction.parentId ?? null,
    name: direction.name,
    sortOrder: direction.sortOrder || 0,
    enabled: direction.enabled !== false
  });
  dialogVisible.value = true;
}

async function saveDirection() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入方向名称');
    return;
  }

  saving.value = true;
  try {
    const payload = { ...form, name: form.name.trim() };
    if (editingId.value) {
      await updateDirection(editingId.value, payload);
      ElMessage.success('方向已更新');
    } else {
      await createDirection(payload);
      ElMessage.success('方向已创建');
    }
    dialogVisible.value = false;
    await loadDirections();
  } finally {
    saving.value = false;
  }
}

async function handleDelete(direction: Direction) {
  if (direction.children?.length) {
    ElMessage.warning('请先删除下级方向');
    return;
  }

  await deleteDirection(direction.id);
  ElMessage.success('方向已删除');
  await loadDirections();
}

function createEmptyForm(parentId?: number): DirectionPayload {
  return {
    parentId: parentId ?? null,
    name: '',
    sortOrder: 0,
    enabled: true
  };
}
</script>

<style scoped>
.full {
  width: 100%;
}
</style>
