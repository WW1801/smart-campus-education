<!-- 页面级错误状态：与真实空数据分离，并把恢复动作交还给当前业务页面。 -->
<template>
  <div class="page-error-state" role="alert">
    <span class="page-error-code">{{ code }}</span>
    <div class="page-error-copy">
      <strong>{{ title }}</strong>
      <p>{{ description }}</p>
    </div>
    <el-button type="primary" plain :loading="retrying" @click="$emit('retry')">{{ retryText }}</el-button>
  </div>
</template>

<script setup>
defineProps({
  code: { type: String, default: 'ED-503' },
  title: { type: String, default: '暂时无法加载数据' },
  description: { type: String, default: '请检查网络连接后重试；当前页面未把请求失败显示为空数据。' },
  retryText: { type: String, default: '重新加载' },
  retrying: { type: Boolean, default: false }
})

defineEmits(['retry'])
</script>

<style scoped>
.page-error-state {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  min-height: 92px;
  margin-bottom: 16px;
  padding: 16px 18px;
  color: var(--ink);
  background: var(--surface);
  border: 1px solid var(--vermilion);
  border-radius: var(--radius-md);
}

.page-error-code {
  padding: 5px 8px;
  color: var(--vermilion);
  background: var(--surface);
  border: 1px solid var(--vermilion);
  border-radius: var(--radius-sm);
  font-family: var(--latin-font);
  font-size: 12px;
  font-weight: 700;
}

.page-error-copy {
  min-width: 0;
}

.page-error-copy strong {
  display: block;
  font-size: 15px;
}

.page-error-copy p {
  margin: 3px 0 0;
  color: var(--text-secondary);
}

@media (max-width: 640px) {
  .page-error-state {
    grid-template-columns: 1fr;
  }

  .page-error-code {
    justify-self: start;
  }

  .page-error-state :deep(.el-button) {
    width: 100%;
  }
}
</style>

