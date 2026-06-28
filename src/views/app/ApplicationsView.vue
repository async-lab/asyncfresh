<template>
  <div class="page">
    <PageHeader title="我的报名" description="报名期可新增、编辑和撤回未分组申请。">
      <template #actions>
        <el-button type="primary" :icon="Plus" :disabled="!metaStore.isRegistration" @click="openCreateDialog">
          新增申请
        </el-button>
      </template>
    </PageHeader>

    <el-alert v-if="!metaStore.isRegistration" type="warning" :closable="false" show-icon>
      当前不是报名期，只能查看历史申请，不能新增、编辑或撤回报名。
    </el-alert>

    <div class="page-section">
      <el-table v-loading="loading" :data="applications" empty-text="暂无报名申请">
        <el-table-column prop="realName" label="姓名" min-width="110" />
        <el-table-column label="意向方向" min-width="180">
          <template #default="{ row }">
            <div class="direction-cell">
              <span>{{ getDirectionLabel(row) }}</span>
              <small class="muted">#{{ row.directionLevel1Id }} / #{{ row.directionLevel2Id }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="college" label="学院" min-width="150" />
        <el-table-column prop="major" label="专业" min-width="150" />
        <el-table-column label="年级" width="90">
          <template #default="{ row }">{{ getGradeLabel(row.grade) }}</template>
        </el-table-column>
        <el-table-column prop="admissionYear" label="入学年份" width="110" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <StatusTag :value="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="更新时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" :icon="View" @click="openDetail(row)">详情</el-button>
            <el-button text :icon="EditPen" :disabled="!canEdit(row)" :title="getEditDisabledReason(row)" @click="openEditDialog(row)">
              编辑
            </el-button>
            <el-button
              text
              type="danger"
              :icon="Delete"
              :loading="withdrawLoadingId === row.id"
              :disabled="!canWithdraw(row)"
              :title="getWithdrawDisabledReason(row)"
              @click="handleWithdraw(row)"
            >
              撤回
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="formDialogVisible"
      :title="editingId ? '编辑报名申请' : '新增报名申请'"
      width="680px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" :disabled="submitting || !metaStore.isRegistration">
        <div class="form-grid">
          <el-form-item label="真实姓名" prop="realName">
            <el-input v-model="form.realName" maxlength="30" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" maxlength="20" />
          </el-form-item>
          <el-form-item label="学院" prop="college">
            <el-input v-model="form.college" maxlength="60" />
          </el-form-item>
          <el-form-item label="专业" prop="major">
            <el-input v-model="form.major" maxlength="60" />
          </el-form-item>
          <el-form-item label="班级" prop="className">
            <el-input v-model="form.className" maxlength="60" />
          </el-form-item>
          <el-form-item label="年级" prop="grade">
            <el-select v-model="form.grade" class="full">
              <el-option v-for="[value, label] in gradeOptions" :key="value" :label="label" :value="value" />
            </el-select>
          </el-form-item>
          <el-form-item label="入学年份" prop="admissionYear">
            <el-input-number v-model="form.admissionYear" class="full" :min="2000" :max="2100" controls-position="right" />
          </el-form-item>
          <el-form-item label="意向方向" prop="directionLevel2Id">
            <DirectionCascader
              v-model="directionPath"
              :disabled="submitting || !metaStore.isRegistration"
              @change="handleDirectionChange"
            />
          </el-form-item>
          <el-form-item class="form-span" label="自我介绍" prop="introduction">
            <el-input
              v-model="form.introduction"
              type="textarea"
              :rows="5"
              maxlength="1000"
              show-word-limit
              placeholder="可以说明已有基础、学习计划或希望加入该方向的原因"
            />
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="!metaStore.isRegistration" @click="handleSubmit">
          保存
        </el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailDrawerVisible" title="报名详情" size="520px" @closed="handleDetailClosed">
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detailApplication">
          <div class="detail-heading">
            <div>
              <h2>{{ detailApplication.realName }}</h2>
              <span class="muted">{{ getDirectionLabel(detailApplication) }}</span>
            </div>
            <StatusTag :value="detailApplication.status" />
          </div>

          <el-descriptions :column="1" border>
            <el-descriptions-item label="手机号">{{ detailApplication.phone }}</el-descriptions-item>
            <el-descriptions-item label="学院">{{ detailApplication.college }}</el-descriptions-item>
            <el-descriptions-item label="专业">{{ detailApplication.major }}</el-descriptions-item>
            <el-descriptions-item label="班级">{{ detailApplication.className }}</el-descriptions-item>
            <el-descriptions-item label="年级">{{ getGradeLabel(detailApplication.grade) }}</el-descriptions-item>
            <el-descriptions-item label="入学年份">{{ detailApplication.admissionYear }}</el-descriptions-item>
            <el-descriptions-item label="申请状态">
              <StatusTag :value="detailApplication.status" />
              <span v-if="detailApplication.statusRemark" class="status-remark">
                {{ detailApplication.statusRemark }}
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="分组结果">
              <span v-if="detailApplication.groupId">#{{ detailApplication.groupId }}</span>
              <span v-else class="muted">暂未分组</span>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatDateTime(detailApplication.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ formatDateTime(detailApplication.updatedAt) }}</el-descriptions-item>
          </el-descriptions>

          <section class="detail-section">
            <h3>自我介绍</h3>
            <p>{{ detailApplication.introduction || '暂无自我介绍。' }}</p>
          </section>

          <div class="drawer-actions">
            <el-button :icon="EditPen" :disabled="!canEdit(detailApplication)" @click="openEditDialog(detailApplication)">
              编辑
            </el-button>
            <el-button
              type="danger"
              :icon="Delete"
              :loading="withdrawLoadingId === detailApplication.id"
              :disabled="!canWithdraw(detailApplication)"
              @click="handleWithdraw(detailApplication)"
            >
              撤回
            </el-button>
          </div>
        </template>
        <el-empty v-else description="报名申请不存在或已不可访问" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { Delete, EditPen, Plus, View } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import {
  createApplication,
  getApplication,
  getApplications,
  updateApplication,
  withdrawApplication
} from '@/api/applications';
import PageHeader from '@/components/common/PageHeader.vue';
import StatusTag from '@/components/common/StatusTag.vue';
import DirectionCascader from '@/components/forms/DirectionCascader.vue';
import { useMetaStore } from '@/stores/meta';
import type { Application, ApplicationForm, Grade } from '@/types/api';
import { gradeLabels } from '@/utils/labels';

const route = useRoute();
const router = useRouter();
const metaStore = useMetaStore();

const applications = ref<Application[]>([]);
const detailApplication = ref<Application | null>(null);
const loading = ref(false);
const detailLoading = ref(false);
const submitting = ref(false);
const formDialogVisible = ref(false);
const detailDrawerVisible = ref(false);
const withdrawLoadingId = ref<number | null>(null);
const editingId = ref<number | null>(null);
const directionPath = ref<number[]>([]);
const formRef = ref<FormInstance>();
const form = reactive<ApplicationForm>(createEmptyForm());

const gradeOptions = Object.entries(gradeLabels) as Array<[Grade, string]>;
const directionNameMap = computed(() => {
  const map = new Map<number, string>();
  metaStore.directions.forEach((level1) => {
    map.set(level1.id, level1.name);
    level1.children?.forEach((level2) => map.set(level2.id, level2.name));
  });
  return map;
});

const rules: FormRules<ApplicationForm> = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (/^1[3-9]\d{9}$/.test(value.trim())) {
          callback();
          return;
        }
        callback(new Error('请输入有效的 11 位手机号'));
      },
      trigger: 'blur'
    }
  ],
  college: [{ required: true, message: '请输入学院', trigger: 'blur' }],
  major: [{ required: true, message: '请输入专业', trigger: 'blur' }],
  className: [{ required: true, message: '请输入班级', trigger: 'blur' }],
  grade: [{ required: true, message: '请选择年级', trigger: 'change' }],
  admissionYear: [{ required: true, type: 'number', message: '请输入入学年份', trigger: 'change' }],
  directionLevel2Id: [
    {
      validator: (_rule, value: number, callback) => {
        if (form.directionLevel1Id && value) {
          callback();
          return;
        }
        callback(new Error('请选择报名方向'));
      },
      trigger: 'change'
    }
  ]
};

onMounted(loadApplications);

watch(
  () => (route.name === 'app-application-detail' ? route.params.id : undefined),
  async (id) => {
    if (!id) {
      detailDrawerVisible.value = false;
      detailApplication.value = null;
      return;
    }

    detailDrawerVisible.value = true;
    await loadDetail(String(id));
  },
  { immediate: true }
);

async function loadApplications() {
  loading.value = true;
  try {
    applications.value = await getApplications();
  } finally {
    loading.value = false;
  }
}

async function loadDetail(id: string | number) {
  detailLoading.value = true;
  try {
    detailApplication.value = await getApplication(id);
  } catch {
    detailApplication.value = null;
  } finally {
    detailLoading.value = false;
  }
}

function openCreateDialog() {
  if (!metaStore.isRegistration) {
    ElMessage.warning('当前不是报名期，暂不能新增报名');
    return;
  }

  editingId.value = null;
  resetForm();
  formDialogVisible.value = true;
}

function openEditDialog(application: Application) {
  if (!canEdit(application)) {
    ElMessage.warning(getEditDisabledReason(application));
    return;
  }

  editingId.value = application.id;
  resetForm(application);
  detailDrawerVisible.value = false;
  formDialogVisible.value = true;
}

function openDetail(application: Application) {
  detailApplication.value = application;
  if (route.name === 'app-application-detail' && String(route.params.id) === String(application.id)) {
    detailDrawerVisible.value = true;
    return;
  }

  void router.push({ name: 'app-application-detail', params: { id: application.id } });
}

async function handleSubmit() {
  if (!metaStore.isRegistration) {
    ElMessage.warning('当前不是报名期，暂不能提交报名申请');
    return;
  }

  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }

  const duplicate = applications.value.find(
    (item) =>
      item.id !== editingId.value &&
      item.directionLevel2Id === form.directionLevel2Id &&
      item.status !== 'WITHDRAWN'
  );

  if (duplicate) {
    ElMessage.warning('已存在相同二级方向的报名申请，请编辑原申请');
    return;
  }

  submitting.value = true;
  try {
    if (editingId.value) {
      await updateApplication(editingId.value, buildPayload());
      ElMessage.success('报名申请已更新');
    } else {
      await createApplication(buildPayload());
      ElMessage.success('报名申请已提交');
    }
    formDialogVisible.value = false;
    await loadApplications();
  } finally {
    submitting.value = false;
  }
}

async function handleWithdraw(application: Application) {
  if (!canWithdraw(application)) {
    ElMessage.warning(getWithdrawDisabledReason(application));
    return;
  }

  const confirmed = await confirmWithdraw(application);
  if (!confirmed) {
    return;
  }

  withdrawLoadingId.value = application.id;
  try {
    await withdrawApplication(application.id);
    ElMessage.success('报名申请已撤回');
    await loadApplications();
    if (detailApplication.value?.id === application.id) {
      await loadDetail(application.id);
    }
  } finally {
    withdrawLoadingId.value = null;
  }
}

async function confirmWithdraw(application: Application) {
  try {
    await ElMessageBox.confirm(
      `确认撤回「${getDirectionLabel(application)}」报名申请？撤回后该申请将只读展示。`,
      '撤回确认',
      {
        confirmButtonText: '确认撤回',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    return true;
  } catch {
    return false;
  }
}

function handleDetailClosed() {
  if (route.name === 'app-application-detail') {
    void router.push({ name: 'app-applications' });
  }
}

function handleDirectionChange(level1Id?: number, level2Id?: number) {
  form.directionLevel1Id = level1Id || 0;
  form.directionLevel2Id = level2Id || 0;
  void formRef.value?.validateField('directionLevel2Id');
}

function resetForm(application?: Application) {
  Object.assign(form, application ? toForm(application) : createEmptyForm());
  directionPath.value = form.directionLevel1Id && form.directionLevel2Id ? [form.directionLevel1Id, form.directionLevel2Id] : [];
  void nextTick(() => formRef.value?.clearValidate());
}

function createEmptyForm(): ApplicationForm {
  return {
    realName: '',
    phone: '',
    college: '',
    major: '',
    className: '',
    grade: 'YEAR_1',
    admissionYear: new Date().getFullYear(),
    directionLevel1Id: 0,
    directionLevel2Id: 0,
    introduction: ''
  };
}

function toForm(application: Application): ApplicationForm {
  return {
    realName: application.realName,
    phone: application.phone,
    college: application.college,
    major: application.major,
    className: application.className,
    grade: application.grade,
    admissionYear: application.admissionYear,
    directionLevel1Id: application.directionLevel1Id,
    directionLevel2Id: application.directionLevel2Id,
    introduction: application.introduction || ''
  };
}

function buildPayload(): ApplicationForm {
  return {
    realName: form.realName.trim(),
    phone: form.phone.trim(),
    college: form.college.trim(),
    major: form.major.trim(),
    className: form.className.trim(),
    grade: form.grade,
    admissionYear: form.admissionYear,
    directionLevel1Id: form.directionLevel1Id,
    directionLevel2Id: form.directionLevel2Id,
    introduction: form.introduction?.trim() || undefined
  };
}

function canEdit(application?: Application | null) {
  return Boolean(
    application &&
      metaStore.isRegistration &&
      (application.status === 'SUBMITTED' || application.status === 'REJECTED')
  );
}

function canWithdraw(application?: Application | null) {
  return Boolean(application && metaStore.isRegistration && application.status === 'SUBMITTED');
}

function getEditDisabledReason(application: Application) {
  if (!metaStore.isRegistration) return '当前不是报名期，不能编辑报名申请';
  if (application.status === 'GROUPED') return '已分组申请不能编辑';
  if (application.status === 'WITHDRAWN') return '已撤回申请只读展示';
  return '当前状态不能编辑';
}

function getWithdrawDisabledReason(application: Application) {
  if (!metaStore.isRegistration) return '当前不是报名期，不能撤回报名申请';
  if (application.status === 'GROUPED') return '已分组申请不能撤回';
  if (application.status === 'WITHDRAWN') return '该申请已撤回';
  if (application.status === 'REJECTED') return '已驳回申请不能撤回';
  return '当前状态不能撤回';
}

function getDirectionLabel(application: Application) {
  const level1 = directionNameMap.value.get(application.directionLevel1Id);
  const level2 = directionNameMap.value.get(application.directionLevel2Id);
  return [level1, level2].filter(Boolean).join(' / ') || '未知方向';
}

function getGradeLabel(grade?: Grade) {
  return grade ? gradeLabels[grade] : '未知';
}

function formatDateTime(value?: string) {
  if (!value) return '-';

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date);
}
</script>

<style scoped>
.direction-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.direction-cell small {
  font-size: 12px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.form-span {
  grid-column: 1 / -1;
}

.full,
.form-grid :deep(.el-cascader) {
  width: 100%;
}

.detail-body {
  min-height: 240px;
}

.detail-heading {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 18px;
}

.detail-heading h2 {
  margin: 0 0 6px;
  font-size: 20px;
}

.status-remark {
  margin-left: 8px;
  color: var(--app-muted);
}

.detail-section {
  margin-top: 18px;
}

.detail-section h3 {
  margin: 0 0 8px;
  font-size: 16px;
}

.detail-section p {
  margin: 0;
  color: var(--app-text);
  line-height: 1.7;
  white-space: pre-wrap;
}

.drawer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

@media (max-width: 720px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
