# 采购收货单付款金额合计 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在采购收货单列表的合计行展示当前页的本单已付和未付金额。

**Architecture:** 复用收货单列表组件已有的 `footerMethod`、`sumByField` 与 `formatAmount`。该方法基于 vxe-grid 当前页传入的 `data` 生成合计行，因此新增两个字段的汇总不会改变接口或分页逻辑。

**Tech Stack:** Vue 3、TypeScript、ant-design-vue、vxe-table 4.4.5。

## Global Constraints

- 仅修改前端收货单列表的合计展示，不修改后端接口或金额口径。
- `paidAmount` 与 `unpaidAmount` 均按当前页数据汇总，并使用既有 `formatAmount` 格式化。
- 保留现有列定义、合计项和 vxe-table 配置。

---

### Task 1: 补齐付款金额合计

**Files:**
- Modify: `frontend/src/views/sc/purchase/receive/components/sheet-list.vue:451-473`
- Test: `frontend/src/views/sc/purchase/receive/components/sheet-list.vue`（通过类型检查验证模板与脚本）

**Interfaces:**
- Consumes: `footerMethod({ columns, data })` 的 vxe-table 参数，`sumByField(data, field)` 和 `formatAmount(value)` 组件方法。
- Produces: `paidAmount` 与 `unpaidAmount` 两列的格式化合计单元格文本。

- [ ] **Step 1: 记录预期行为**

在 `footerMethod` 的返回映射中，要求 `paidAmount` 列返回 `formatAmount(sumByField(data, 'paidAmount'))`，`unpaidAmount` 列返回 `formatAmount(sumByField(data, 'unpaidAmount'))`；其他列继续遵循现有分支。

- [ ] **Step 2: 验证改动前缺少行为**

Run: `rg -n -C 3 "paidAmount|unpaidAmount" frontend/src/views/sc/purchase/receive/components/sheet-list.vue`

Expected: 列定义包含两个字段，但 `footerMethod` 中不存在它们的汇总和返回分支。

- [ ] **Step 3: 实现最小改动**

在 `footerMethod` 中增加：

```ts
const paidAmount = this.sumByField(data, 'paidAmount');
const unpaidAmount = this.sumByField(data, 'unpaidAmount');
```

并在 `columns.map` 中、现有 `totalAmount` 分支后增加：

```ts
if (column.field === 'paidAmount') {
  return this.formatAmount(paidAmount);
}

if (column.field === 'unpaidAmount') {
  return this.formatAmount(unpaidAmount);
}
```

- [ ] **Step 4: 验证改动后行为与前端类型**

Run: `pnpm run lint`

Expected: 退出码为 0，且没有 lint 错误。

- [ ] **Step 5: 提交实现**

```bash
git add frontend/src/views/sc/purchase/receive/components/sheet-list.vue
git commit -m "feat: show receive payment totals"
```
