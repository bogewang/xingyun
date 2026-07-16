# Task 4 P2 修复报告

## 修复内容

- 为 `batchHandleFn` 分支补充精确回归断言：`handleFn` 调用 0 次，每行恰好触发一次 `confirm-row`，最终恰好触发一次 `confirm`。
- 补充 `batchHandleFn` 缺省时的旧分支回归测试：确认实例化 `ConcurrentPromise`，逐行调用 `handleFn`，并保持逐行 `confirm-row` 与一次 `confirm` 的行为。
- 未修改业务逻辑。

## 验证结果

- `pnpm exec vitest run src/views/base-data/product/info/__tests__/productAvailability.test.ts`：5/5 通过。
- `git diff --check`：通过。
