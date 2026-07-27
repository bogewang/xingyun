# Task 4：单客户明细迁移与回归

## 状态

已实现并提交：`330d602d feat: move customer settlement actions to detail page`。

## 变更

- 新增 `frontend/src/views/customer-settle/sheet/detail.vue`：固定路由 `customerId` 的单客户明细，保留混合销售出库/退货查询、导出、确认对账、确认结算、负数金额校验及关联单据跳转。
- 路由缺少客户参数时显示 `createError('客户参数不能为空！')`，且不发起接口请求。
- 所有明细查询、导出、对账和结算提交均使用路由客户 ID；页面筛选值不能覆盖它。
- 新增 `validateCustomerDetailRoute`、`buildCustomerDetailQuery`，并扩展直接结算 payload 断言，覆盖 `unSettleAmount` 与 `checkAmount`。

## TDD 与验证

- 红：`pnpm test:unit src/views/customer-settle/sheet/customerSettleWorkbench.test.ts`，因 `buildCustomerDetailQuery is not a function` 失败。
- 绿：同一测试 9/9 通过。
- `pnpm test:unit src/router/routes/customerSettleRoute.test.ts src/views/customer-settle/sheet/customerSettleWorkbench.test.ts`：11/11 通过。
- `pnpm exec eslint --max-warnings 0 ...detail.vue ...customerSettleWorkbench.ts ...customerSettleWorkbench.test.ts`：通过。
- `pnpm build`：通过。
- `mvn -pl xingyun-settle '-Dtest=CustomerSettleSheetOverviewServiceImplTest,CustomerSettleSheetControllerTest,CustomerSettleSheetServiceImplTest' test`：40 项、0 失败、0 错误。
- `git diff --check`：通过（无输出，存在其他工作区文件的 CRLF 警告）。

## 已知环境问题

`pnpm type:check` 未完成：`vue-tsc` 在 Node 20.20.0 下启动即报 `Search string not found: "for (const existingRoot of buildInfoVersionMap.roots) {"`，未输出项目类型诊断。生产 Vite 构建已通过，作为组件编译验证。

## 协作边界

详情页引用共享工作区中已有、但未由本任务提交的 `frontend/src/api/customer-settle/check/` 客户对账 API；该 API 属于并行的后端/API基线变更。

## 审查修复（2026-07-27）

- 修复组件复用时仅在 `created` 校验路由的问题：监听 `$route.query`，客户变更时清空旧表格/勾选并重新查询；客户参数缺失时清空状态并阻断请求。
- 在查询、导出、对账和结算入口重新校验路由；请求返回时也确认客户未切换，避免旧请求覆盖新客户数据。
- `DirectSettleRow` 增加可选数值字段 `unSettleAmount`、`checkAmount`，移除 `buildDirectSettlePayload` 中的 `any`；测试覆盖正负金额透传。

### 审查修复验证

- 红：新增路由切换测试在旧实现中因缺少 `$route.query` watcher 失败。
- 绿：`cd frontend; pnpm test:unit src/router/routes/customerSettleRoute.test.ts src/views/customer-settle/sheet/customerSettleWorkbench.test.ts`：12/12 通过。
- `cd frontend; pnpm exec eslint --max-warnings 0 src/views/customer-settle/sheet/detail.vue src/views/customer-settle/sheet/customerSettleWorkbench.ts src/views/customer-settle/sheet/customerSettleWorkbench.test.ts`：通过。
- `cd frontend; pnpm build`：通过。
- `cd frontend; pnpm type:check`：未完成；`vue-tsc` 在 Node 20.20.0 启动阶段报 `Search string not found: "for (const existingRoot of buildInfoVersionMap.roots) {"`，未产生项目类型诊断。

## P1 竞态复审修复（2026-07-27）

- 根因：`loadList` 原先只以客户 ID 保护成功分支；旧请求的 `catch` 与 `finally` 会无条件清空数据/勾选并关闭加载状态，且 C1→C2→C1 时旧 C1 成功响应会通过客户 ID 判断并覆盖最后一次请求。
- 修复：新增单调递增的 `loadRequestSequence`；每次 `loadList` 调用捕获序列号，只有序列仍为最新时才更新成功、失败和加载状态。
- 测试：新增可控异步请求回归用例，覆盖旧 C1 拒绝不影响 C2 现有数据/加载状态，以及 C1→C2→C1 时仅最后一次 C1 请求可写入。

### P1 复审验证

- 红：`cd frontend; pnpm vitest run src/views/customer-settle/sheet/customerSettleWorkbench.test.ts`：新增 2 个用例在旧实现中失败，分别暴露旧失败清空数据与同客户旧响应覆盖。
- 绿：同一命令：12/12 通过。
- `cd frontend; pnpm exec eslint --max-warnings 0 src/views/customer-settle/sheet/detail.vue src/views/customer-settle/sheet/customerSettleWorkbench.test.ts`：通过。
- `cd frontend; pnpm run build`：通过（保留项目既有的 `vg-print` eval、动态导入和 chunk 大小警告）。
- `git diff --check`：通过。
