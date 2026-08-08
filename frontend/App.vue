<!-- 应用根组件：承载全局设计变量、Element Plus 主题映射与路由过渡。 -->
<template>
  <div class="app-container">
    <router-view v-slot="{ Component }">
      <transition name="route-fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </div>
</template>

<script setup>
</script>

<style>
:root {
  --ink: #142b4a;
  --text-primary: #142b4a;
  --text-regular: #4a5568;
  --text-secondary: #4a5568;
  --text-muted: #7d8fa0;
  --text-placeholder: #7d8fa0;
  --paper: #f5f8fc;
  --surface: #ffffff;
  --bg-page: #f5f8fc;
  --bg-card: #ffffff;
  --bg-soft: #ecf1f6;
  --line: #d4deea;
  --border-color: #d4deea;
  --border-light: #e8edf2;
  --blue: #295da8;
  --blue-bright: #4a8fd6;
  --primary-color: #295da8;
  --primary-light: #4a8fd6;
  --primary-dark: #204b89;
  --primary-bg: #edf4fb;
  --sage: #54785e;
  --success-color: #54785e;
  --vermilion: #b7352a;
  --danger-color: #b7352a;
  --warning-color: #b7352a;
  --info-color: #7d8fa0;
  --sidebar-grad: linear-gradient(180deg, #16294a 0%, #102138 100%);
  --button-grad: linear-gradient(135deg, #4a8fd6 0%, #295da8 100%);
  --shadow-sm: 0 1px 2px rgba(20, 43, 74, 0.05);
  --shadow-card: 0 1px 0 #d4deea, 0 5px 16px rgba(16, 32, 56, 0.06);
  --shadow-dialog: 0 18px 48px rgba(16, 32, 56, 0.18);
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 14px;
  --display-font: 'Noto Serif SC', 'Source Han Serif SC', STSong, SimSun, serif;
  --latin-font: 'Sora', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  --body-font: 'Sora', -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  --sidebar-width: 246px;
  --sidebar-collapsed-width: 82px;
  --header-height: 64px;

  --el-color-primary: #295da8;
  --el-color-primary-light-3: #4a8fd6;
  --el-color-primary-light-5: #79a9da;
  --el-color-primary-light-7: #b4cee9;
  --el-color-primary-light-8: #d4e3f2;
  --el-color-primary-light-9: #edf4fb;
  --el-color-primary-dark-2: #204b89;
  --el-color-success: #54785e;
  --el-color-success-light-9: #f0f5f1;
  --el-color-warning: #b7352a;
  --el-color-warning-light-9: #fbf0ef;
  --el-color-danger: #b7352a;
  --el-color-danger-light-9: #fbf0ef;
  --el-color-info: #7d8fa0;
  --el-text-color-primary: #142b4a;
  --el-text-color-regular: #4a5568;
  --el-text-color-secondary: #4a5568;
  --el-text-color-placeholder: #4a5568;
  --el-border-color: #d4deea;
  --el-border-color-light: #e8edf2;
  --el-border-color-lighter: #eef2f6;
  --el-fill-color-light: #f5f8fc;
  --el-fill-color-lighter: #f8fafc;
  --el-bg-color-page: #f5f8fc;
  --el-border-radius-base: 8px;
  --el-font-family: var(--body-font);
}

* {
  box-sizing: border-box;
}

.visually-hidden {
  position: absolute !important;
  width: 1px !important;
  height: 1px !important;
  padding: 0 !important;
  overflow: hidden !important;
  clip: rect(0, 0, 0, 0) !important;
  white-space: nowrap !important;
  border: 0 !important;
}

html,
body,
#app {
  width: 100%;
  min-width: 320px;
  height: 100%;
  margin: 0;
}

body {
  color: var(--ink);
  background: var(--paper);
  font-family: var(--body-font);
  font-size: 14px;
  line-height: 1.6;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

button,
input,
textarea,
select {
  font: inherit;
}

button,
a {
  -webkit-tap-highlight-color: transparent;
}

:focus-visible {
  outline: 2px solid var(--blue-bright);
  outline-offset: 2px;
}

.app-container {
  width: 100%;
  min-height: 100%;
}

.route-fade-enter-active,
.route-fade-leave-active {
  transition: opacity 160ms ease-out;
}

.route-fade-enter-from,
.route-fade-leave-to {
  opacity: 0;
}

/* Element Plus 基础组件统一映射到校园控制台的纸面与墨蓝语义。 */
.el-card {
  border: 1px solid var(--line) !important;
  border-radius: var(--radius-md) !important;
  box-shadow: var(--shadow-card) !important;
}

.el-card__header {
  padding: 17px 20px !important;
  border-bottom: 1px solid var(--border-light) !important;
}

.el-card__body {
  padding: 20px !important;
}

.el-button {
  min-height: 44px;
  border-radius: 8px;
  font-weight: 600;
}

.el-button.is-link {
  min-height: 44px;
}

.el-button--primary:not(.is-link) {
  --el-button-bg-color: var(--blue);
  --el-button-border-color: var(--blue);
  --el-button-hover-bg-color: var(--blue-bright);
  --el-button-hover-border-color: var(--blue-bright);
  --el-button-active-bg-color: var(--primary-dark);
  --el-button-active-border-color: var(--primary-dark);
}

.el-input__wrapper,
.el-select__wrapper,
.el-textarea__inner,
.el-date-editor.el-input__wrapper {
  min-height: 44px;
  border-radius: 8px !important;
  box-shadow: 0 0 0 1px var(--line) inset !important;
}

.el-input__wrapper.is-focus,
.el-select__wrapper.is-focused,
.el-textarea__inner:focus {
  box-shadow: 0 0 0 1px var(--blue) inset !important;
}

.el-table {
  --el-table-border-color: var(--border-light);
  --el-table-header-bg-color: var(--bg-soft);
  --el-table-row-hover-bg-color: var(--primary-bg);
  color: var(--text-regular);
  border-radius: var(--radius-sm);
}

.el-table th.el-table__cell {
  height: 48px;
  color: var(--ink) !important;
  background: var(--bg-soft) !important;
  font-size: 13px;
  font-weight: 700;
}

.el-table td.el-table__cell {
  height: 52px;
  font-size: 13px;
}

.el-table .el-table__row:hover > td.el-table__cell {
  background: var(--primary-bg) !important;
}

.el-tag {
  min-height: 28px;
  padding: 0 10px !important;
  border-radius: 5px !important;
  font-weight: 600;
}

.el-dialog {
  border-radius: var(--radius-lg) !important;
  box-shadow: var(--shadow-dialog) !important;
  overflow: hidden;
}

.el-dialog__header {
  padding: 20px 22px 16px !important;
  margin-right: 0 !important;
  border-bottom: 1px solid var(--border-light);
}

.el-dialog__title {
  color: var(--ink) !important;
  font-family: var(--display-font);
  font-size: 19px !important;
  font-weight: 700 !important;
}

.el-dialog__body {
  padding: 22px !important;
}

.el-dialog__footer {
  padding: 14px 22px 20px !important;
  border-top: 1px solid var(--border-light);
}

.el-pagination {
  justify-content: flex-end;
  margin-top: 18px;
}

.el-message-box {
  border-radius: var(--radius-lg) !important;
}

/* 使用几何居中替代组件默认的行内基线居中，避免确认框在不同字体下产生纵向偏移。 */
.is-message-box .el-overlay-message-box {
  display: flex;
  align-items: center;
  justify-content: center;
}

.is-message-box .el-overlay-message-box::after {
  display: none;
}

/* 退出登录确认框：弹层会被 Teleport 到 body，因此在根样式中提供独立的实体面板。 */
.el-message-box.logout-confirm {
  --el-messagebox-width: 460px;
  padding: 0;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg) !important;
  box-shadow: var(--shadow-dialog);
}

.logout-confirm .el-message-box__header {
  padding: 24px 24px 12px;
}

.logout-confirm .el-message-box__title {
  color: var(--ink);
  font-family: var(--display-font);
  font-size: 20px;
  font-weight: 700;
}

.logout-confirm .el-message-box__headerbtn {
  top: 10px;
  right: 10px;
  border-radius: 8px;
}

.logout-confirm .el-message-box__headerbtn:hover {
  background: var(--bg-soft);
}

.logout-confirm .el-message-box__content {
  padding: 4px 24px 24px;
  color: var(--text-regular);
}

.logout-confirm .el-message-box__container {
  align-items: flex-start;
  gap: 14px;
}

.logout-confirm .el-message-box__status {
  display: grid;
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
  margin-top: 1px;
  place-items: center;
  color: var(--vermilion);
  background: var(--el-color-warning-light-9);
  border: 1px solid rgba(183, 53, 42, 0.2);
  border-radius: 10px;
  font-size: 22px;
}

.logout-confirm .el-message-box__message {
  padding-top: 8px;
}

.logout-confirm .el-message-box__message p {
  line-height: 1.7;
}

.logout-confirm .el-message-box__btns {
  gap: 10px;
  padding: 16px 24px 20px;
  background: var(--bg-soft);
  border-top: 1px solid var(--border-light);
}

.logout-confirm .el-message-box__btns .el-button {
  min-width: 104px;
  margin-left: 0;
}

.logout-confirm .logout-confirm-button {
  --el-button-bg-color: var(--vermilion);
  --el-button-border-color: var(--vermilion);
  --el-button-hover-bg-color: #9f2d24;
  --el-button-hover-border-color: #9f2d24;
  --el-button-active-bg-color: #84251e;
  --el-button-active-border-color: #84251e;
}

.page-container {
  width: 100%;
  max-width: 1500px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 20px;
}

.page-header-left {
  display: flex;
  align-items: center;
  gap: 13px;
  min-width: 0;
}

.page-header-icon,
.page-header-icon[style] {
  display: grid;
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
  place-items: center;
  color: var(--blue) !important;
  background: var(--primary-bg) !important;
  border: 1px solid var(--line);
  border-radius: 8px;
}

.page-header-title {
  color: var(--ink) !important;
  font-family: var(--display-font);
  font-size: clamp(20px, 2vw, 25px) !important;
  font-weight: 700 !important;
  letter-spacing: -0.02em;
}

.page-header-desc {
  margin-top: 2px;
  color: var(--text-regular) !important;
  font-size: 13px;
}

.search-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 18px;
  padding: 14px;
  background: var(--bg-soft) !important;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md) !important;
}

.action-bar,
.action-group {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

/* 旧页面的指标卡在全局主题下合并为连续分栏，避免漂浮卡片墙。 */
.page-container .stats-grid {
  gap: 0 !important;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

.page-container .stats-grid > .el-card {
  border: 0 !important;
  border-right: 1px solid var(--border-light) !important;
  border-radius: 0 !important;
  box-shadow: none !important;
}

.page-container .stats-grid > .el-card:last-child {
  border-right: 0 !important;
}

.stat-value,
.metric-value,
.ledger-id {
  font-family: var(--display-font);
}

::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-thumb {
  background: var(--text-muted);
  border: 2px solid transparent;
  border-radius: 999px;
  background-clip: padding-box;
}

::-webkit-scrollbar-track {
  background: transparent;
}

@media (max-width: 768px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .page-header > .el-button,
  .page-header > .action-group {
    width: 100%;
  }

  .page-header > .el-button {
    margin-left: 0;
  }

  .search-bar .el-input,
  .search-bar .el-select,
  .search-bar .el-date-editor,
  .search-bar .el-button {
    width: 100% !important;
    margin-left: 0;
  }

  .el-card__body {
    padding: 15px !important;
  }

  .el-pagination {
    justify-content: flex-start;
    overflow-x: auto;
    padding-bottom: 4px;
  }

  /* 手机端取消固定操作列，避免其覆盖姓名、角色等关键字段；表格仍可横向滚动查看。 */
  .el-table .el-table-fixed-column--left,
  .el-table .el-table-fixed-column--right {
    position: static !important;
    right: auto !important;
    left: auto !important;
  }

  .el-dialog {
    width: calc(100vw - 28px) !important;
    margin-top: 5vh !important;
  }

  .el-message-box.logout-confirm {
    width: calc(100vw - 32px);
  }

  .logout-confirm .el-message-box__header {
    padding: 22px 20px 10px;
  }

  .logout-confirm .el-message-box__content {
    padding: 4px 20px 20px;
  }

  .logout-confirm .el-message-box__btns {
    padding: 14px 20px 18px;
  }

  .logout-confirm .el-message-box__btns .el-button {
    min-width: 0;
    flex: 1;
  }

  .page-container .stats-grid > .el-card {
    border-right: 0 !important;
    border-bottom: 1px solid var(--border-light) !important;
  }

  .page-container .stats-grid > .el-card:last-child {
    border-bottom: 0 !important;
  }
}

@media (prefers-reduced-motion: reduce) {
  html {
    scroll-behavior: auto;
  }

  .route-fade-enter-active,
  .route-fade-leave-active,
  .sidebar {
    transition: none !important;
  }
}
</style>
