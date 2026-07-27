# Task 4 Report

- 在 `detail.vue` 的 `$nextTick` 回调中再次校验 `requestSequence === loadRequestSequence`，避免新路由或查询请求开始后旧请求同步勾选状态。
- 新增可控延后 `$nextTick` 的 Vitest 回归测试，覆盖旧请求成功、新请求开始、旧回调随后执行的竞态。
- 已执行：`pnpm vitest run src/views/customer-settle/sheet/customerSettleWorkbench.test.ts`（13 项通过）。
- 已执行：`pnpm exec eslint src/views/customer-settle/sheet/detail.vue src/views/customer-settle/sheet/customerSettleWorkbench.test.ts`（通过）。
