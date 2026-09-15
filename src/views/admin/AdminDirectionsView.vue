<template>
  <div class="page">
    <PageHeader title="方向管理" description="维护两级学习方向、启用状态和排序。">
      <template #actions>
        <el-button :icon="Plus" type="primary" @click="openCreateDialog()">新增一级方向</el-button>
      </template>
    </PageHeader>

    <section class="page-section">
      <MobileList
        v-if="isMobile"
        :data="directions"
        :loading="loading"
        empty-text="暂无方向"
        key-field="id"
      >
        <template #item="{ item }">
          <div class="direction-card">
            <div class="direction-card__head">
              <span class="direction-card__name">{{ item.name }}</span>
              <el-tag :type="item.enabled === false ? 'info' : 'success'" effect="light" size="small">
                {{ item.enabled === false ? '停用' : '启用' }}
              </el-tag>
            </div>
            <div class="direction-card__actions">
              <el-button size="small" text type="primary" :icon="Plus" @click="openCreateDialog(item.id)">
                子方向
              </el-button>
              <el-button size="small" text :icon="EditPen" @click="openEditDialog(item)">编辑</el-button>
              <ConfirmAction title="确认删除该方向？" @confirm="handleDelete(item)">
                <el-button size="small" text type="danger" :icon="Delete">删除</el-button>
              </ConfirmAction>
            </div>

            <div v-if="item.children?.length" class="direction-card__children">
              <div v-for="child in item.children" :key="child.id" class="direction-child">
                <div class="direction-child__info">
                  <span class="direction-child__name">{{ child.name }}</span>
                  <span class="direction-child__meta">排序 {{ child.sortOrder ?? 0 }}</span>
                </div>
                <el-tag :type="child.enabled === false ? 'info' : 'success'" effect="light" size="small">
                  {{ child.enabled === false ? '停用' : '启用' }}
                </el-tag>
                <div class="direction-child__actions">
                  <el-button text type="primary" :icon="EditPen" title="编辑" @click="openEditDialog(child)" />
                  <ConfirmAction title="确认删除该方向？" @confirm="handleDelete(child)">
                    <el-button text type="danger" :icon="Delete" title="删除" />
                  </ConfirmAction>
                </div>
              </div>
            </div>
          </div>
        </template>
      </MobileList>

      <el-table
        v-else
        v-loading="loading"
        :data="directions"
        row-key="id"
        default-expand-all
        :tree-props="{ children: 'children' }"
        empty-text="暂无方向"
      >
        <el-table-column prop="name" label="方向名称" min-width="300" />
        <el-table-column label="层级" min-width="140">
          <template #default="{ row }">{{ row.level === 1 ? '一级' : '二级' }}</template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" min-width="130" />
        <el-table-column label="状态" min-width="150">
          <template #default="{ row }">
            <el-tag :type="row.enabled === false ? 'info' : 'success'" effect="light">
              {{ row.enabled === false ? '停用' : '启用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="380" fixed="right" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button v-if="row.level === 1" text type="primary" :icon="Plus" @click="openCreateDialog(row.id)">
                新增子方向
              </el-button>
              <el-button text :icon="EditPen" @click="openEditDialog(row)">编辑</el-button>
              <ConfirmAction title="确认删除该方向？" @confirm="handleDelete(row)">
                <el-button text type="danger" :icon="Delete">删除</el-button>
              </ConfirmAction>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑方向' : '新增方向'" :width="dialogWidth" :close-on-click-modal="false">
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
import MobileList from '@/components/common/MobileList.vue';
import PageHeader from '@/components/common/PageHeader.vue';
import type { Direction } from '@/types/api';
import { useOverlayLayout } from '@/composables/useMediaQuery';

const { isMobile, dialogWidth } = useOverlayLayout({ dialogWidth: '520px' });
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

.direction-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.direction-card__head {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
  min-width: 0;
}

.direction-card__name {
  min-width: 0;
  overflow-wrap: anywhere;
  font-size: 16px;
  font-weight: 600;
  color: var(--app-text);
}

.direction-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 8px;
  margin-top: 2px;
}

.direction-card__actions :deep(.el-button) {
  margin-left: 0 !important;
  min-height: 30px;
}

.direction-card__children {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 4px;
  padding-left: 12px;
  border-left: 3px solid rgba(165, 155, 212, 0.35);
}

.direction-child {
  display: flex;
  gap: 8px;
  align-items: center;
  min-width: 0;
}

.direction-child__info {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.direction-child__name {
  min-width: 0;
  overflow-wrap: anywhere;
  font-weight: 500;
  color: var(--app-text);
}

.direction-child__meta {
  color: var(--app-muted);
  font-size: 12px;
}

.direction-child__actions {
  display: flex;
  flex-shrink: 0;
  gap: 2px;
}

.direction-child__actions :deep(.el-button) {
  margin-left: 0 !important;
}
</style>
