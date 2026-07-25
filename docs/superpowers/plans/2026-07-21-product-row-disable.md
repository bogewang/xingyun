# 商品信息行内停用按钮 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在商品信息列表中为启用商品提供带确认提示的行内停用操作。

**Architecture:** 保持现有单文件页面结构，在操作菜单数组中按商品状态新增一个操作项。该操作复用 `updateAvailable` 与 `buildProductAvailabilityRequest`，不新增接口或后端逻辑；成功后刷新列表。

**Tech Stack:** Vue 3、TypeScript、Ant Design Vue、Vitest。

## Global Constraints

- 只修改前端，复用 `PUT /basedata/product/available`。
- 行内停用操作仅对 `row.available === true` 的商品显示。
- 使用权限 `base-data:product:info:modify`。
- 停用前必须调用 `createConfirm`，成功后调用 `createSuccess` 并刷新列表。
- 所有新增方法必须添加中文注释。

---

### Task 1: 为单商品停用请求补充测试

**Files:**
- Modify: `frontend/src/views/base-data/product/info/__tests__/productAvailability.test.ts`

**Interfaces:**
- Consumes: `buildProductAvailabilityRequest(records, available)`。
- Produces: 断言 `{ ids: ['p-1'], available: false }` 可用于单商品停用请求。

- [ ] **Step 1: 写入失败测试**

在“去重商品 ID 并保留目标状态”测试后加入：

```ts
it('单个商品停用时生成停用状态请求', () => {
  expect(buildProductAvailabilityRequest([{ id: 'p-1' }], false)).toEqual({
    ids: ['p-1'],
    available: false,
  });
});
```

- [ ] **Step 2: 运行测试以确认当前覆盖缺失**

运行：`cd frontend && pnpm vitest run src/views/base-data/product/info/__tests__/productAvailability.test.ts`

预期：测试当前已可通过，因为请求构造函数已支持单个 ID；该步骤用于锁定行内停用复用的请求契约。

- [ ] **Step 3: 提交测试基线**

```bash
git add frontend/src/views/base-data/product/info/__tests__/productAvailability.test.ts
git commit -m "test: cover single product disable request"
```

### Task 2: 在商品操作列增加停用操作

**Files:**
- Modify: `frontend/src/views/base-data/product/info/index.vue:426-466`

**Interfaces:**
- Consumes: `api.updateAvailable(data)`、`buildProductAvailabilityRequest(records, available)`、`createConfirm(message)`、`createSuccess(message)`。
- Produces: `disableProduct(row)` 方法与仅对启用商品显示的“停用”操作项。

- [ ] **Step 1: 在 `methods` 中新增最小实现**

在 `doBatchDelete` 前添加：

```ts
/**
 * 停用单个商品。
 * @param row 商品列表行数据
 */
disableProduct(row) {
  createConfirm(`确认停用商品“${row.name}”？`).then(() => {
    api.updateAvailable(buildProductAvailabilityRequest([row], false)).then(() => {
      createSuccess('停用成功！');
      this.search();
    });
  });
},
```

- [ ] **Step 2: 在 `createActions(row)` 的“修改”操作后添加条件操作项**

```ts
...(row.available
  ? [
      {
        permission: ['base-data:product:info:modify'],
        label: '停用',
        danger: true,
        onClick: () => this.disableProduct(row),
      },
    ]
  : []),
```

将此代码放在“修改”操作对象之后、“删除”操作对象之前；已停用商品的操作数组不包含“停用”。

- [ ] **Step 3: 运行针对性测试**

运行：`cd frontend && pnpm vitest run src/views/base-data/product/info/__tests__/productAvailability.test.ts`

预期：全部测试通过。

- [ ] **Step 4: 运行前端静态检查**

运行：`cd frontend && pnpm run lint`

预期：命令以 0 退出；若存在与本次变更无关的既有错误，记录完整命令输出并确认本次文件未出现新的 lint 错误。

- [ ] **Step 5: 提交实现**

```bash
git add frontend/src/views/base-data/product/info/index.vue
git commit -m "feat: add product row disable action"
```
