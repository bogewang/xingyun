# 采购入库与销售出库金额编辑 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在采购入库和销售出库的八个新增、修改页面中支持编辑明细金额，并以金额作为手工输入行的汇总依据。

**Architecture:** 新增纯前端金额计算工具，集中维护手工金额状态、金额反算单价和行金额取值。八个页面只传入各自的数量与单价字段；金额输入写入手工状态，数量或单价输入解除该状态。汇总统一从工具读取行金额，避免六位小数单价反乘篡改用户金额。

**Tech Stack:** Vue 3 Options API、TypeScript、ant-design-vue、vxe-table、Vitest。

## Global Constraints

- 覆盖采购入库和销售出库的关联、非关联新增与修改页面，不改动审核、详情、列表和后端接口。
- 金额仅接受非负普通小数；拒绝负数、字母、多个小数点和科学计数法。
- 手工金额反算单价保留 6 位小数，自动金额与单据汇总按 2 位小数处理。
- 手工金额行汇总直接使用 `taxAmount`；数量或单价修改后切回自动金额。
- 数量为空或为 0 时不得除零，金额仍须保留；用户后续输入有效数量时切回自动金额。
- `manualTaxAmount` 为前端临时字段，不得进入接口请求参数。

---

## 文件结构

- 新建 `frontend/src/utils/sheetAmountInput.ts`：金额输入、切换自动金额、计算行金额的纯函数。
- 新建 `frontend/src/utils/__tests__/sheetAmountInput.test.ts`：公共金额规则测试。
- 修改采购入库的 `add-require.vue`、`add-un-require.vue`、`modify-require.vue`、`modify-un-require.vue`。
- 修改销售出库的 `add-require.vue`、`add-un-require.vue`、`modify-require.vue`、`modify-un-require.vue`。

### Task 1: 公共金额状态与计算工具

**Files:**
- Create: `frontend/src/utils/sheetAmountInput.ts`
- Test: `frontend/src/utils/__tests__/sheetAmountInput.test.ts`

**Interfaces:**
- Produces `applyManualSheetAmount(row, amount, quantityField, priceField): void`：清理金额、设置 `manualTaxAmount`，并在数量大于 0 时按 6 位小数更新单价。
- Produces `clearManualSheetAmount(row): void`：删除 `manualTaxAmount`。
- Produces `getSheetLineAmount(row, quantityField, priceField): number`：手工金额行返回 `taxAmount`，其他行返回数量乘单价后的 2 位金额。

- [ ] **Step 1: 写入失败的公共规则测试**

```ts
import { describe, expect, it } from 'vitest';
import { applyManualSheetAmount, clearManualSheetAmount, getSheetLineAmount } from '../sheetAmountInput';

describe('单据明细金额输入', () => {
  it('输入金额后反算六位小数单价且保留手工金额', () => {
    const row = { receiveNum: '3', purchasePrice: '', taxAmount: '' };
    applyManualSheetAmount(row, '80', 'receiveNum', 'purchasePrice');
    expect(row.taxAmount).toBe('80');
    expect(row.purchasePrice).toBe(26.666667);
    expect(row.manualTaxAmount).toBe(true);
    expect(getSheetLineAmount(row, 'receiveNum', 'purchasePrice')).toBe(80);
  });

  it('清除手工金额后使用数量乘单价', () => {
    const row = { outNum: '3', taxPrice: 26.6, taxAmount: '80', manualTaxAmount: true };
    clearManualSheetAmount(row);
    expect(getSheetLineAmount(row, 'outNum', 'taxPrice')).toBe(79.8);
  });

  it('非法输入保留上一次金额且零数量不反算单价', () => {
    const row = { outNum: '0', taxPrice: '12', taxAmount: '8.5', manualTaxAmount: true };
    applyManualSheetAmount(row, '1e3', 'outNum', 'taxPrice');
    expect(row.taxAmount).toBe('8.5');
    expect(row.taxPrice).toBe('12');
    expect(row.manualTaxAmount).toBe(true);
  });
});
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd frontend && pnpm vitest run src/utils/__tests__/sheetAmountInput.test.ts`

Expected: FAIL，提示无法解析 `../sheetAmountInput`。

- [ ] **Step 3: 实现最小公共工具**

```ts
import { div, getNumber, mul } from '@/utils/number';
import { sanitizeNonNegativeDecimalInput } from '@/utils/numberInput';

type SheetAmountRow = Record<string, any>;

export function applyManualSheetAmount(row: SheetAmountRow, amount: string | number | null | undefined, quantityField: string, priceField: string): void {
  row.taxAmount = sanitizeNonNegativeDecimalInput(amount, String(row.taxAmount ?? ''));
  row.manualTaxAmount = true;
  const quantity = Number(row[quantityField]);
  if (quantity > 0 && row.taxAmount !== '') {
    row[priceField] = getNumber(div(row.taxAmount, quantity), 6);
  }
}

export function clearManualSheetAmount(row: SheetAmountRow): void {
  delete row.manualTaxAmount;
}

export function getSheetLineAmount(row: SheetAmountRow, quantityField: string, priceField: string): number {
  if (row.manualTaxAmount) return Number(row.taxAmount || 0);
  return getNumber(mul(row[quantityField] || 0, row[priceField] || 0), 2);
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd frontend && pnpm vitest run src/utils/__tests__/sheetAmountInput.test.ts`

Expected: PASS，3 个测试全部通过。

- [ ] **Step 5: 提交公共工具**

```bash
git add frontend/src/utils/sheetAmountInput.ts frontend/src/utils/__tests__/sheetAmountInput.test.ts
git commit -m "feat: 支持明细手工金额计算"
```

### Task 2: 采购入库新增与修改页面

**Files:**
- Modify: `frontend/src/views/sc/purchase/receive/add-require.vue`
- Modify: `frontend/src/views/sc/purchase/receive/add-un-require.vue`
- Modify: `frontend/src/views/sc/purchase/receive/modify-require.vue`
- Modify: `frontend/src/views/sc/purchase/receive/modify-un-require.vue`

**Interfaces:**
- Consumes Task 1 的 `applyManualSheetAmount(row, value, 'receiveNum', 'purchasePrice')`、`clearManualSheetAmount(row)`、`getSheetLineAmount(row, 'receiveNum', 'purchasePrice')`。
- Produces 每个页面的 `taxAmountInput(row, value)`。

- [ ] **Step 1: 使金额单元格可编辑**

在每个 `#taxAmount_default` 插槽使用下列组件，替换原有按采购价乘收货数量显示的 `<span>`：

```vue
<a-input v-model:value="row.taxAmount" class="number-input" @input="(e) => taxAmountInput(row, e.target.value)" />
```

- [ ] **Step 2: 导入工具并实现金额事件**

```ts
import { applyManualSheetAmount, clearManualSheetAmount, getSheetLineAmount } from '@/utils/sheetAmountInput';

taxAmountInput(row, value) {
  applyManualSheetAmount(row, value, 'receiveNum', 'purchasePrice');
  this.calcSum();
},
```

- [ ] **Step 3: 数量和采购价修改解除手工金额**

在 `purchasePriceInput` 和 `receiveNumInput` 的实际赋值路径中先执行 `clearManualSheetAmount(row)`，再执行原有输入清理和 `this.calcSum()`。批量录入、选单位经这些事件后自动恢复自动金额。

- [ ] **Step 4: 更新汇总取值**

在四个 `calcSum` 中保留数量累加，并将金额累加改为：

```ts
totalAmount = add(totalAmount, getSheetLineAmount(t, 'receiveNum', 'purchasePrice'));
```

筛选条件改为：

```ts
.filter((t) => t.manualTaxAmount || (isFloatGeZero(t.purchasePrice) && isFloatGeZero(t.receiveNum)))
```

- [ ] **Step 5: 运行检查并提交**

Run: `cd frontend && pnpm lint -- src/views/sc/purchase/receive/add-require.vue src/views/sc/purchase/receive/add-un-require.vue src/views/sc/purchase/receive/modify-require.vue src/views/sc/purchase/receive/modify-un-require.vue`

Expected: PASS，无 lint 错误。

```bash
git add frontend/src/views/sc/purchase/receive
git commit -m "feat: 支持采购入库明细金额编辑"
```

### Task 3: 销售出库新增与修改页面

**Files:**
- Modify: `frontend/src/views/sc/sale/out/add-require.vue`
- Modify: `frontend/src/views/sc/sale/out/add-un-require.vue`
- Modify: `frontend/src/views/sc/sale/out/modify-require.vue`
- Modify: `frontend/src/views/sc/sale/out/modify-un-require.vue`

**Interfaces:**
- Consumes Task 1 的 `applyManualSheetAmount(row, value, 'outNum', 'taxPrice')`、`clearManualSheetAmount(row)`、`getSheetLineAmount(row, 'outNum', 'taxPrice')`。
- Produces 每个页面的 `taxAmountInput(row, value)`。

- [ ] **Step 1: 使金额单元格可编辑**

在每个 `#taxAmount_default` 插槽使用：

```vue
<a-input v-model:value="row.taxAmount" class="number-input" @input="(e) => taxAmountInput(row, e.target.value)" />
```

保留现有列标题（含税金额或金额）和宽度，移除 `taxPrice × outNum` 的只读 `<span>`。

- [ ] **Step 2: 导入工具并实现金额事件**

```ts
import { applyManualSheetAmount, clearManualSheetAmount, getSheetLineAmount } from '@/utils/sheetAmountInput';

taxAmountInput(row, value) {
  applyManualSheetAmount(row, value, 'outNum', 'taxPrice');
  this.calcSum();
},
```

- [ ] **Step 3: 数量和销售单价修改解除手工金额**

在 `taxPriceInput`、`outNumInput` 的实际赋值路径前执行 `clearManualSheetAmount(row)`。修改页面的 `costPriceInput` 不得清除手工销售金额。

- [ ] **Step 4: 更新汇总取值**

在四个 `calcSum` 中使用：

```ts
totalAmount = add(totalAmount, getSheetLineAmount(t, 'outNum', 'taxPrice'));
```

筛选条件为：

```ts
.filter((t) => t.manualTaxAmount || (isFloatGeZero(t.taxPrice) && isFloatGeZero(t.outNum)))
```

- [ ] **Step 5: 运行检查并提交**

Run: `cd frontend && pnpm lint -- src/views/sc/sale/out/add-require.vue src/views/sc/sale/out/add-un-require.vue src/views/sc/sale/out/modify-require.vue src/views/sc/sale/out/modify-un-require.vue`

Expected: PASS，无 lint 错误。

```bash
git add frontend/src/views/sc/sale/out
git commit -m "feat: 支持销售出库明细金额编辑"
```

### Task 4: 全量验证

**Files:**
- Verify: `frontend/src/utils/__tests__/numberInput.test.ts`
- Verify: `frontend/src/utils/__tests__/sheetAmountInput.test.ts`
- Verify: Tasks 2–3 的八个 Vue 文件。

- [ ] **Step 1: 运行金额工具测试**

Run: `cd frontend && pnpm vitest run src/utils/__tests__/numberInput.test.ts src/utils/__tests__/sheetAmountInput.test.ts`

Expected: PASS，现有输入清理测试与新增金额规则测试均通过。

- [ ] **Step 2: 运行前端静态检查**

Run: `cd frontend && pnpm run lint`

Expected: PASS，无新增错误。

- [ ] **Step 3: 审查最终改动**

Run: `git diff --check && git status --short`

Expected: 无空白错误；仅保留用户主动要求未提交的文件（正常情况下为空）。
