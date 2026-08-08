<!-- 未匹配路由的恢复页面，不把错误地址呈现为空白内容区。 -->
<template>
  <main class="not-found-page">
    <section class="not-found-sheet" aria-labelledby="not-found-title">
      <span class="error-seal" aria-hidden="true">404</span>
      <p class="error-kicker">ROUTE NOT FOUND</p>
      <h1 id="not-found-title">没有找到这个教务页面</h1>
      <p>当前地址不存在，或对应功能已经调整。你可以返回角色首页继续处理教务事项。</p>
      <div class="not-found-actions">
        <el-button type="primary" @click="goHome">返回工作台</el-button>
        <el-button @click="router.back()">返回上一页</el-button>
      </div>
    </section>
  </main>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { getDefaultHomePath } from '../router'

const router = useRouter()
const store = useStore()
const goHome = () => router.push(getDefaultHomePath(store.state.user?.roleId) || '/')
</script>

<style scoped>
.not-found-page {
  display: grid;
  min-height: 100vh;
  padding: 24px;
  place-items: center;
  background: var(--paper);
}

.not-found-sheet {
  width: min(100%, 620px);
  padding: clamp(28px, 6vw, 56px);
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
}

.error-seal {
  display: inline-flex;
  min-height: 44px;
  align-items: center;
  padding: 0 12px;
  color: var(--vermilion);
  border: 1px solid var(--vermilion);
  border-radius: var(--radius-sm);
  font-family: var(--latin-font);
  font-weight: 700;
}

.error-kicker {
  margin: 24px 0 5px;
  color: var(--blue);
  font-family: var(--latin-font);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

h1 {
  margin: 0;
  font-family: var(--display-font);
  font-size: clamp(28px, 6vw, 42px);
  line-height: 1.25;
}

.not-found-sheet > p:last-of-type {
  max-width: 48ch;
  margin: 14px 0 28px;
  color: var(--text-secondary);
}

.not-found-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

@media (max-width: 480px) {
  .not-found-actions :deep(.el-button) {
    width: 100%;
    margin-left: 0;
  }
}
</style>
