# 测试与验收

后端：`cd backend; mvn test`，然后 `mvn package -DskipTests`。前端：`cd frontend; npm run build`。静态检查：`git diff --check`。

密钥扫描使用 `rg -l "sk-[A-Za-z0-9]{20,}|DEEPSEEK_API_KEY\s*:\s*sk-" .`，只根据是否有文件名输出判断；不得打印可能命中的内容。Windows 上若 `npm run build` 报 `EPERM`，先关闭占用 `frontend/node_modules/.vite` 或构建输出的进程后重试，不删除项目文件。

Postman 验收、接口参数和预期响应见 [接口测试流程](../api/agent-api.md)。
