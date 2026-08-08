# EducationAgentController 接口契约

## 通用约定

- 基础地址：`http://localhost:8080/api`；下列路径均为包含上下文路径的完整路径。
- 全部 16 个接口都要求 `Authorization: Bearer <JWT>`，且仅角色 `1`（管理员）可访问。
- 响应统一为 `{ "code": number, "message": string, "data": any|null }`。
- 时间字段使用 ISO-8601，例如 `2026-07-28T18:30:00`；百分比字段为 `0-100` 数值。
- 通用失败：`{"code":401,"message":"未登录或登录已过期","data":null}`；权限不足时 HTTP 403；未捕获异常为 `{"code":500,"message":"系统内部错误，请联系管理员","data":null}`。

## 1. 学业预警学生列表

- 方法与路径：`GET /api/agent/warnings/students`
- JWT：必需，管理员。
- 路径参数：无。
- 查询参数：`semesterId`、`departmentId`、`majorId`、`classId`、`keyword`、`level`（`high|medium|low`）均可选；`page` 默认 `1`；`pageSize` 默认 `10`。
- 请求体：无。
- `data`：MyBatis-Plus 分页对象，字段 `records,total,size,current,pages`；`records[]` 含 `studentId,studentName,departmentId,majorId,majorName,classId,className,riskLevel,riskScore,riskReason,triggeredRules,triggeredRuleCount`。
- 成功示例：`{"code":200,"message":"操作成功","data":{"records":[{"studentId":"S004","riskLevel":"high","riskScore":80,"triggeredRules":["failed_course"],"triggeredRuleCount":1}],"total":1,"size":10,"current":1,"pages":1}}`
- 常见失败：分页或 `level` 非法时 `{"code":400,"message":"参数不合法","data":null}`；另见通用 401/403/500。

## 2. 单个学生预警详情

- 方法与路径：`GET /api/agent/warnings/students/{studentId}`
- JWT：必需，管理员。
- 路径参数：`studentId` 必填，学生内部 ID。
- 查询参数：`semesterId` 可选。
- 请求体：无。
- `data`：`studentId,studentName,majorName,className,semesterId,riskLevel,riskScore,failedCourseCount,absentCount,lateCount,graduationAuditStatus,triggeredRules,riskReason,interventionSuggestion`。
- 成功示例：`{"code":200,"message":"操作成功","data":{"studentId":"S004","riskLevel":"high","riskScore":80,"failedCourseCount":2,"absentCount":3,"triggeredRules":["failed_course"]}}`
- 常见失败：学生不存在时 `{"code":500,"message":"学生不存在","data":null}`；另见通用 401/403/500。

## 3. 学生成绩趋势

- 方法与路径：`GET /api/agent/warnings/students/{studentId}/grade-trend`
- JWT：必需，管理员。
- 路径参数：`studentId` 必填。
- 查询参数、请求体：无。
- `data`：`studentId,studentName,semesters,trendDirection,trendConclusion,riskChange,dataNotice`；`semesters[]` 含 `semesterId,averageScore,failedCourseCount,courseCount`。
- 成功示例：`{"code":200,"message":"操作成功","data":{"studentId":"S003","semesters":[{"semesterId":"SEM002","averageScore":78.5,"failedCourseCount":1,"courseCount":6}],"trendDirection":"down","riskChange":"风险上升"}}`
- 常见失败：学生不存在时业务失败；另见通用 401/403/500。

## 4. 保存单个学生预警快照

- 方法与路径：`POST /api/agent/warnings/students/{studentId}/records`
- JWT：必需，管理员。
- 路径参数：`studentId` 必填。
- 查询参数：`semesterId` 可选。
- 请求体：无。
- `data`：`recordId,studentId,studentName,semesterId,riskLevel,riskScore,riskReason,triggeredRules,calculatedAt,processStatus,processOpinion,processedBy,processedAt,createdAt,updatedAt`。
- 成功示例：`{"code":200,"message":"学业预警记录已保存","data":{"recordId":"WR001","studentId":"S004","riskLevel":"high","processStatus":"pending"}}`
- 常见失败：学生不存在或快照保存失败时业务错误；另见通用 401/403/500。

## 5. 学生预警历史

- 方法与路径：`GET /api/agent/warnings/students/{studentId}/records`
- JWT：必需，管理员。
- 路径参数：`studentId` 必填。
- 查询参数：`semesterId`、`processStatus`、`riskLevel` 可选；`page` 默认 `1`；`pageSize` 默认 `10`。
- 请求体：无。
- `data`：分页对象 `records,total,size,current,pages`；记录字段同“保存单个学生预警快照”。无历史时 `records=[]`。
- 成功示例：`{"code":200,"message":"操作成功","data":{"records":[{"recordId":"WR001","processStatus":"processing"}],"total":1,"size":10,"current":1,"pages":1}}`
- 常见失败：分页或状态筛选非法时业务错误；另见通用 401/403/500。

## 6. 更新预警处理状态

- 方法与路径：`PUT /api/agent/warnings/records/{recordId}/process`
- JWT：必需，管理员；处理人取当前 JWT 的用户 ID，客户端不能伪造。
- 路径参数：`recordId` 必填。
- 查询参数：无。
- 请求体：`processStatus` 必填（`pending|processing|completed`），`processOpinion` 在 `completed` 时必填。
- `data`：更新后的完整预警记录，字段同“保存单个学生预警快照”。
- 成功示例：`{"code":200,"message":"学业预警处理状态已更新","data":{"recordId":"WR001","processStatus":"completed","processOpinion":"已完成辅导","processedBy":"U001"}}`
- 常见失败：`{"code":400,"message":"处理状态不合法","data":null}`；记录不存在时业务错误；另见通用 401/403/500。

## 7. 批量保存预警快照

- 方法与路径：`POST /api/agent/warnings/records/snapshot`
- JWT：必需，管理员。
- 路径参数、查询参数：无。
- 请求体：可省略；字段 `semesterId,departmentId,majorId,classId,keyword,level` 均可选。
- `data`：预警记录数组，元素字段同“保存单个学生预警快照”。
- 成功示例：`{"code":200,"message":"学业预警快照已保存","data":[{"recordId":"WR001","studentId":"S004","riskLevel":"high"}]}`
- 常见失败：筛选值非法或批量持久化失败时业务错误；另见通用 401/403/500。

## 8. Agent 预览

- 方法与路径：`GET /api/agent/preview`
- JWT：必需，管理员。
- 路径参数、请求体：无。
- 查询参数：`semesterId,departmentId,majorId,classId` 均可选。
- `data`：`agentName,status,summary,filters,dataOverview,gradeAnalysis,attendanceAnalysis,courseLoadAnalysis,graduationAnalysis,insights,modules,nextApis`。`filters` 含四个查询筛选；`dataOverview` 为概览指标并增加 `riskSignal{level,message}`；三类分析字段见下列指标接口；`graduationAnalysis` 含 `totalStudents,auditedCount,approvedCount,rejectedCount,degreeGrantedCount,pendingDegreeCount,approvalRate,degreeRate`；`insights` 为字符串数组；`modules[]` 含 `code,name,description`；`nextApis` 为后续接口路径数组。
- 成功示例：`{"code":200,"message":"操作成功","data":{"agentName":"智能教务数据分析 Agent","status":"ready","filters":{"semesterId":"SEM002"},"insights":[]}}`
- 常见失败：筛选 ID 不存在或数据聚合失败时业务错误；另见通用 401/403/500。

## 9. Agent 综合分析

- 方法与路径：`GET /api/agent/analysis`
- JWT：必需，管理员。
- 路径参数、请求体：无。
- 查询参数：`semesterId,departmentId,majorId,classId` 均可选。
- `data`：与 `/api/agent/preview` 完全相同，包含其列出的全部顶层和子字段。
- 成功示例：`{"code":200,"message":"操作成功","data":{"status":"ready","dataOverview":{"studentCount":120},"gradeAnalysis":{"metricCode":"grade"}}}`
- 常见失败：筛选 ID 不存在或数据聚合失败时业务错误；另见通用 401/403/500。

## 10. 专业维度汇总

- 方法与路径：`GET /api/agent/analysis/major-summary`
- JWT：必需，管理员。
- 路径参数、请求体：无。
- 查询参数：`semesterId,departmentId,majorId,classId` 均可选。
- `data`：数组；元素含 `majorId,majorName,studentCount,activeStudentCount,warningStudentCount,warningRate`。
- 成功示例：`{"code":200,"message":"操作成功","data":[{"majorId":"M001","majorName":"计算机科学","studentCount":60,"activeStudentCount":58,"warningStudentCount":4,"warningRate":6.67}]}`
- 常见失败：筛选 ID 不存在或汇总失败时业务错误；另见通用 401/403/500。

## 11. 教务概览指标

- 方法与路径：`GET /api/agent/metrics/overview`
- JWT：必需，管理员。
- 路径参数、请求体：无。
- 查询参数：`semesterId,departmentId,majorId,classId` 均可选。
- `data`：`studentCount,activeStudentCount,gradeRecordCount,failedCourseCount,gradePassRate,attendanceRecordCount,abnormalAttendanceCount,graduationAuditCount,approvedGraduationCount,rejectedGraduationCount,graduationApprovalRate`。
- 成功示例：`{"code":200,"message":"操作成功","data":{"studentCount":120,"activeStudentCount":118,"gradeRecordCount":600,"failedCourseCount":12,"gradePassRate":98.0}}`
- 常见失败：筛选 ID 不存在或统计失败时业务错误；另见通用 401/403/500。

## 12. 成绩指标

- 方法与路径：`GET /api/agent/metrics/grade`
- JWT：必需，管理员。
- 路径参数、请求体：无。
- 查询参数：`semesterId,departmentId,majorId,classId` 均可选。
- `data`：`metricCode,total,averageScore,average,maxScore,minScore,passCount,failedCount,passRate,excellentRate,distribution,courseAverageList,courseAvgList,explanation`；课程均分元素含 `courseId,courseName,averageScore,name,avg,studentCount`。
- 成功示例：`{"code":200,"message":"操作成功","data":{"metricCode":"grade","total":600,"averageScore":81.2,"passCount":588,"failedCount":12,"passRate":98.0}}`
- 常见失败：筛选 ID 不存在或成绩聚合失败时业务错误；另见通用 401/403/500。

## 13. 考勤指标

- 方法与路径：`GET /api/agent/metrics/attendance`
- JWT：必需，管理员。
- 路径参数、请求体：无。
- 查询参数：`semesterId,departmentId,majorId,classId` 均可选。
- `data`：`metricCode,total,normalCount,abnormalCount,abnormalStudentCount,abnormalRate,statusCounts,explanation`；`statusCounts` 按考勤状态返回计数。
- 成功示例：`{"code":200,"message":"操作成功","data":{"metricCode":"attendance","total":500,"normalCount":470,"abnormalCount":30,"abnormalStudentCount":18,"abnormalRate":6.0}}`
- 常见失败：筛选 ID 不存在或考勤聚合失败时业务错误；另见通用 401/403/500。

## 14. 课程负载指标

- 方法与路径：`GET /api/agent/metrics/course`
- JWT：必需，管理员。
- 路径参数、请求体：无。
- 查询参数：`semesterId,departmentId,majorId,classId` 均可选。
- `data`：`metricCode,scheduleCount,courseCount,selectedCount,totalCapacity,loadRate,courseLoads,explanation`；`courseLoads[]` 含 `scheduleId,courseId,courseName,teacherId,classId,capacity,selectedCount,loadRate`。
- 成功示例：`{"code":200,"message":"操作成功","data":{"metricCode":"course_load","scheduleCount":20,"courseCount":12,"selectedCount":400,"totalCapacity":600,"loadRate":66.67}}`
- 常见失败：筛选 ID 不存在或负载聚合失败时业务错误；另见通用 401/403/500。

## 15. Agent 问答

- 方法与路径：`POST /api/agent/chat`
- JWT：必需，管理员。
- 路径参数、查询参数：无。
- 请求体：`question` 必填；`semesterId,departmentId,majorId,classId,studentId,compareStudentId` 可选。接口只调用固定指标服务，不执行自由 SQL。
- `data`：`intentCode,intentName,answer,intentSource,answerSource,metricsOverview,insights,detailRows`；`metricsOverview` 字段同“教务概览指标”。
- 成功示例：`{"code":200,"message":"操作成功","data":{"intentCode":"academic_risk","intentName":"学业风险","answer":"当前有4名高风险学生。","intentSource":"keyword","answerSource":"keyword","insights":[],"detailRows":[]}}`
- 常见失败：`{"code":400,"message":"question不能为空","data":null}`；大模型不可用时自动返回 `answerSource=keyword`，不视为接口失败；另见通用 401/403/500。

## 16. Agent 能力声明

- 方法与路径：`GET /api/agent/capabilities`
- JWT：必需，管理员。
- 路径参数、查询参数、请求体：无。
- `data`：`mode,llmProvider,supportsTextToSql,fallback,fixedServices`；`fixedServices` 是能力代码到固定 Service 方法的映射。
- 成功示例：`{"code":200,"message":"操作成功","data":{"mode":"hybrid_rule_based","llmProvider":"deepseek","supportsTextToSql":false,"fallback":"keyword_matching","fixedServices":{"academic_risk":"EducationAgentService#getAcademicRiskAnalysis"}}}`
- 常见失败：仅通用 401/403/500。

## 验收清单（2026-07-28）

- [x] Controller 的 16 个接口均已记录方法、完整路径、JWT、参数、请求体、`data` 字段、成功和失败响应。
- [x] Agent 只调用固定 Service 指标，不提供 Text2SQL。
- [x] 预警列表、详情、趋势、历史、快照、处理状态、综合分析、指标和问答均有真实数据链路。
- [x] 前端 Agent 调用路径与 Controller 映射一致。
- [x] 已在 MySQL 8.0.15 隔离验收库验证登录、预警、Agent 回退、自动排课和批量考勤。

---

## 历史草稿（已由以上接口契约取代）

# 认证与学业预警接口

统一响应：`{ "code": 200, "message": "success", "data": ... }`；失败示例：`{ "code": 401, "message": "未登录或令牌无效", "data": null }`，或 `{"code":500,"message":"学生不存在","data":null}`。除登录外，以下接口均要求 `Authorization: Bearer <JWT>`，且当前系统沿用既有管理员访问控制，不新增角色或权限体系。

## 登录

`POST /api/login`；请求体：`{"username":"admin","password":"***"}`；无需 JWT。成功 `data` 至少含 `token`、`user`（`userId`、`username`、`name`、`roleId`）；错误密码返回 `code` 非 200 与错误消息。

## 预警列表

`GET /api/agent/warnings/students`；查询参数：可选 `semesterId`、`departmentId`、`majorId`、`classId`、`keyword`、`level`（`high|medium|low`），`page=1`、`pageSize=10`。成功 `data` 为分页对象：`records`、`total`、`current`、`size`；每个记录含 `studentId`、`studentName`、`departmentId`、`majorId`、`majorName`、`classId`、`className`、`riskLevel`、`riskScore`、`riskReason`、`triggeredRules`、`triggeredRuleCount`。示例：`{"code":200,"message":"success","data":{"records":[{"studentId":"S004","riskLevel":"high"}],"total":1}}`。

## 学生详情与趋势

`GET /api/agent/warnings/students/{studentId}`；路径参数 `studentId` 必填，查询 `semesterId` 可选。成功 `data`：`studentId`、`studentName`、`majorName`、`className`、`semesterId`、`riskLevel`、`riskScore`、`failedCourseCount`、`absentCount`、`lateCount`、`graduationAuditStatus`、`triggeredRules`、`riskReason`、`interventionSuggestion`。

`GET /api/agent/warnings/students/{studentId}/grade-trend`；路径参数必填，无请求体。成功 `data`：`studentId`、`studentName`、`semesterTrends`（每项含学期、平均分、及格率、课程数）、`trendConclusion`、`riskChange`。示例：`{"code":200,"message":"success","data":{"studentId":"S003","riskChange":"风险上升"}}`。

## 预警历史

`POST /api/agent/warnings/students/{studentId}/records`；路径 `studentId` 必填，查询 `semesterId` 可选，无请求体。成功 `data`：`recordId`、`studentId`、`semesterId`、`riskLevel`、`riskScore`、`riskReason`、`triggeredRules`、`calculatedAt`、`processStatus`、`processOpinion`、`processedBy`、`processedAt`。

`GET /api/agent/warnings/students/{studentId}/records`；路径 `studentId` 必填；可选查询 `semesterId`、`processStatus`、`riskLevel`、`page=1`、`pageSize=10`。成功分页 `data.records` 的字段同上；空历史返回 `records: []`。示例：`{"code":200,"message":"success","data":{"records":[{"recordId":"WR001","processStatus":"processing"}],"total":1}}`。

`PUT /api/agent/warnings/records/{recordId}/process`；路径 `recordId` 必填；请求体 `{"processStatus":"processing","processOpinion":"已约谈"}`。成功 `data` 为更新后的历史记录字段；无效状态或记录不存在返回业务失败。

`POST /api/agent/warnings/records/snapshot`；可为空请求体，完整筛选体：`{"semesterId":"SEM002","departmentId":"D001","majorId":"M001","classId":"C001","keyword":"","level":"high"}`。成功 `data` 为保存的历史记录数组，元素字段同上。

## Agent 对话与能力

`POST /api/agent/chat`；请求体 `question` 必填，可选 `semesterId`、`departmentId`、`majorId`、`classId`、`studentId`、`compareStudentId`。成功 `data`：`intentCode`、`intentName`、`answer`、`intentSource`、`answerSource`、`metricsOverview`、`insights`、`detailRows`。示例：`{"code":200,"message":"success","data":{"intentCode":"academic_risk","intentSource":"keyword","answerSource":"keyword"}}`；空白、SQL 或无关问题返回受限说明，不执行自由查询。

`GET /api/agent/capabilities`；无参数、无请求体。成功 `data`：`mode`、`llmProvider`、`supportsTextToSql`、`fallback`、`fixedServices`；示例：`{"code":200,"message":"success","data":{"supportsTextToSql":false,"fallback":"keyword_matching"}}`。

## Postman 流程

1. 调用登录，复制 `data.token`，在 Collection 的 Authorization 设置 Bearer Token。
2. 依次调用预警列表、`S004` 详情、`S003` 趋势、`S003` 历史；调用快照后再次查询历史。
3. 用 `PUT` 更新一个 `recordId` 的处理状态，再验证 `processStatus` 筛选。
4. 调用 Agent 对话；分别验证有效问题、401（移除 Token）、错误学生 ID、无历史学生、空数据范围与 DeepSeek 不可用时的 `keyword` 回退。
