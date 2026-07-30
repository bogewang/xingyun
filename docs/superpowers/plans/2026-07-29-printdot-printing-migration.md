# PrintDot 打印组件替换 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans. Steps use checkbox syntax.

**Goal:** 以 `vue-print-designer@1.7.33` 替换 `vg-print`，保持业务打印入口和后端模板接口稳定。

**Architecture:** `PrintDesigner` 与 `PrintDialog` 封装 PrintDot Web Component；`printRuntimeApi.preview` 仍是业务页入口。运行时弹窗展示可见的 PrintDot 预览与模板切换，用户点击打印后调用 `print({ mode: 'browser' })`。

**Tech Stack:** Vue 3、TypeScript、Vitest、Vite、ant-design-vue、vue-print-designer 1.7.33。

## Global Constraints

- 精确固定 `vue-print-designer@1.7.33`，删除 `vg-print`。
- 不改后端 API，不自动转换或清空旧 `panels` 模板。
- 检测到旧模板时提示“请先迁移模板”，禁止加载、编辑和打印。
- 保留 `vgPrintPreview`、模板切换和字段说明；默认仅浏览器打印。

### Task 1: 建立 PrintDot 适配层

**Files:** Create `frontend/src/components/PrintDesigner/src/printdot.ts`; Create `frontend/src/components/PrintDesigner/src/printdot.test.ts`; Modify `frontend/src/components/PrintDesigner/src/printUtils.ts`.

- [ ] 写失败测试：`isPrintDotTemplate({ data: {} }) === true`、`isPrintDotTemplate({ panels: [] }) === false`、数组业务数据取第一项为变量对象。
- [ ] 运行 `pnpm vitest run src/components/PrintDesigner/src/printdot.test.ts`，预期因模块缺失失败。
- [ ] 实现 `PrintDesignerElement`（`loadTemplateData`、`getTemplateData`、`setTestData`、`getTestData`、`setVariables`、`print`）和 `isPrintDotTemplate`、`toPrintDotVariables`；删除 `panels` 规范化及 `buildPrintPayload`。
- [ ] 重跑上述测试，预期通过；提交 `feat: add PrintDot template adapter`。

### Task 2: 替换 PrintDesigner

**Files:** Modify `frontend/src/components/PrintDesigner/src/PrintDesigner.vue`; Modify `frontend/src/views/base-data/print-template/setting.vue`; Create `frontend/src/components/PrintDesigner/src/PrintDesigner.test.ts`.

- [ ] 写失败测试：从元素 `getTemplateData()`、`getTestData()` 读取并发出既有 `save(templateJson, demoData)` 契约。
- [ ] 使用 `<print-designer lang="zh">` 取代 `FullDesigner`；挂载后仅加载有效模板并设置测试数据；保存时读取 PrintDot 数据。
- [ ] 保留字段说明按钮与 `getFieldDesc`；删除 `hiprint.register`、导入缓存和 vg-print 专属代码；旧模板显示迁移提示。
- [ ] 将 setting 页空模板改为空值，不生成 `panels`；运行组件测试并提交 `feat: replace template designer with PrintDot`。

### Task 3: 替换 PrintDialog 运行时打印

**Files:** Modify `frontend/src/components/PrintDialog/src/PrintDialog.vue`; Modify `frontend/src/components/PrintDialog/src/printDialog.ts`; Modify `frontend/src/components/PrintDesigner/src/printRuntime.ts`; Create `frontend/src/components/PrintDialog/src/PrintDialog.test.ts`.

- [ ] 写失败测试：有效模板依次调用 `loadTemplateData`、`setVariables`、`print({ mode: 'browser' })`；旧模板抛出“请先迁移模板”。
- [ ] 用可见的 `<print-designer>` 替换 `Preview`、本地打印机连接、PDF 导出和 `vg-print` 实例；在弹窗保留模板下拉和“打印”按钮，用户点击按钮后才调用浏览器打印。
- [ ] 保留模板下拉、缓存和 `api.getSetting`；切换到旧/失败模板时保留当前有效模板并显示错误。
- [ ] 取消 `PrintDialogPayload` 和 `printRuntime` 中的 `panels` 类型约束；运行测试并提交 `feat: use PrintDot for runtime printing`。

### Task 4: 依赖与全量验证

**Files:** Modify `frontend/package.json`; Modify `frontend/pnpm-lock.yaml`; Modify `frontend/src/components/registerGlobComp.ts`.

- [ ] 删除 `vg-print`，添加精确版本 `vue-print-designer: 1.7.33`，执行 `pnpm install`。
- [ ] 在全局注册文件加入 `import 'vue-print-designer';` 和 `import 'vue-print-designer/style.css';`。
- [ ] 运行 `rg -n "vg-print|hiprint|createTemplate|refreshPrinterList" src`，预期无输出。
- [ ] 运行 `pnpm run test:unit && pnpm run type:check && pnpm run build`，预期全部 exit code 0；提交 `chore: replace vg-print dependency with PrintDot`。

## 自检

四项任务覆盖模板兼容、设计器保存、运行时模板切换/浏览器打印、依赖清理与自动化验证；所有跨层接口统一使用 `PrintDesignerElement` 和 `Record<string, unknown>`。
