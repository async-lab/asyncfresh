<template>
  <article class="markdown-body" v-html="safeHtml" />
</template>

<script setup lang="ts">
import DOMPurify from 'dompurify';
import MarkdownIt from 'markdown-it';
import { computed } from 'vue';

const props = defineProps<{
  content?: string;
}>();

const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true
});

const safeHtml = computed(() => DOMPurify.sanitize(md.render(props.content || '')));
</script>

<style scoped>
.markdown-body {
  line-height: 1.72;
  color: var(--app-text);
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin: 20px 0 10px;
}

.markdown-body :deep(p) {
  margin: 8px 0;
}

.markdown-body :deep(code) {
  padding: 2px 7px;
  border: 1px solid rgba(126, 114, 97, 0.1);
  border-radius: 10px;
  background: rgba(247, 242, 234, 0.95);
  color: #594f45;
}

.markdown-body :deep(pre) {
  overflow-x: auto;
  margin: 14px 0;
  padding: 14px 16px;
  border: 1px solid rgba(126, 114, 97, 0.12);
  border-radius: 18px;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.92), rgba(244, 238, 229, 0.84)),
    var(--app-surface);
  box-shadow:
    inset 1px 1px 0 rgba(255, 255, 255, 0.68),
    8px 8px 18px rgba(145, 128, 106, 0.08);
}

.markdown-body :deep(pre code) {
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  color: inherit;
}

.markdown-body :deep(blockquote) {
  margin: 14px 0;
  padding: 10px 14px;
  border: 1px solid rgba(165, 155, 212, 0.18);
  border-radius: 16px;
  background: rgba(165, 155, 212, 0.07);
  color: var(--app-muted);
}

.markdown-body :deep(table) {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  overflow: hidden;
  border: 1px solid rgba(126, 114, 97, 0.1);
  border-radius: 18px;
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  padding: 10px 12px;
  border-bottom: 1px solid rgba(126, 114, 97, 0.08);
}

.markdown-body :deep(th) {
  background: rgba(247, 242, 234, 0.9);
}

.markdown-body :deep(tr:last-child td) {
  border-bottom: 0;
}
</style>
