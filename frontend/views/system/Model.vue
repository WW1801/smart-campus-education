<!-- 系统管理模型管理页面组件，负责处理系统管理模块的页面展示与交互。 -->
<template>
  <div class="page-container model-page">
    <div class="page-header">
      <div class="page-header-left">
        <div class="page-header-icon">ER</div>
        <div>
          <div class="page-header-title">数据模型</div>
          <div class="page-header-desc">教务管理系统核心实体关系图，可用于说明书、演示和需求分析展示</div>
        </div>
      </div>
      <div class="action-bar">
        <el-button type="primary" @click="scrollToDiagram">查看 ER 图</el-button>
        <el-button type="success" @click="diagramPreviewVisible = true">放大查看</el-button>
        <el-button type="warning" @click="scrollToStructure">查看功能结构图</el-button>
        <el-button @click="scrollToTables">查看数据表</el-button>
      </div>
    </div>

    <el-row :gutter="16" class="metric-row">
      <el-col :xs="12" :sm="12" :md="6" v-for="item in metrics" :key="item.label">
        <el-card class="metric-card">
          <div class="metric-label">{{ item.label }}</div>
          <div class="metric-value">{{ item.value }}</div>
          <div class="metric-desc">{{ item.desc }}</div>
        </el-card>
      </el-col>
    </el-row>

    <div class="model-layout">
      <el-card class="diagram-card" ref="diagramRef">
        <template #header>
          <div class="card-title">ER 图总览</div>
        </template>
        <div class="diagram-canvas">
          <svg class="diagram-lines" viewBox="0 0 1200 760" preserveAspectRatio="none" aria-hidden="true">
            <line v-for="line in lines" :key="line.id" :x1="line.x1" :y1="line.y1" :x2="line.x2" :y2="line.y2" />
          </svg>

          <div
            v-for="node in nodes"
            :key="node.id"
            class="entity-node"
            :class="`entity-${node.variant}`"
            :style="{ left: node.left, top: node.top }"
          >
            <div class="entity-header">
              <span class="entity-type">{{ node.type }}</span>
              <span class="entity-name">{{ node.name }}</span>
            </div>
            <div class="entity-fields">
              <div v-for="field in node.fields" :key="field.name" class="field-row">
                <span class="field-name">{{ field.name }}</span>
                <span class="field-meta">{{ field.meta }}</span>
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <div class="side-stack">
        <el-card class="guide-card">
          <template #header>
            <div class="card-title">关系说明</div>
          </template>
          <el-timeline>
            <el-timeline-item v-for="item in relations" :key="item.title" :timestamp="item.tag" placement="top">
              <div class="relation-title">{{ item.title }}</div>
              <div class="relation-desc">{{ item.desc }}</div>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <el-card class="guide-card">
          <template #header>
            <div class="card-title">设计要点</div>
          </template>
          <ul class="bullet-list">
            <li>用户、角色、权限采用一对多 / 多对多关联，支撑统一登录与权限控制。</li>
            <li>学生、课程、排课、成绩、考勤、毕业审核共享主数据，避免重复维护。</li>
            <li>补修课程基于教学计划和已录入成绩动态计算，便于后续扩展规则。</li>
          </ul>
        </el-card>
      </div>
    </div>

    <el-card class="table-card" ref="tablesRef">
      <template #header>
        <div class="card-title">核心数据表</div>
      </template>
      <el-table :data="tableRows" stripe>
        <el-table-column prop="name" label="表名" width="160" />
        <el-table-column prop="purpose" label="用途" min-width="200" />
        <el-table-column prop="keys" label="主键 / 关键字段" min-width="240" />
        <el-table-column prop="relation" label="关联关系" min-width="240" />
      </el-table>
    </el-card>

    <el-card class="structure-card" ref="structureRef">
      <template #header>
        <div class="card-title">系统功能结构图</div>
      </template>
        <div class="structure-canvas">
          <svg class="diagram-lines structure-lines" viewBox="0 0 1200 600" preserveAspectRatio="none" aria-hidden="true">
            <line v-for="line in structureLines" :key="line.id" :x1="line.x1" :y1="line.y1" :x2="line.x2" :y2="line.y2" />
          </svg>

          <div
            v-for="node in structureNodes"
            :key="node.id"
            class="structure-node"
            :class="node.variant"
            :style="{ left: node.left, top: node.top, transform: node.transform || 'translateX(-50%)' }"
          >
          <div class="structure-node-title">{{ node.title }}</div>
          <div class="structure-node-desc">{{ node.desc }}</div>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="diagramPreviewVisible" fullscreen class="diagram-preview-dialog">
      <div class="diagram-preview">
        <div class="diagram-preview-header">
          <div>
            <div class="page-header-title">ER 图放大预览</div>
            <div class="page-header-desc">用于截图、汇报和说明书插图展示</div>
          </div>
          <el-button @click="diagramPreviewVisible = false">关闭</el-button>
        </div>
        <div class="diagram-canvas preview-canvas">
          <svg class="diagram-lines" viewBox="0 0 1200 760" preserveAspectRatio="none" aria-hidden="true">
            <line v-for="line in lines" :key="line.id" :x1="line.x1" :y1="line.y1" :x2="line.x2" :y2="line.y2" />
          </svg>
          <div
            v-for="node in nodes"
            :key="`preview-${node.id}`"
            class="entity-node"
            :class="`entity-${node.variant}`"
            :style="{ left: node.left, top: node.top }"
          >
            <div class="entity-header">
              <span class="entity-type">{{ node.type }}</span>
              <span class="entity-name">{{ node.name }}</span>
            </div>
            <div class="entity-fields">
              <div v-for="field in node.fields" :key="`preview-${node.id}-${field.name}`" class="field-row">
                <span class="field-name">{{ field.name }}</span>
                <span class="field-meta">{{ field.meta }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { nextTick, ref } from 'vue'

const diagramRef = ref(null)
const tablesRef = ref(null)
const structureRef = ref(null)
const diagramPreviewVisible = ref(false)

const metrics = [
  { label: '核心实体', value: 12, desc: '覆盖系统主要业务表' },
  { label: '关键关系', value: 16, desc: '体现主外键与业务联动' },
  { label: '业务域', value: 6, desc: '贯穿教务全流程' },
  { label: '示例流程', value: 5, desc: '登录、录入、统计、审核、补修' }
]

const nodes = [
  {
    id: 'user',
    name: 'User',
    type: '系统账号',
    variant: 'primary',
    left: '80px',
    top: '80px',
    fields: [
      { name: 'user_id', meta: 'PK' },
      { name: 'username', meta: '登录名' },
      { name: 'role_id', meta: 'FK -> Role' }
    ]
  },
  {
    id: 'role',
    name: 'Role',
    type: '权限角色',
    variant: 'primary',
    left: '360px',
    top: '60px',
    fields: [
      { name: 'role_id', meta: 'PK' },
      { name: 'name', meta: '角色名' }
    ]
  },
  {
    id: 'permission',
    name: 'Permission',
    type: '权限点',
    variant: 'primary',
    left: '660px',
    top: '60px',
    fields: [
      { name: 'permission_id', meta: 'PK' },
      { name: 'code', meta: '权限编码' }
    ]
  },
  {
    id: 'student',
    name: 'Student',
    type: '学生档案',
    variant: 'green',
    left: '80px',
    top: '250px',
    fields: [
      { name: 'student_id', meta: 'PK' },
      { name: 'major_id', meta: '专业' },
      { name: 'class_id', meta: '班级' }
    ]
  },
  {
    id: 'course',
    name: 'Course',
    type: '课程信息',
    variant: 'green',
    left: '360px',
    top: '250px',
    fields: [
      { name: 'course_id', meta: 'PK' },
      { name: 'credits', meta: '学分' },
      { name: 'nature', meta: '课程性质' }
    ]
  },
  {
    id: 'plan',
    name: 'TeachingPlan',
    type: '教学计划',
    variant: 'amber',
    left: '660px',
    top: '250px',
    fields: [
      { name: 'plan_id', meta: 'PK' },
      { name: 'major_id', meta: '专业' },
      { name: 'course_id', meta: 'FK -> Course' }
    ]
  },
  {
    id: 'schedule',
    name: 'Schedule',
    type: '排课安排',
    variant: 'amber',
    left: '960px',
    top: '250px',
    fields: [
      { name: 'schedule_id', meta: 'PK' },
      { name: 'semester_id', meta: '学期' },
      { name: 'teacher_id', meta: '教师' }
    ]
  },
  {
    id: 'grade',
    name: 'Grade',
    type: '成绩记录',
    variant: 'blue',
    left: '140px',
    top: '470px',
    fields: [
      { name: 'grade_id', meta: 'PK' },
      { name: 'student_id', meta: 'FK -> Student' },
      { name: 'course_id', meta: 'FK -> Course' }
    ]
  },
  {
    id: 'attendance',
    name: 'Attendance',
    type: '考勤记录',
    variant: 'blue',
    left: '460px',
    top: '470px',
    fields: [
      { name: 'attendance_id', meta: 'PK' },
      { name: 'student_id', meta: 'FK -> Student' },
      { name: 'course_id', meta: 'FK -> Course' }
    ]
  },
  {
    id: 'graduation',
    name: 'GraduationAudit',
    type: '毕业审核',
    variant: 'purple',
    left: '780px',
    top: '470px',
    fields: [
      { name: 'audit_id', meta: 'PK' },
      { name: 'student_id', meta: 'FK -> Student' },
      { name: 'status', meta: '审核状态' }
    ]
  }
]

const lines = [
  { id: 'u-r', x1: 250, y1: 150, x2: 360, y2: 130 },
  { id: 'r-p', x1: 540, y1: 130, x2: 660, y2: 130 },
  { id: 's-g', x1: 250, y1: 320, x2: 300, y2: 470 },
  { id: 'c-g', x1: 520, y1: 320, x2: 380, y2: 470 },
  { id: 'c-p', x1: 520, y1: 320, x2: 660, y2: 320 },
  { id: 'p-s', x1: 900, y1: 320, x2: 960, y2: 320 },
  { id: 's-a', x1: 250, y1: 320, x2: 620, y2: 530 },
  { id: 's-gr', x1: 250, y1: 320, x2: 780, y2: 530 },
  { id: 'c-a', x1: 520, y1: 320, x2: 620, y2: 530 },
  { id: 'c-gr', x1: 520, y1: 320, x2: 780, y2: 530 }
]

const relations = [
  { tag: '1:N', title: 'User -> Role', desc: '一个账号隶属一个角色，用于登录后权限控制。' },
  { tag: 'M:N', title: 'Role -> Permission', desc: '角色与权限点多对多，支持细粒度授权。' },
  { tag: '1:N', title: 'Student -> Grade / Attendance', desc: '一个学生对应多条成绩与考勤记录。' },
  { tag: '1:N', title: 'Course -> Schedule / Grade', desc: '课程关联排课与成绩，支撑教学执行。' },
  { tag: '1:N', title: 'Student -> GraduationAudit', desc: '毕业审核按学生维度汇总学分、绩点和补修信息。' }
]

const tableRows = [
  { name: 'user', purpose: '系统登录与账号管理', keys: 'user_id, role_id', relation: '关联 role、student、teacher' },
  { name: 'role', purpose: '角色定义', keys: 'role_id', relation: '关联 permission' },
  { name: 'permission', purpose: '权限点配置', keys: 'permission_id', relation: '角色权限中间表' },
  { name: 'student', purpose: '学生基础信息', keys: 'student_id', relation: '关联 grade、attendance、graduation_audit' },
  { name: 'course', purpose: '课程主数据', keys: 'course_id', relation: '关联 teaching_plan、schedule、grade' },
  { name: 'teaching_plan', purpose: '专业教学计划', keys: 'plan_id', relation: '关联 major、course' },
  { name: 'schedule', purpose: '排课安排', keys: 'schedule_id', relation: '关联 semester、course、teacher、classroom' },
  { name: 'grade', purpose: '成绩录入与审核', keys: 'grade_id', relation: '关联 student、course、schedule' },
  { name: 'attendance', purpose: '学生考勤记录', keys: 'attendance_id', relation: '关联 student、course、semester' },
  { name: 'graduation_audit', purpose: '毕业资格审核', keys: 'audit_id', relation: '关联 student、grade、teaching_plan' }
]

const structureNodes = [
  { id: 'auth', title: '登录认证', desc: '统一身份校验与权限控制', kind: 'root', left: '600px', top: '50px' },
  { id: 'sys', title: '系统管理', desc: '用户、角色、权限', kind: 'module', left: '90px', top: '210px' },
  { id: 'stu', title: '学生管理', desc: '学生信息与注册', kind: 'module', left: '240px', top: '210px' },
  { id: 'tea', title: '教师管理', desc: '教师信息维护', kind: 'module', left: '390px', top: '210px' },
  { id: 'course', title: '课程管理', desc: '课程与教学计划', kind: 'module', left: '540px', top: '210px' },
  { id: 'schedule', title: '排课管理', desc: '排课安排与查询', kind: 'module', left: '690px', top: '210px' },
  { id: 'grade', title: '成绩管理', desc: '录入、查询、统计', kind: 'module', left: '840px', top: '210px' },
  { id: 'attendance', title: '考勤管理', desc: '登记、查询、统计', kind: 'module', left: '990px', top: '210px' },
  { id: 'graduation', title: '毕业管理', desc: '审核、补修、授予学位', kind: 'module', left: '1140px', top: '210px' },
  { id: 'grade-input', title: '成绩录入', desc: '按课程/学期录入', kind: 'leaf', left: '610px', top: '410px' },
  { id: 'grade-query', title: '成绩查询', desc: '按学生/课程筛选', kind: 'leaf', left: '730px', top: '410px' },
  { id: 'grade-stat', title: '成绩统计', desc: '及格率、分布统计', kind: 'leaf', left: '850px', top: '410px' },
  { id: 'att-record', title: '考勤登记', desc: '按学号和课程登记', kind: 'leaf', left: '930px', top: '410px' },
  { id: 'att-query', title: '考勤查询', desc: '按日期与课程查询', kind: 'leaf', left: '1050px', top: '410px' },
  { id: 'att-stat', title: '考勤统计', desc: '出勤、缺勤汇总', kind: 'leaf', left: '1170px', top: '410px' },
  { id: 'grad-audit', title: '毕业审核', desc: '学分、绩点、必修课', kind: 'leaf', left: '1210px', top: '410px' },
  { id: 'grad-remedial', title: '补修课程', desc: '自动生成补修列表', kind: 'leaf', left: '1330px', top: '410px' },
  { id: 'grad-degree', title: '学位授予', desc: '审核通过后授予', kind: 'leaf', left: '1450px', top: '410px' }
]

const structureLines = [
  { id: 'auth-sys', x1: 600, y1: 130, x2: 90, y2: 210 },
  { id: 'auth-stu', x1: 600, y1: 130, x2: 240, y2: 210 },
  { id: 'auth-tea', x1: 600, y1: 130, x2: 390, y2: 210 },
  { id: 'auth-course', x1: 600, y1: 130, x2: 540, y2: 210 },
  { id: 'auth-schedule', x1: 600, y1: 130, x2: 690, y2: 210 },
  { id: 'auth-grade', x1: 600, y1: 130, x2: 840, y2: 210 },
  { id: 'auth-attendance', x1: 600, y1: 130, x2: 990, y2: 210 },
  { id: 'auth-graduation', x1: 600, y1: 130, x2: 1140, y2: 210 },
  { id: 'grade-input', x1: 840, y1: 282, x2: 610, y2: 410 },
  { id: 'grade-query', x1: 840, y1: 282, x2: 730, y2: 410 },
  { id: 'grade-stat', x1: 840, y1: 282, x2: 850, y2: 410 },
  { id: 'att-record', x1: 990, y1: 282, x2: 930, y2: 410 },
  { id: 'att-query', x1: 990, y1: 282, x2: 1050, y2: 410 },
  { id: 'att-stat', x1: 990, y1: 282, x2: 1170, y2: 410 },
  { id: 'grad-audit', x1: 1140, y1: 282, x2: 1210, y2: 410 },
  { id: 'grad-remedial', x1: 1140, y1: 282, x2: 1330, y2: 410 },
  { id: 'grad-degree', x1: 1140, y1: 282, x2: 1450, y2: 410 }
]

// 处理scrolltodiagram
const scrollToDiagram = async () => {
  await nextTick()
  diagramRef.value?.$el?.scrollIntoView?.({ behavior: 'smooth', block: 'start' })
}

// 处理scrolltotables
const scrollToTables = async () => {
  await nextTick()
  tablesRef.value?.$el?.scrollIntoView?.({ behavior: 'smooth', block: 'start' })
}

// 处理scrolltostructure
const scrollToStructure = async () => {
  await nextTick()
  structureRef.value?.$el?.scrollIntoView?.({ behavior: 'smooth', block: 'start' })
}
</script>

<style scoped>
.model-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.metric-row {
  margin-bottom: 2px;
}

.metric-card {
  min-height: 122px;
  background:
    radial-gradient(circle at top right, rgba(67, 97, 238, 0.14), transparent 40%),
    linear-gradient(180deg, #fff, #fbfcff);
}

.metric-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.metric-value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
}

.metric-desc {
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-secondary);
}

.model-layout {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
  align-items: start;
}

.diagram-card,
.guide-card,
.table-card {
  overflow: hidden;
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

.diagram-canvas {
  position: relative;
  min-height: 1020px;
  background:
    linear-gradient(180deg, rgba(67, 97, 238, 0.05), rgba(46, 196, 182, 0.04)),
    radial-gradient(circle at top left, rgba(255, 159, 28, 0.08), transparent 30%),
    #fdfdff;
  border-radius: 14px;
  overflow: hidden;
}

.preview-canvas {
  --preview-scale: 0.7;
  min-height: calc(78vh / var(--preview-scale));
  height: calc(78vh / var(--preview-scale));
  width: calc(100% / var(--preview-scale));
  transform: scale(var(--preview-scale));
  transform-origin: top left;
}

.diagram-lines {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.diagram-lines line {
  stroke: rgba(67, 97, 238, 0.26);
  stroke-width: 3;
  stroke-linecap: round;
  stroke-dasharray: 8 8;
}

.entity-node {
  position: absolute;
  width: 230px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 14px 32px rgba(26, 26, 46, 0.10);
  border: 1px solid rgba(226, 232, 240, 0.92);
  backdrop-filter: blur(6px);
}

.entity-header {
  padding: 14px 16px 10px;
  border-bottom: 1px solid rgba(232, 236, 241, 0.9);
}

.entity-type {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  color: #fff;
  background: var(--primary-color);
}

.entity-name {
  display: block;
  margin-top: 8px;
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary);
}

.entity-fields {
  padding: 10px 14px 14px;
}

.field-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 7px 0;
  font-size: 12px;
  color: var(--text-regular);
}

.field-row + .field-row {
  border-top: 1px dashed rgba(232, 236, 241, 0.8);
}

.field-name {
  font-family: Consolas, 'SFMono-Regular', Monaco, monospace;
}

.field-meta {
  color: var(--text-secondary);
  white-space: nowrap;
}

.entity-primary .entity-type {
  background: linear-gradient(135deg, #4361ee, #6b83f2);
}

.entity-green .entity-type {
  background: linear-gradient(135deg, #2ec4b6, #58d4c7);
}

.entity-amber .entity-type {
  background: linear-gradient(135deg, #ff9f1c, #ffbf69);
}

.entity-blue .entity-type {
  background: linear-gradient(135deg, #3a86ff, #6aa4ff);
}

.entity-purple .entity-type {
  background: linear-gradient(135deg, #8a5cf6, #a98ef8);
}

.side-stack {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.relation-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.relation-desc {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-secondary);
}

.bullet-list {
  padding-left: 18px;
  color: var(--text-regular);
}

.bullet-list li + li {
  margin-top: 10px;
}

.table-card {
  margin-top: 2px;
}

.structure-card {
  margin-top: 2px;
}

.structure-canvas {
  position: relative;
  min-height: 560px;
  background:
    linear-gradient(180deg, rgba(255, 159, 28, 0.06), rgba(67, 97, 238, 0.04)),
    radial-gradient(circle at top right, rgba(46, 196, 182, 0.08), transparent 30%),
    #fdfdff;
  border-radius: 14px;
  overflow: hidden;
}

.structure-lines line {
  stroke: rgba(67, 97, 238, 0.28);
  stroke-width: 2.5;
  stroke-linecap: round;
}

.structure-node {
  position: absolute;
  width: 108px;
  height: 72px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid rgba(224, 230, 240, 0.95);
  box-shadow: 0 10px 24px rgba(26, 26, 46, 0.08);
  text-align: center;
}

.structure-node.root {
  width: 130px;
  height: 80px;
  border-color: rgba(67, 97, 238, 0.22);
  background: linear-gradient(180deg, #ffffff, #f4f7ff);
}

.structure-node.module {
  background: linear-gradient(180deg, #ffffff, #fbfcff);
}

.structure-node.leaf {
  width: 102px;
  height: 66px;
  background: #fff;
}

.structure-node-title {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.3;
}

.structure-node-desc {
  margin-top: 4px;
  font-size: 11px;
  line-height: 1.35;
  color: var(--text-secondary);
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.diagram-preview-dialog :deep(.el-dialog__body) {
  padding: 0 !important;
  height: 100vh;
}

.diagram-preview {
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 20px;
  gap: 16px;
  background: linear-gradient(180deg, rgba(67, 97, 238, 0.04), rgba(46, 196, 182, 0.03));
}

.diagram-preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

@media (max-width: 1200px) {
  .diagram-canvas {
    min-height: 860px;
  }

  .side-stack {
    grid-template-columns: 1fr;
  }

  .structure-canvas {
    min-height: 640px;
  }
}

@media (max-width: 900px) {
  .entity-node {
    width: 180px;
  }

  .diagram-canvas {
    min-height: 980px;
  }

  .side-stack {
    grid-template-columns: 1fr;
  }

  .structure-node {
    width: 92px;
    height: 66px;
  }

  .structure-node.root {
    width: 116px;
    height: 74px;
  }

  .structure-node.leaf {
    width: 92px;
    height: 60px;
  }
}
</style>
