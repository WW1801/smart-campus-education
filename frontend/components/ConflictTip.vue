<!-- 通用冲突提示组件，负责展示排课冲突信息。 -->
<template>
  <div class="conflict-tip">
    <div v-for="(conflict, index) in conflicts" :key="index" class="conflict-item" :class="'priority-' + conflict.priority">
      <div class="conflict-header">
        <el-tag :type="priorityType(conflict.priority)" size="small" effect="dark">
          {{ priorityLabel(conflict.priority) }}
        </el-tag>
        <span class="conflict-type">{{ typeLabel(conflict.type) }}</span>
      </div>
      <div class="conflict-body">
        <p class="conflict-message">{{ conflict.reason || conflict.message }}</p>
        <p v-if="conflict.suggestion" class="conflict-suggestion">建议：{{ conflict.suggestion }}</p>
        <div class="conflict-details" v-if="conflict.conflictCourseName || conflict.conflictTeacherName || conflict.conflictDayOfWeek">
          <span v-if="conflict.conflictCourseName" class="detail-item">
            <el-icon><Document /></el-icon>
            冲突课程：{{ conflict.conflictCourseName }}
          </span>
          <span v-if="conflict.conflictTeacherName" class="detail-item">
            <el-icon><User /></el-icon>
            教师：{{ conflict.conflictTeacherName }}
          </span>
          <span v-if="conflict.conflictClassroomName" class="detail-item">
            <el-icon><OfficeBuilding /></el-icon>
            教室：{{ conflict.conflictClassroomName }}
          </span>
          <span v-if="conflict.conflictDayOfWeek" class="detail-item">
            <el-icon><Clock /></el-icon>
            时间：周{{ conflict.conflictDayOfWeek }} 第{{ conflict.conflictStartPeriod }}-{{ conflict.conflictEndPeriod }}节
          </span>
        </div>
        <div v-if="conflict.candidateClassroomName" class="conflict-details">
          <span class="detail-item">
            <el-icon><OfficeBuilding /></el-icon>
            候选教室：{{ conflict.candidateClassroomName }}（容量 {{ conflict.candidateClassroomCapacity ?? '未维护' }}）
          </span>
          <span v-if="conflict.requiredCapacity != null" class="detail-item">所需容量：{{ conflict.requiredCapacity }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Document, User, Clock, OfficeBuilding } from '@element-plus/icons-vue'
import appData from '../config/appData.json'

const props = defineProps({
  conflicts: { type: Array, default: () => [] }
})

// 获取优先级标签
const priorityLabel = (p) => appData.conflicts.priorityLabels[p] || p
// 获取优先级类型
const priorityType = (p) => appData.conflicts.priorityTypes[p] || 'warning'
// 获取类型标签
const typeLabel = (t) => appData.conflicts.typeLabels[t] || t
</script>

<style scoped>
.conflict-tip {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.conflict-item {
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid;
}

.conflict-item.priority-P0 { border-color: var(--vermilion); background: var(--surface); }
.conflict-item.priority-P1 { border-color: var(--vermilion); background: var(--paper); }
.conflict-item.priority-P2 { border-color: var(--line); background: var(--paper); }
.conflict-item.priority-P3 { border-color: var(--blue); background: var(--surface); }

.conflict-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--bg-soft);
}

.conflict-type {
  font-weight: 600;
  font-size: 13px;
  color: var(--text-primary);
}

.conflict-body {
  padding: 8px 12px;
}

.conflict-message {
  font-size: 13px;
  color: var(--text-regular);
  margin: 0 0 6px 0;
}

.conflict-suggestion {
  margin: 0 0 8px;
  color: var(--text-regular);
  font-size: 13px;
  font-weight: 600;
  overflow-wrap: anywhere;
}

.conflict-details {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-secondary);
}

.detail-item .el-icon {
  font-size: 14px;
}
</style>
