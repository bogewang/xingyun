# 采购入库与销售出库折后金额初始化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在既有单据的修改、查看、审核页首次加载时显示单据总金额，仅在修改页编辑明细后重算折后金额。

**Architecture:** 保持现有 Options API 页面和 `calcSum()` 计算逻辑不变。每个页面的 `loadData()` 将接口 `res.totalAmount` 写入表单，并移除加载完成后触发的 `calcSum()`；修改页已有的编辑事件继续调用 `calcSum()`，因此编辑后的金额仍由明细汇总覆盖。

**Tech Stack:** Vue 3.3、TypeScript、Vitest、pnpm、ESLint。

## Global Constraints

- 仅调整采购入库和销售出库的修改、查看、审核页面；新增页面不改动。
- 加载期折后金额必须等于接口 `res.totalAmount`。
- 修改页的明细编辑必须继续使用既有 `calcSum()` 重新计算数量和折后金额。
- 不新增接口、后端改动或无关重构。
- 所有代码注释使用中文。

---

### Task 1: 调整采购入库页面的加载期金额

**Files:**

- Modify: `frontend/src/views/sc/purchase/receive/modify-require.vue:607-648`
- Modify: `frontend/src/views/sc/purchase/receive/modify-un-require.vue:528-569`
- Modify: `frontend/src/views/sc/purchase/receive/approve.vue:235-268`
- Modify: `frontend/src/views/sc/purchase/receive/detail.vue:219-241`

**Interfaces:**

- Consumes: `api.get(this.id)` 返回的 `res.totalAmount`、`res.details`、`res.paidAmount`。
- Produces: 加载完成的 `formData.totalAmount` 保持为单据金额；修改页后续调用 `calcSum(): void` 时仍可覆盖为明细汇总金额。

- [ ] **Step 1: 写出加载期回归检查清单**

在浏览器或本地代理响应中准备一张采购收货单，使 `totalAmount` 与任意明细 `purchasePrice * receiveNum` 汇总不同，例如接口金额为 `88.88`、明细汇总为 `100`。记录预期：首次打开修改、审核、查看页均显示 `88.88`；在两个修改页改变数量或采购价后，金额显示当前明细汇总。

- [ ] **Step 2: 执行回归检查，确认现状失败**

打开上述任一采购收货单的查看页。

Expected: 当前版本显示 `100` 而不是 `88.88`，证明加载阶段的 `calcSum()` 覆盖了接口金额。

- [ ] **Step 3: 以最小改动保留接口金额**

在四个页面的 `loadData()` 表单赋值中，把加载期的金额字段从 `0` 改为接口金额，并删除在 `this.tableData = res.details || []`（或映射后的 `tableData`）之后、加载回调中的唯一 `this.calcSum();`。采购入库查看页需要同时直接计算未付款金额：

```ts
totalAmount: res.totalAmount || 0,
paidAmount: res.paidAmount || 0,
unpaidAmount: sub(res.totalAmount || 0, res.paidAmount || 0),
```

修改页保持既有编辑事件不变，例如：

```ts
purchasePriceInput(_row, _value) {
  this.calcSum();
},
receiveNumInput(row, value) {
  row.receiveNum = sanitizeNonNegativeDecimalInput(value);
  this.calcSum();
},
```

- [ ] **Step 4: 执行采购入库回归检查，确认通过**

重复步骤 1 的四种页面场景。

Expected: 未编辑时均显示 `88.88`；修改页编辑采购价、数量、商品、单位或增删明细后，`calcSum()` 显示当前明细汇总。

- [ ] **Step 5: 提交采购入库改动**

```bash
git add frontend/src/views/sc/purchase/receive/modify-require.vue frontend/src/views/sc/purchase/receive/modify-un-require.vue frontend/src/views/sc/purchase/receive/approve.vue frontend/src/views/sc/purchase/receive/detail.vue
git commit -m "fix: preserve receive total amount on load"
```

### Task 2: 调整销售出库页面的加载期金额

**Files:**

- Modify: `frontend/src/views/sc/sale/out/modify-require.vue:636-665`
- Modify: `frontend/src/views/sc/sale/out/modify-un-require.vue:577-634`
- Modify: `frontend/src/views/sc/sale/out/approve.vue:297-318`
- Modify: `frontend/src/views/sc/sale/out/detail.vue:297-320`

**Interfaces:**

- Consumes: `api.get(this.id)` 返回的 `res.totalAmount`、`res.details`、`res.paidAmount`。
- Produces: 加载完成的 `formData.totalAmount` 保持为单据金额；修改页的 `calcSum(): void` 在表格编辑后重算金额。

- [ ] **Step 1: 写出加载期回归检查清单**

准备一张销售出库单，使接口 `totalAmount` 为 `188.88`，而明细 `taxPrice * outNum` 汇总为 `200`。记录预期：修改、审核、查看页首次显示 `188.88`；两个修改页编辑表格后显示 `200` 或编辑后的新汇总。

- [ ] **Step 2: 执行回归检查，确认现状失败**

打开该销售出库单查看页。

Expected: 当前版本显示明细汇总 `200`，而不是 `188.88`。

- [ ] **Step 3: 以最小改动保留接口金额**

在四个页面的 `loadData()` 表单赋值中，将加载期的金额字段改为 `totalAmount: res.totalAmount || 0`，并删除明细赋值后的加载期 `this.calcSum();`。销售出库查看页还应使用接口金额计算未付款金额：

```ts
totalAmount: res.totalAmount || 0,
paidAmount: res.paidAmount || 0,
unpaidAmount: sub(res.totalAmount || 0, res.paidAmount || 0),
```

不要改动修改页既有表格编辑入口：

```ts
taxPriceInput(row, value) {
  row.taxPrice = sanitizeNonNegativeDecimalInput(value);
  this.calcSum();
},
outNumInput(row, value) {
  row.outNum = sanitizeNonNegativeDecimalInput(value);
  this.calcSum();
},
```

- [ ] **Step 4: 执行销售出库回归检查，确认通过**

重复步骤 1 的四种页面场景。

Expected: 未编辑表格时显示接口 `188.88`；修改页编辑单价、数量、商品、单位或增删明细后按当前明细重算折后金额，查看与审核页仍保留接口金额。

- [ ] **Step 5: 提交销售出库改动**

```bash
git add frontend/src/views/sc/sale/out/modify-require.vue frontend/src/views/sc/sale/out/modify-un-require.vue frontend/src/views/sc/sale/out/approve.vue frontend/src/views/sc/sale/out/detail.vue
git commit -m "fix: preserve sale out total amount on load"
```

### Task 3: 静态检查与自动化验证

**Files:**

- Verify: 任务 1 和任务 2 中的八个 Vue 页面。

**Interfaces:**

- Consumes: 前两项任务完成后的八个 Vue 单文件组件。
- Produces: 通过前端静态检查和现有 Vitest 测试的改动。

- [ ] **Step 1: 检查加载逻辑是否仍重算金额**

运行：

```powershell
rg -n -U "this\.tableData = (res\.details|tableData)[\s\S]{0,200}this\.calcSum\(\)" frontend/src/views/sc/purchase/receive frontend/src/views/sc/sale/out
```

Expected: 八个目标页面的 `loadData()` 中无匹配；新增页面可以保留匹配，因为不在本次范围内。

- [ ] **Step 2: 运行现有前端单元测试**

运行：

```powershell
pnpm --dir frontend test:unit
```

Expected: Vitest 退出码为 0。

- [ ] **Step 3: 运行前端 lint**

运行：

```powershell
pnpm --dir frontend lint
```

Expected: 命令退出码为 0，且没有新引入的 Vue、TypeScript 或样式检查错误。

- [ ] **Step 4: 检查提交范围**

运行：

```powershell
git status --short
git log -2 --oneline
```

Expected: 仅包含两次功能提交；不暂存或提交工作区中已有的无关文件。

