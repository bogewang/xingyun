# Task 5 前端实现报告

## 实现内容

- 商品列表默认使用 `available=true` 查询。
- 增加“启用 / 禁用 / 全部”状态筛选，其中“全部”下发空字符串。
- 增加 `AvailableTag` 状态列。
- 增加批量启用、批量禁用确认窗口和菜单命令。
- 批量状态更新通过 `buildProductAvailabilityRequest` 构建参数，并由 `updateAvailable` 发起单次请求；成功后刷新列表并清除选择。
- 保留原有批量删除及单行删除功能。
- `QueryProductVo.available` 调整为 `boolean | ''`，`QueryProductBo.available` 保持为 `boolean`。
- 补充 QueryProductVo/Bo 状态请求契约测试。

## 验证

- `pnpm exec vitest run src/views/base-data/product/info/__tests__/productAvailability.test.ts`：通过，6 tests。
- 目标文件 ESLint：通过。
- `pnpm run lint`：未通过，失败来自现有 `frontend/internal/*/dist/*.d.ts` 格式问题，与本次文件无关。
- `pnpm run type:check`：未通过，`vue-tsc@1.8.27` 与当前 TypeScript 触发既有工具异常：`Search string not found: "for (const existingRoot of buildInfoVersionMap.roots) {"`。
- 直接 `pnpm exec tsc --noEmit --skipLibCheck`：未通过，存在多处既有全仓类型错误；本次修改文件未出现在错误列表中。
- `git diff --check`：通过。

## 风险

仓库现有 lint/typecheck 基线不通过，无法据此声明全仓检查为绿色；本次范围内的聚焦测试、目标 ESLint 和 diff 空白检查均已通过。
