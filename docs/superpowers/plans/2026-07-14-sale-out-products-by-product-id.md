# 销售出库商品明细按商品编号提交 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增需订单和非订单销售出库时，将每个含 `productId` 的表格行提交到 `products`，不再因数量为空或为零而遗漏。

**Architecture:** 将两类页面的明细映射提取为无 UI 依赖的纯函数，由各自 `buildParams()` 调用。纯函数只以 `productId` 决定是否保留一行，并保留原始 `outNum` 到 `orderNum` 的映射，因此可以用 Vitest 覆盖请求数据而不挂载大型 Vue 页面。

**Tech Stack:** Vue 3 Options API、TypeScript、Vite、Vitest、pnpm 8。

## Global Constraints

- 有效明细的唯一准则是非空 `productId`；空数量和零数量均必须保留。
- `outNum` 不做默认值转换：空值与 `0` 分别保留其原始语义。
- 仅修改两个新增销售出库页；不改编辑页、退货页、后端接口或库存业务。
- 保留现有价格/数量格式、精度及非负数校验；需订单页现有“至少一条正数量”的前置提交校验保持不变。

---

### Task 1: 建立可回归测试的纯明细组装函数

**Files:**
- Create: `frontend/src/views/sc/sale/out/components/saleOutProductParams.ts`
- Create: `frontend/src/views/sc/sale/out/components/__tests__/saleOutProductParams.test.ts`
- Modify: `frontend/package.json`

**Interfaces:**
- Produces: `buildUnrequiredSaleOutProducts(rows)`，返回 `{ productId, unitId, oriPrice, taxPrice, orderNum, description }[]`。
- Produces: `buildRequiredSaleOutProducts(rows)`，返回 `{ productId, orderNum, description, oriPrice, taxPrice, saleOrderDetailId? }[]`。
- Consumes: 包含 `productId`、`outNum` 及对应请求字段的表格行数组。

- [ ] **Step 1: 写失败的前端单元测试**

在 `frontend/src/views/sc/sale/out/components/__tests__/saleOutProductParams.test.ts` 编写以下断言；不要挂载 Vue 组件：

```ts
import { describe, expect, it } from 'vitest';
import {
  buildRequiredSaleOutProducts,
  buildUnrequiredSaleOutProducts,
} from '../saleOutProductParams';

describe('销售出库明细请求组装', () => {
  it('非订单出库保留有商品编号但数量为空和为零的行', () => {
    expect(buildUnrequiredSaleOutProducts([
      { productId: 'p-empty', unitId: 'u-1', outNum: '', oriPrice: '10', taxPrice: '10', description: '' },
      { productId: 'p-zero', unitId: 'u-2', outNum: 0, oriPrice: '20', taxPrice: '20', description: '零数量' },
      { productId: '', outNum: 3 },
    ])).toEqual([
      { productId: 'p-empty', unitId: 'u-1', orderNum: '', oriPrice: '10', taxPrice: '10', description: '' },
      { productId: 'p-zero', unitId: 'u-2', orderNum: 0, oriPrice: '20', taxPrice: '20', description: '零数量' },
    ]);
  });

  it('需订单出库保留有商品编号的零数量行及订单明细关联', () => {
    expect(buildRequiredSaleOutProducts([
      { id: 'detail-1', isFixed: true, productId: 'p-1', outNum: 0, oriPrice: '5', taxPrice: '5', description: '' },
      { productId: '', outNum: 2 },
    ])).toEqual([
      { productId: 'p-1', orderNum: 0, description: '', oriPrice: '5', taxPrice: '5', saleOrderDetailId: 'detail-1' },
    ]);
  });
});
```

- [ ] **Step 2: 配置并验证红灯**

在 `frontend/package.json` 的 `scripts` 添加 `"test:unit": "vitest run"`，在 `devDependencies` 添加与当前 Vite 4 兼容的 `"vitest": "^0.34.6"`。安装依赖后运行：

```bash
pnpm --dir frontend install
pnpm --dir frontend run test:unit -- src/views/sc/sale/out/components/__tests__/saleOutProductParams.test.ts
```

Expected: FAIL，报错无法解析 `../saleOutProductParams`，证明测试覆盖的是尚未实现的边界。

- [ ] **Step 3: 实现最小纯函数**

创建 `saleOutProductParams.ts`，使用以下完整结构；`filter` 只能检查 `productId`：

```ts
export interface SaleOutTableRow {
  id?: string;
  productId?: string;
  unitId?: string;
  oriPrice?: string | number;
  taxPrice?: string | number;
  outNum?: string | number;
  description?: string;
  isFixed?: boolean;
}

const hasProductId = (row: SaleOutTableRow): row is SaleOutTableRow & { productId: string } =>
  !!row.productId;

export function buildUnrequiredSaleOutProducts(rows: SaleOutTableRow[]) {
  return rows.filter(hasProductId).map((row) => ({
    productId: row.productId,
    unitId: row.unitId,
    oriPrice: row.oriPrice,
    taxPrice: row.taxPrice,
    orderNum: row.outNum,
    description: row.description,
  }));
}

export function buildRequiredSaleOutProducts(rows: SaleOutTableRow[]) {
  return rows.filter(hasProductId).map((row) => {
    const product = {
      productId: row.productId,
      orderNum: row.outNum,
      description: row.description,
      oriPrice: row.oriPrice,
      taxPrice: row.taxPrice,
    };
    return row.isFixed ? { ...product, saleOrderDetailId: row.id } : product;
  });
}
```

不引入与页面无关的字段或默认值。

- [ ] **Step 4: 验证绿灯并提交**

```bash
pnpm --dir frontend run test:unit -- src/views/sc/sale/out/components/__tests__/saleOutProductParams.test.ts
git add frontend/package.json frontend/pnpm-lock.yaml frontend/src/views/sc/sale/out/components
git commit -m "test: cover sale out product request mapping"
```

Expected: 两个断言 PASS。

### Task 2: 接入非订单销售出库请求

**Files:**
- Modify: `frontend/src/views/sc/sale/out/add-un-require.vue:919-943`
- Test: `frontend/src/views/sc/sale/out/components/__tests__/saleOutProductParams.test.ts`

**Interfaces:**
- Consumes: `buildUnrequiredSaleOutProducts(this.tableData)`。
- Produces: 非订单创建与直接审核请求中的完整 `products`。

- [ ] **Step 1: 在 `buildParams()` 写入函数调用**

新增导入：

```ts
import { buildUnrequiredSaleOutProducts } from './components/saleOutProductParams';
```

用下列代码替换当前 `products: validTableData.filter(...).map(...)` 整段；删除只为该映射准备的 `validTableData`：

```ts
products: buildUnrequiredSaleOutProducts(this.tableData),
```

- [ ] **Step 2: 运行针对性测试和类型检查**

```bash
pnpm --dir frontend run test:unit -- src/views/sc/sale/out/components/__tests__/saleOutProductParams.test.ts
pnpm --dir frontend run type:check
```

Expected: PASS；TypeScript 不报告未使用的 `isFloatGtZero` 或导入错误。

- [ ] **Step 3: 手工验证请求边界**

启动 `pnpm --dir frontend run dev`，在非订单新增页选择一个商品、数量留空并点击保存。浏览器网络请求的 `products` 必须包含该商品的 `productId` 和空 `orderNum`；将数量填 `0` 后再次验证该行仍存在。

- [ ] **Step 4: 提交**

```bash
git add frontend/src/views/sc/sale/out/add-un-require.vue
git commit -m "fix: keep selected products in unrequired sale out"
```

### Task 3: 接入需订单销售出库请求

**Files:**
- Modify: `frontend/src/views/sc/sale/out/add-require.vue:902-930`
- Test: `frontend/src/views/sc/sale/out/components/__tests__/saleOutProductParams.test.ts`

**Interfaces:**
- Consumes: `buildRequiredSaleOutProducts(this.tableData)`。
- Produces: 需订单创建与直接审核请求中的完整 `products`，并保留固定行的 `saleOrderDetailId`。

- [ ] **Step 1: 在 `buildParams()` 写入函数调用**

新增导入：

```ts
import { buildRequiredSaleOutProducts } from './components/saleOutProductParams';
```

替换当前 `products: validTableData.filter(...).map(...)` 整段为：

```ts
products: buildRequiredSaleOutProducts(this.tableData),
```

保留 `validData()` 中的以下保护，确保订单出库至少有一行正数量：

```ts
if (validTableData.filter((item) => isFloatGtZero(item.outNum)).length === 0) {
  createError('销售订单中的商品必须全部或部分出库！');
  return false;
}
```

- [ ] **Step 2: 运行测试、类型检查与静态检查**

```bash
pnpm --dir frontend run test:unit -- src/views/sc/sale/out/components/__tests__/saleOutProductParams.test.ts
pnpm --dir frontend run type:check
pnpm --dir frontend exec eslint src/views/sc/sale/out/add-require.vue src/views/sc/sale/out/add-un-require.vue src/views/sc/sale/out/components/saleOutProductParams.ts
```

Expected: 全部 PASS，且不存在未使用导入。

- [ ] **Step 3: 手工验证订单关联字段**

在需订单新增页选择销售订单，录入至少一行正数量、保留另一行 `0`。保存请求须包含两条商品；原 `isFixed` 行必须带 `saleOrderDetailId`。

- [ ] **Step 4: 最终验证与提交**

```bash
git diff --check
pnpm --dir frontend run test:unit
pnpm --dir frontend run type:check
git add frontend/src/views/sc/sale/out/add-require.vue frontend/src/views/sc/sale/out/components frontend/package.json frontend/pnpm-lock.yaml
git commit -m "fix: keep selected products in required sale out"
```

Expected: 所有前端单元测试和类型检查 PASS，工作区无未提交业务改动。
