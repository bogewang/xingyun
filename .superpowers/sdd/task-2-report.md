# Task 2：替换 PrintDesigner

## 完成内容

- 使用 PrintDot Web Component 替换 `FullDesigner` 和 `hiprint` 注册。
- 有效的 `{ data: ... }` 模板会加载到 Web Component，并设置测试数据。
- 保存按钮从 Web Component 读取 `getTemplateData()` 与 `getTestData()`，继续发出 `save(templateJson, demoData)`。
- 保留字段说明与 `getFieldDesc`，删除 vg-print 本地导入缓存逻辑。
- 检测到旧版 `panels` 模板时显示“请先迁移模板”，且不挂载设计器。
- 设置页不再生成 `panels` 空模板，空模板值为 `null`。

## TDD 记录

- RED：新增 `PrintDesigner.test.ts` 后运行定向测试，旧组件因 Task 1 已移除的 `normalizeTemplate` 导出而失败。
- GREEN：替换设计器实现后，定向测试通过。

## 验证

```text
pnpm vitest run src/components/PrintDesigner/src/PrintDesigner.test.ts --reporter=verbose
1 passed
```

`git diff --check` 通过。

## 已知跨任务状态

Task 1 已移除 `printUtils` 的旧模板导出；Task 3 尚未迁移 `PrintDialog` 和 `printRuntime` 的对应引用，因此全量类型检查应在 Task 3 连续完成后执行。
