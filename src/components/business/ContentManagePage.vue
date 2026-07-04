<template>
  <div class="page">
    <PageHeader :title="title" :description="description" />

    <section class="page-section">
      <div class="page-toolbar content-toolbar">
        <div class="toolbar-left">
          <el-button type="primary" :icon="Plus" @click="openCreate">新建{{ itemName }}</el-button>
          <el-select v-if="kind !== 'announcements'" v-model="query.groupId" class="group-select" placeholder="选择责任包" clearable>
            <el-option v-for="group in groups" :key="group.id" :label="group.name" :value="group.id" />
          </el-select>
          <el-select v-if="kind === 'announcements' && mode === 'admin'" v-model="query.scope" class="scope-select">
            <el-option label="全部范围" value="" />
            <el-option label="全局" value="GLOBAL" />
            <el-option label="组内" value="GROUP" />
          </el-select>
          <el-input v-model="query.keyword" class="keyword-input" clearable :placeholder="`搜索${itemName}标题或内容`" @keyup.enter="loadItems" />
        </div>
        <el-button :icon="Refresh" :loading="loading" @click="loadItems">刷新</el-button>
      </div>

      <PageTable :data="pagedItems" :loading="loading" empty-text="暂无内容">
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column v-if="kind === 'announcements'" label="范围" width="96">
          <template #default="{ row }">
            <el-tag effect="plain">{{ scopeLabels[row.scope as Scope] || '组内' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-else label="责任包" min-width="150">
          <template #default="{ row }">{{ getGroupName(row.groupId) }}</template>
        </el-table-column>
        <el-table-column v-if="kind === 'tasks'" label="满分" width="86">
          <template #default="{ row }">{{ row.maxScore }}</template>
        </el-table-column>
        <el-table-column v-if="kind === 'tasks'" label="截止时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.deadlineAt) }}</template>
        </el-table-column>
        <el-table-column v-if="kind !== 'announcements'" label="附件" width="92">
          <template #default="{ row }">
            <el-tag v-if="row.attachmentFileId || row.attachmentUrl" type="success" effect="plain">有附件</el-tag>
            <span v-else class="muted">无</span>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button v-if="kind === 'tasks'" text type="primary" :icon="Checked" @click="openReviews(row)">批阅</el-button>
            <el-button text type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm :title="`确认删除该${itemName}？`" confirm-button-text="删除" cancel-button-text="取消" @confirm="handleDelete(row)">
              <template #reference>
                <el-button text type="danger" :icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </PageTable>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        class="pager"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
        :total="filteredItems.length"
        @size-change="query.page = 1"
      />
    </section>

    <el-drawer v-model="drawerVisible" :title="editingId ? `编辑${itemName}` : `新建${itemName}`" size="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="86px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="80" show-word-limit />
        </el-form-item>
        <el-form-item v-if="kind === 'announcements' && mode === 'admin'" label="范围" prop="scope">
          <el-radio-group v-model="form.scope">
            <el-radio-button label="GLOBAL">全局</el-radio-button>
            <el-radio-button label="GROUP">组内</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="kind !== 'announcements' || form.scope === 'GROUP'" label="责任包" prop="groupId">
          <el-select v-model="form.groupId" class="full" placeholder="请选择责任包">
            <el-option v-for="group in groups" :key="group.id" :label="group.name" :value="group.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="kind === 'tasks'" label="满分" prop="maxScore">
          <el-input-number v-model="form.maxScore" :min="1" :max="1000" />
        </el-form-item>
        <el-form-item v-if="kind === 'tasks'" label="截止时间" prop="deadlineAt">
          <el-date-picker v-model="form.deadlineAt" class="full" type="datetime" value-format="YYYY-MM-DDTHH:mm:ssZ" />
        </el-form-item>
        <el-form-item v-if="kind !== 'announcements'" label="附件">
          <FileUploader
            v-model="form.attachmentFileId"
            :purpose="kind === 'tasks' ? 'TASK_ATTACHMENT' : 'MATERIAL_ATTACHMENT'"
            :existing-file-name="form.attachmentFileName"
            @clear="form.removeAttachment = true"
            @uploaded="form.removeAttachment = false"
          />
        </el-form-item>
        <el-form-item label="正文" prop="contentMarkdown">
          <MarkdownEditor v-model="form.contentMarkdown" :rows="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-drawer>

    <el-drawer v-model="reviewVisible" title="任务批阅" size="720px">
      <div class="review-head">
        <strong>{{ currentTask?.title }}</strong>
        <el-button :icon="Download" @click="downloadSubmissions">批下载</el-button>
      </div>
      <PageTable :data="submissions" :loading="reviewLoading" empty-text="暂无提交记录">
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="username" label="账号" width="130" />
        <el-table-column label="提交时间" min-width="170">
          <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.reviewed ? 'success' : 'warning'" effect="plain">{{ row.reviewed ? '已批阅' : '待批阅' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="分数" width="80" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="openReviewDialog(row)">评分</el-button>
            <el-popconfirm title="确认打回该提交？" confirm-button-text="打回" cancel-button-text="取消" @confirm="returnSubmission(row)">
              <template #reference>
                <el-button text type="warning">打回</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </PageTable>
    </el-drawer>

    <el-dialog v-model="scoreVisible" title="提交评分" width="420px">
      <el-form :model="scoreForm" label-width="72px">
        <el-form-item label="分数">
          <el-input-number v-model="scoreForm.score" :min="0" :max="currentTask?.maxScore || 100" />
        </el-form-item>
        <el-form-item label="评语">
          <el-input v-model="scoreForm.comment" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scoreVisible = false">取消</el-button>
        <el-button type="primary" :loading="scoreSubmitting" @click="submitReview">保存评分</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { Checked, Delete, Download, Edit, Plus, Refresh } from '@element-plus/icons-vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';

import {
  createAdminAnnouncement,
  createAdminMaterial,
  createAdminTask,
  deleteAdminAnnouncement,
  deleteAdminMaterial,
  deleteAdminTask,
  getAdminGroups,
  getAdminTaskBatchDownloadUrl,
  getAdminTaskSubmissions,
  getAnnouncements,
  getMaterials,
  getTasks,
  returnAdminSubmission,
  reviewAdminSubmission,
  updateAdminAnnouncement,
  updateAdminMaterial,
  updateAdminTask
} from '@/api/admin';
import {
  createLeaderAnnouncement,
  createLeaderMaterial,
  createLeaderTask,
  deleteLeaderAnnouncement,
  deleteLeaderMaterial,
  deleteLeaderTask,
  getGroups,
  getLeaderTaskBatchDownloadUrl,
  getLeaderTaskSubmissions,
  returnLeaderSubmission,
  reviewLeaderSubmission,
  updateLeaderAnnouncement,
  updateLeaderMaterial,
  updateLeaderTask
} from '@/api/leader';
import type { GroupSubmissionSummary } from '@/api/tasks';
import FileUploader from '@/components/common/FileUploader.vue';
import PageHeader from '@/components/common/PageHeader.vue';
import PageTable from '@/components/common/PageTable.vue';
import MarkdownEditor from '@/components/markdown/MarkdownEditor.vue';
import type { Announcement, Group, Material, Scope, Task } from '@/types/api';
import { scopeLabels } from '@/utils/labels';

type Mode = 'leader' | 'admin';
type Kind = 'announcements' | 'materials' | 'tasks';
type ManagedItem = Announcement | Material | Task;

const props = defineProps<{
  mode: Mode;
  kind: Kind;
  title: string;
  description: string;
}>();

const itemNameMap: Record<Kind, string> = {
  announcements: '公告',
  materials: '资料',
  tasks: '任务'
};

const itemName = computed(() => itemNameMap[props.kind]);
const loading = ref(false);
const submitting = ref(false);
const drawerVisible = ref(false);
const editingId = ref<number | null>(null);
const formRef = ref<FormInstance>();
const groups = ref<Group[]>([]);
const items = ref<ManagedItem[]>([]);
const query = reactive({
  page: 1,
  size: 10,
  groupId: undefined as number | undefined,
  scope: '' as '' | Scope,
  keyword: ''
});
const form = reactive({
  title: '',
  contentMarkdown: '',
  scope: 'GROUP' as Scope,
  groupId: undefined as number | undefined,
  attachmentFileId: null as number | null,
  attachmentFileName: '',
  removeAttachment: false,
  maxScore: 100,
  deadlineAt: ''
});
const rules = computed<FormRules>(() => ({
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  contentMarkdown: [{ required: true, message: '请输入正文', trigger: 'blur' }],
  groupId:
    props.kind !== 'announcements' || form.scope === 'GROUP'
      ? [{ required: true, message: '请选择责任包', trigger: 'change' }]
      : [],
  maxScore: props.kind === 'tasks' ? [{ required: true, message: '请输入满分', trigger: 'blur' }] : [],
  deadlineAt: props.kind === 'tasks' ? [{ required: true, message: '请选择截止时间', trigger: 'change' }] : []
}));

const filteredItems = computed(() => {
  const keyword = query.keyword.trim();
  return items.value.filter((item) => {
    if ('groupId' in item && query.groupId && item.groupId !== query.groupId) return false;
    if ('scope' in item && query.scope && item.scope !== query.scope) return false;
    if (!keyword) return true;
    return [item.title, 'contentMarkdown' in item ? item.contentMarkdown : undefined, 'content' in item ? item.content : undefined]
      .filter(Boolean)
      .some((value) => String(value).includes(keyword));
  });
});
const pagedItems = computed(() => {
  const start = (query.page - 1) * query.size;
  return filteredItems.value.slice(start, start + query.size);
});

const reviewVisible = ref(false);
const reviewLoading = ref(false);
const currentTask = ref<Task | null>(null);
const submissions = ref<GroupSubmissionSummary[]>([]);
const scoreVisible = ref(false);
const scoreSubmitting = ref(false);
const scoreTarget = ref<GroupSubmissionSummary | null>(null);
const scoreForm = reactive({ score: 0, comment: '' });

onMounted(async () => {
  await loadGroups();
  await loadItems();
});

async function loadGroups() {
  if (props.mode === 'admin') {
    groups.value = await getAdminGroups();
  } else {
    const result = await getGroups({ page: 1, size: 100 });
    groups.value = result.list;
  }
  if (!query.groupId && props.kind !== 'announcements' && groups.value.length) {
    query.groupId = groups.value[0].id;
  }
}

async function loadItems() {
  loading.value = true;
  try {
    if (props.kind === 'announcements') {
      items.value = await getAnnouncements({ scope: query.scope || undefined, keyword: query.keyword || undefined });
    } else if (props.kind === 'materials') {
      items.value = await getMaterials({ keyword: query.keyword || undefined, groupId: query.groupId });
    } else {
      items.value = await getTasks({ keyword: query.keyword || undefined, groupId: query.groupId });
    }
    query.page = 1;
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  editingId.value = null;
  resetForm();
  drawerVisible.value = true;
}

function openEdit(item: ManagedItem) {
  editingId.value = item.id;
  form.title = item.title;
  form.contentMarkdown = item.contentMarkdown || item.content || '';
  form.scope = 'scope' in item ? item.scope || 'GROUP' : 'GROUP';
  form.groupId = 'groupId' in item ? item.groupId || undefined : undefined;
  form.attachmentFileId = 'attachmentFileId' in item ? item.attachmentFileId || null : null;
  form.attachmentFileName = 'attachmentFileName' in item ? item.attachmentFileName || '' : '';
  form.removeAttachment = false;
  form.maxScore = 'maxScore' in item ? item.maxScore : 100;
  form.deadlineAt = 'deadlineAt' in item ? item.deadlineAt : '';
  drawerVisible.value = true;
}

function resetForm() {
  form.title = '';
  form.contentMarkdown = '';
  form.scope = props.mode === 'admin' ? 'GLOBAL' : 'GROUP';
  form.groupId = groups.value[0]?.id;
  form.attachmentFileId = null;
  form.attachmentFileName = '';
  form.removeAttachment = false;
  form.maxScore = 100;
  form.deadlineAt = '';
}

async function submitForm() {
  await formRef.value?.validate();
  submitting.value = true;
  try {
    if (props.kind === 'announcements') {
      const payload = {
        title: form.title,
        content: form.contentMarkdown,
        scope: props.mode === 'leader' ? ('GROUP' as Scope) : form.scope,
        groupId: form.scope === 'GROUP' ? form.groupId || null : null
      };
      if (props.mode === 'admin') {
        editingId.value ? await updateAdminAnnouncement(editingId.value, payload) : await createAdminAnnouncement(payload);
      } else {
        editingId.value ? await updateLeaderAnnouncement(editingId.value, payload) : await createLeaderAnnouncement(payload);
      }
    } else if (props.kind === 'materials') {
      const payload = {
        title: form.title,
        contentMarkdown: form.contentMarkdown,
        attachmentFileId: form.attachmentFileId,
        removeAttachment: form.removeAttachment
      };
      if (!form.groupId) return;
      if (props.mode === 'admin') {
        editingId.value
          ? await updateAdminMaterial(form.groupId, editingId.value, payload)
          : await createAdminMaterial(form.groupId, payload);
      } else {
        editingId.value
          ? await updateLeaderMaterial(form.groupId, editingId.value, payload)
          : await createLeaderMaterial(form.groupId, payload);
      }
    } else {
      const payload = {
        title: form.title,
        contentMarkdown: form.contentMarkdown,
        attachmentFileId: form.attachmentFileId,
        removeAttachment: form.removeAttachment,
        maxScore: form.maxScore,
        deadlineAt: form.deadlineAt
      };
      if (!form.groupId) return;
      if (props.mode === 'admin') {
        editingId.value ? await updateAdminTask(form.groupId, editingId.value, payload) : await createAdminTask(form.groupId, payload);
      } else {
        editingId.value
          ? await updateLeaderTask(form.groupId, editingId.value, payload)
          : await createLeaderTask(form.groupId, payload);
      }
    }
    ElMessage.success('保存成功');
    drawerVisible.value = false;
    await loadItems();
  } finally {
    submitting.value = false;
  }
}

async function handleDelete(item: ManagedItem) {
  if (props.kind === 'announcements') {
    props.mode === 'admin' ? await deleteAdminAnnouncement(item.id) : await deleteLeaderAnnouncement(item.id);
  } else if (props.kind === 'materials') {
    const groupId = 'groupId' in item ? item.groupId : undefined;
    if (!groupId) return;
    props.mode === 'admin' ? await deleteAdminMaterial(groupId, item.id) : await deleteLeaderMaterial(groupId, item.id);
  } else {
    const groupId = 'groupId' in item ? item.groupId : undefined;
    if (!groupId) return;
    props.mode === 'admin' ? await deleteAdminTask(groupId, item.id) : await deleteLeaderTask(groupId, item.id);
  }
  ElMessage.success('删除成功');
  await loadItems();
}

async function openReviews(task: Task) {
  currentTask.value = task;
  reviewVisible.value = true;
  reviewLoading.value = true;
  try {
    submissions.value =
      props.mode === 'admin' ? await getAdminTaskSubmissions(task.id) : await getLeaderTaskSubmissions(task.id);
  } finally {
    reviewLoading.value = false;
  }
}

function openReviewDialog(row: GroupSubmissionSummary) {
  scoreTarget.value = row;
  scoreForm.score = row.score || 0;
  scoreForm.comment = '';
  scoreVisible.value = true;
}

async function submitReview() {
  if (!currentTask.value || !scoreTarget.value) return;
  scoreSubmitting.value = true;
  try {
    const payload = { score: scoreForm.score, comment: scoreForm.comment };
    props.mode === 'admin'
      ? await reviewAdminSubmission(currentTask.value.id, scoreTarget.value.userId, payload)
      : await reviewLeaderSubmission(currentTask.value.id, scoreTarget.value.userId, payload);
    ElMessage.success('评分已保存');
    scoreVisible.value = false;
    await openReviews(currentTask.value);
  } finally {
    scoreSubmitting.value = false;
  }
}

async function returnSubmission(row: GroupSubmissionSummary) {
  if (!currentTask.value) return;
  props.mode === 'admin'
    ? await returnAdminSubmission(currentTask.value.id, row.userId)
    : await returnLeaderSubmission(currentTask.value.id, row.userId);
  ElMessage.success('已打回提交');
}

function downloadSubmissions() {
  if (!currentTask.value?.groupId) return;
  const url =
    props.mode === 'admin'
      ? getAdminTaskBatchDownloadUrl(currentTask.value.groupId, currentTask.value.id)
      : getLeaderTaskBatchDownloadUrl(currentTask.value.groupId, currentTask.value.id);
  window.open(url, '_blank');
}

function getGroupName(groupId?: number | null) {
  return groups.value.find((group) => group.id === groupId)?.name || (groupId ? `责任包 #${groupId}` : '-');
}

function formatDateTime(value?: string | null) {
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
.content-toolbar,
.toolbar-left {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.toolbar-left {
  flex: 1;
}

.keyword-input {
  width: 260px;
}

.group-select {
  width: 220px;
}

.scope-select {
  width: 130px;
}

.full {
  width: 100%;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.review-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}
</style>
