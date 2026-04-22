<template>
  <div class="app-container">
    <router-view v-slot="{ Component }">
      <transition name="fade-transform" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </div>
</template>

<script setup>
</script>

<style>
:root {
  --primary-color: #4361ee;
  --primary-light: #6b83f2;
  --primary-dark: #3249cb;
  --primary-bg: #eef1ff;
  --success-color: #2ec4b6;
  --warning-color: #ff9f1c;
  --danger-color: #e71d36;
  --info-color: #8d99ae;
  --text-primary: #1a1a2e;
  --text-regular: #4a4a68;
  --text-secondary: #8d99ae;
  --text-placeholder: #b0b8c8;
  --bg-page: #f0f2f5;
  --bg-card: #ffffff;
  --bg-sidebar: #1a1a2e;
  --bg-sidebar-hover: #252542;
  --bg-sidebar-active: #2d2d50;
  --border-color: #e8ecf1;
  --border-light: #f0f2f5;
  --shadow-sm: 0 1px 3px rgba(0, 0, 0, 0.06);
  --shadow-md: 0 4px 12px rgba(0, 0, 0, 0.08);
  --shadow-lg: 0 8px 24px rgba(0, 0, 0, 0.12);
  --shadow-card: 0 2px 8px rgba(67, 97, 238, 0.06);
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 16px;
  --sidebar-width: 240px;
  --sidebar-collapsed-width: 64px;
  --header-height: 60px;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  width: 100%;
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.app-container {
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.25s ease;
}
.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-10px);
}
.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(10px);
}

.el-card {
  border: none !important;
  border-radius: var(--radius-md) !important;
  box-shadow: var(--shadow-card) !important;
}

.el-card__header {
  border-bottom: 1px solid var(--border-light) !important;
  padding: 16px 20px !important;
}

.el-card__body {
  padding: 20px !important;
}

.el-table {
  --el-table-border-color: var(--border-light);
  --el-table-header-bg-color: #f8f9fc;
  border-radius: var(--radius-sm) !important;
  overflow: hidden;
}

.el-table th.el-table__cell {
  background-color: #f8f9fc !important;
  color: var(--text-primary) !important;
  font-weight: 600 !important;
  font-size: 13px !important;
}

.el-table td.el-table__cell {
  font-size: 13px !important;
  color: var(--text-regular) !important;
}

.el-table .el-table__row:hover > td {
  background-color: var(--primary-bg) !important;
}

.el-dialog {
  border-radius: var(--radius-lg) !important;
  overflow: hidden;
}

.el-dialog__header {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
  padding: 16px 20px !important;
  margin-right: 0 !important;
}

.el-dialog__title {
  color: #fff !important;
  font-size: 16px !important;
  font-weight: 600 !important;
}

.el-dialog__headerbtn .el-dialog__close {
  color: rgba(255, 255, 255, 0.8) !important;
  font-size: 18px !important;
}

.el-dialog__headerbtn:hover .el-dialog__close {
  color: #fff !important;
}

.el-dialog__body {
  padding: 24px !important;
}

.el-dialog__footer {
  padding: 12px 20px 16px !important;
  border-top: 1px solid var(--border-light);
}

.el-button--primary {
  --el-button-bg-color: var(--primary-color);
  --el-button-border-color: var(--primary-color);
  --el-button-hover-bg-color: var(--primary-light);
  --el-button-hover-border-color: var(--primary-light);
  --el-button-active-bg-color: var(--primary-dark);
  --el-button-active-border-color: var(--primary-dark);
}

.el-pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.el-tag {
  border-radius: 20px !important;
  padding: 0 10px !important;
}

.el-input__wrapper {
  border-radius: var(--radius-sm) !important;
}

.el-select .el-input__wrapper {
  border-radius: var(--radius-sm) !important;
}

.el-breadcrumb {
  margin-bottom: 16px;
  font-size: 13px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-header-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #fff;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-light));
  flex-shrink: 0;
}

.page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.page-header-desc {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
  flex-wrap: wrap;
  padding: 16px 20px;
  background: #f8f9fc;
  border-radius: var(--radius-md);
}

.search-bar .el-input,
.search-bar .el-select {
  width: auto;
}

.action-bar {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}

::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #a1a1a1;
}

::-webkit-scrollbar-track {
  background: transparent;
}
</style>
