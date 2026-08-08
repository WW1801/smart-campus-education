# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

校园管理员（已确认）在日常教务管理中维护用户、学生、教师、课程、排课、成绩、考勤、毕业审核和学业预警；教师与学生使用其各自授权的功能。

## Product Purpose

校园教务系统集中承载校内人员、教学和学籍相关的管理与查询工作。管理员需要快速发现待办与异常，并进入对应业务模块完成处理。

## Positioning

同一个基于角色授权的工作台串联教务运营数据、日常处理入口和 AI 学业分析能力。

## Operating Context

管理员通过浏览器在办公场景使用系统；当前前端为 Vue 3、Element Plus 和 Vuex，后端 API 上下文路径为 `/api`。

## Capabilities and Constraints

- 已确认角色：管理员、教务处、辅导员、教师、学生。
- 管理员可访问系统、学生、教师、课程、排课、成绩、考勤、毕业、AI 数据分析与学业预警模块。
- 当前原型只呈现 UI 结构与示例数据，不连接接口，不替代既有业务功能。

## Evidence on Hand

- 现有页面与路由：`frontend/router/index.js`、`frontend/views/`。
- 演示数据为原型作者编写的非真实数据，不能作为业务指标或事实声明。

## Product Principles

- 先暴露需要管理员处理的事项，再展示概览数据。
- 让每项指标和提醒都能自然通往对应业务动作。
- 角色权限决定可见功能，避免跨角色的信息干扰。
- 将 AI 分析定位为辅助判断，而非自动业务决策。
