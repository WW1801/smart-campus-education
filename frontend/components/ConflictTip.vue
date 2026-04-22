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
        <p class="conflict-message">{{ conflict.message }}</p>
        <div class="conflict-details" v-if="conflict.conflictCourseName || conflict.conflictTeacherName || conflict.conflictDayOfWeek">
          <span v-if="conflict.conflictCourseName" class="detail-item">
            <el-icon><Document /></el-icon>
            冲突课程：{{ conflict.conflictCourseName }}
          </span>
          <span v-if="conflict.conflictTeacherName" class="detail-item">
            <el-icon><User /></el-icon>
            教师：{{ conflict.conflictTeacherName }}
          </span>
          <span v-if="conflict.conflictDayOfWeek" class="detail-item">
            <el-icon><Clock /></el-icon>
            时间：周{{ conflict.conflictDayOfWeek }} 第{{ conflict.conflictStartPeriod }}-{{ conflict.conflictEndPeriod }}节
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Document, User, Clock } from '@element-plus/icons-vue'

const props = defineProps({
  conflicts: { type: Array, default: () => [] }
})

const priorityLabel = (p) => ({ P0: '不可调和', P1: '容量不足', P2: '可换教室', P3: '可调时段' }[p] || p)
const priorityType = (p) => ({ P0: 'error', P1: 'warning', P2: 'warning', P3: 'info' }[p] || 'warning')
const typeLabel = (t) => ({ teacher: '教师冲突', classroom: '教室冲突', class: '班级冲突', capacity: '容量冲突' }[t] || t)
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

.conflict-item.priority-P0 { border-color: #fca5a5; background: #fef2f2; }
.conflict-item.priority-P1 { border-color: #fcd34d; background: #fffbeb; }
.conflict-item.priority-P2 { border-color: #fde68a; background: #fefce8; }
.conflict-item.priority-P3 { border-color: #93c5fd; background: #eff6ff; }

.conflict-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(0, 0, 0, 0.03);
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
