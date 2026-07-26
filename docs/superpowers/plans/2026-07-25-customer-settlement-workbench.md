# 客户结算工作台重构 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将客户结算改造成销售单/销售退货单直接结算的工作台和结算记录，并删除旧客户预收、费用、对账和结算流程代码。

**Architecture:** 工作台批量聚合销售出库单与销售退货单；客户结算单与明细保存最终结算记录。直接结算在 Service 事务中校验、金额分摊、写入结算记录并更新源单状态；所有现有客户结算表保留且不迁移。

**Tech Stack:** Java 8、Spring Boot 2.2、MyBatis-Plus、JUnit 4、Vue 3、TypeScript、ant-design-vue、vxe-table。

## Global Constraints

- Controller 仅做参数校验和 `InvokeResult` 包装；业务与事务放 Service。
- 校验失败使用 `DefaultClientException`；批量读取和更新，禁止循环查库。
- 金额分摊复用 `SettleAmountAllocationUtil.allocate`，保留两位小数、末项补尾差、不得产生负数。
- 仅删除代码/API/页面/路由，保留数据库表且不执行删表迁移。
- 所有新增方法使用中文 Javadoc 注释。

---

### Task 1: 建立客户结算工作台的读模型与查询接口

**Files:**
- Create: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/bo/sheet/customer/CustomerSaleSettleInfoBo.java`
- Create: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/vo/sheet/customer/QueryCustomerSaleSettleInfoVo.java`
- Modify: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/service/CustomerSettleSheetService.java`
- Modify: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/impl/CustomerSettleSheetServiceImpl.java`
- Modify: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/controller/CustomerSettleSheetController.java`
- Test: `backend/xingyun-settle/src/test/java/com/lframework/xingyun/settle/controller/CustomerSettleSheetControllerTest.java`

**Interfaces:**
- Produces `POST /customer/settle/sheet/sale-settle-infos` → `PageResult<CustomerSaleSettleInfoBo>`.
- `CustomerSaleSettleInfoBo` fields: `id, bizType, code, customerId, customerName, totalAmount, receivedAmount, settleAmount, unSettleAmount, settleStatus`.
- Service contract: `PageResult<CustomerSaleSettleInfoBo> querySaleSettleInfos(QueryCustomerSaleSettleInfoVo vo)`.

- [ ] **Step 1: 写失败测试**

```java
@Test
public void shouldReturnCustomerSaleSettlePage() throws Exception {
  PageResult<CustomerSaleSettleInfoBo> expected = PageResultUtil.newInstance(1, 20, 3,
      Collections.singletonList(new CustomerSaleSettleInfoBo()));
  Mockito.when(customerSettleSheetService.querySaleSettleInfos(Mockito.any())).thenReturn(expected);
  Assert.assertSame(expected, controller.querySaleSettleInfos(new QueryCustomerSaleSettleInfoVo()).getData());
}
```

- [ ] **Step 2: 验证失败**

Run: `cd backend && mvn -pl xingyun-settle -Dtest=CustomerSettleSheetControllerTest test`

Expected: 编译失败，工作台类型和方法不存在。

- [ ] **Step 3: 最小实现**

分别批量查询销售出库单和销售退货单；用单次 `CustomerSettleSheetDetail` 查询按 `bizId` 汇总已结算金额；计算 `unSettleAmount = max(totalAmount - receivedAmount - settleAmount, 0)`。Controller 记录异常日志后返回受控失败响应。

```java
@PostMapping("/sale-settle-infos")
public InvokeResult<PageResult<CustomerSaleSettleInfoBo>> querySaleSettleInfos(
    @RequestBody @Valid QueryCustomerSaleSettleInfoVo vo) {
  try {
    return InvokeResultBuilder.success(customerSettleSheetService.querySaleSettleInfos(vo));
  } catch (Exception e) {
    log.error("查询客户结算工作台失败", e);
    return InvokeResultBuilder.fail(e.getMessage(), null);
  }
}
```

- [ ] **Step 4: 验证并提交**

Run: `cd backend && mvn -pl xingyun-settle -Dtest=CustomerSettleSheetControllerTest test`

Expected: PASS。

```bash
git add backend/xingyun-settle/src/main/java backend/xingyun-settle/src/test/java
git commit -m "feat: add customer settlement workbench query"
```

### Task 2: 实现客户直接结算与源单状态维护

**Files:**
- Modify: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/vo/sheet/customer/CreateCustomerSettleSheetVo.java`
- Modify: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/vo/sheet/customer/CustomerSettleSheetItemVo.java`
- Modify: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/service/CustomerSettleSheetService.java`
- Modify: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/impl/CustomerSettleSheetServiceImpl.java`
- Modify: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/controller/CustomerSettleSheetController.java`
- Modify: `backend/xingyun-settle/src/main/resources/mappers/CustomerSettleSheetMapper.xml`
- Create: `backend/xingyun-settle/src/test/java/com/lframework/xingyun/settle/impl/CustomerSettleSheetServiceImplTest.java`

**Interfaces:**
- `CreateCustomerSettleSheetVo`: `customerId, settleAmount, description, items`。
- `CustomerSettleSheetItemVo`: `bizId, bizType`。
- Produces `POST /customer/settle/sheet/approve/pass/direct`，成功后创建已审核结算单和明细。

- [ ] **Step 1: 写失败测试**

```java
@Test(expected = DefaultClientException.class)
public void directApprovePass_shouldRejectDifferentCustomers() {
  service.directApprovePass(voWithCustomer("C1", itemOfCustomer("C2")));
}
```

同时覆盖：负数金额、源单不是未结算/部分结算、状态更新返回 0 时抛出异常且不调用 `saveBatch`；分摊后所有明细总额等于 `settleAmount`。

- [ ] **Step 2: 验证失败**

Run: `cd backend && mvn -pl xingyun-settle -Dtest=CustomerSettleSheetServiceImplTest test`

Expected: FAIL，直接结算尚依赖客户对账单。

- [ ] **Step 3: 最小实现**

在一个 `@Transactional` Service 方法内，按业务类型批量读取源单，校验存在性、客户一致性、状态和金额；使用 `SettleAmountAllocationUtil.allocate` 对当前未结算额分摊；保存 `customer_settle_sheet`、`customer_settle_sheet_detail`；按类型调用 `SaleOutSheetService` 或 `SaleReturnService` 的 `setPartSettle/setSettled`。每次状态更新返回值必须为 1，否则抛出 `DefaultClientException` 回滚。

```java
@Transactional(rollbackFor = Exception.class)
public String directApprovePass(CreateCustomerSettleSheetVo vo) {
  validateDirectSettle(vo);
  List<BigDecimal> amounts = SettleAmountAllocationUtil.allocate(
      vo.getSettleAmount(), queryCurrentUnSettleAmounts(vo.getItems()));
  return createApprovedSheetAndUpdateBizStatus(vo, amounts);
}
```

移除旧对账单依赖的 `getBizItem`、`getUnSettleItems`、修改/普通审核/拒绝接口；保留记录查询、详情、直审和导出。详情与记录用销售单、退货单批量映射单号，不查询已删除对账表。

- [ ] **Step 4: 验证并提交**

Run: `cd backend && mvn -pl xingyun-settle -Dtest=CustomerSettleSheetServiceImplTest,SettleAmountAllocationUtilTest test`

Expected: PASS。

```bash
git add backend/xingyun-settle/src/main/java backend/xingyun-settle/src/main/resources backend/xingyun-settle/src/test/java
git commit -m "feat: support direct customer settlement"
```

### Task 3: 增加工作台和结算记录导出

**Files:**
- Create: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/excel/sheet/customer/CustomerSaleSettleInfoExportModel.java`
- Create: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/excel/sheet/customer/CustomerSaleSettleInfoExportTaskWorker.java`
- Modify: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/excel/sheet/customer/CustomerSettleSheetExportModel.java`
- Modify: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/excel/sheet/customer/CustomerSettleSheetExportTaskWorker.java`
- Modify: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/controller/CustomerSettleSheetController.java`

- [ ] **Step 1: 写失败测试**

```java
@Test
public void shouldCreateCustomerWorkbenchExportTask() {
  Assert.assertTrue(controller.exportSaleSettleInfos(new QueryCustomerSaleSettleInfoVo()).isSuccess());
}
```

- [ ] **Step 2: 验证失败**

Run: `cd backend && mvn -pl xingyun-settle -Dtest=CustomerSettleSheetControllerTest test`

Expected: 编译失败，工作台导出方法不存在。

- [ ] **Step 3: 实现导出**

新增工作台导出接口 `POST /customer/settle/sheet/export-sale-settle-infos` 和记录导出接口 `POST /customer/settle/sheet/export-record`。导出任务只使用新工作台或保留结算单数据，不读取旧预收、费用和对账表。

- [ ] **Step 4: 验证并提交**

Run: `cd backend && mvn -pl xingyun-settle -Dtest=CustomerSettleSheetControllerTest test`

Expected: PASS。

```bash
git add backend/xingyun-settle/src/main/java backend/xingyun-settle/src/test/java
git commit -m "feat: export customer settlement workbench"
```

### Task 4: 实现客户结算工作台和结算记录页面

**Files:**
- Create: `frontend/src/views/customer-settle/sheet/settle.vue`
- Create: `frontend/src/views/customer-settle/sheet/record.vue`
- Create: `frontend/src/api/customer-settle/sheet/model/queryCustomerSaleSettleInfoVo.ts`
- Create: `frontend/src/api/customer-settle/sheet/model/customerSaleSettleInfoBo.ts`
- Modify: `frontend/src/api/customer-settle/sheet/index.ts`
- Modify: `frontend/src/router/routes/index.ts`
- Create: `frontend/src/views/customer-settle/sheet/customerSettleWorkbench.test.ts`

**Interfaces:**
- Consumes `querySaleSettleInfos`、`exportSaleSettleInfos`、`directApprovePass`、`query`、`exportRecord`。
- Produces routes `customer/sheet/settle` and `customer/sheet-record`。

- [ ] **Step 1: 写失败测试**

```ts
it('rejects a selection containing different customers', () => {
  expect(canDirectSettle([{ customerId: 'C1' }, { customerId: 'C2' }])).toBe(false);
});
```

- [ ] **Step 2: 验证失败**

Run: `cd frontend && pnpm exec vitest run src/views/customer-settle/sheet/customerSettleWorkbench.test.ts`

Expected: FAIL，目标工作台模块不存在。

- [ ] **Step 3: 实现页面和 API**

工作台复用供应商 `frontend/src/views/settle/sheet/settle.vue` 的分页、筛选、导出和确认结算交互，删除对账弹窗与 `settleCheckApi`。表格改为客户、销售单/销售退货单、应收、已收、已结算、未结算、状态；只允许同一客户且状态为未结算/部分结算的勾选。

```ts
const payload = {
  customerId: selectedRows[0].customerId,
  settleAmount: settleDialog.amount,
  description: settleDialog.description || undefined,
  items: selectedRows.map(({ id, bizType }) => ({ bizId: id, bizType })),
};
await api.directApprovePass(payload);
```

记录页复用供应商 `record.vue` 的展开明细和导出，替换为客户选择器，并根据 `bizType` 跳转销售出库或销售退货列表。路由只保留这两条客户结算路由。

- [ ] **Step 4: 验证并提交**

Run: `cd frontend && pnpm run lint`

Expected: PASS。

```bash
git add frontend/src/views/customer-settle/sheet frontend/src/api/customer-settle/sheet frontend/src/router/routes/index.ts
git commit -m "feat: add customer settlement workbench pages"
```

### Task 5: 清理旧客户结算实现

**Files:**
- Delete directories: `frontend/src/views/customer-settle/pre-sheet`, `frontend/src/views/customer-settle/fee-sheet`, `frontend/src/views/customer-settle/check-sheet`, `frontend/src/api/customer-settle/pre`, `frontend/src/api/customer-settle/fee`, `frontend/src/api/customer-settle/check`
- Delete backend prefixes: `CustomerSettlePreSheet*`, `CustomerSettleFeeSheet*`, `CustomerSettleCheckSheet*` under `controller`, `service`, `impl`, `mappers`, `entity`, `vo`, `dto`, `bo`, `excel`, `enums`, and `src/main/resources/mappers`.
- Delete: `frontend/src/enums/biz/customerSettlePreSheetStatus.ts`, `customerSettleFeeSheetType.ts`, `customerSettleFeeSheetStatus.ts`, `customerSettleCheckSheetStatus.ts`, `customerSettleCheckSheetCalcType.ts`, `customerSettleCheckSheetBizType.ts`
- Modify: `frontend/src/router/routes/index.ts`, remaining customer settlement imports.

- [ ] **Step 1: 写清理失败检查**

```powershell
rg -n "customer-settle/(pre|fee|check)|CustomerSettle(Pre|Fee|Check)" backend frontend
if ($LASTEXITCODE -eq 0) { throw '发现旧客户结算引用' }
```

- [ ] **Step 2: 验证失败**

Run: 上述 PowerShell 命令。

Expected: FAIL，列出现有旧实现。

- [ ] **Step 3: 删除旧实现**

删除列出的代码和 API，不删除任何数据库迁移或表定义；从保留客户结算单代码中移除旧类型 import、注入和调用。

- [ ] **Step 4: 验证并提交**

Run: `rg -n "customer-settle/(pre|fee|check)|CustomerSettle(Pre|Fee|Check)" backend frontend`

Expected: 无输出，退出码 1。

Run: `cd backend && mvn clean compile -DskipTests`

Expected: BUILD SUCCESS。

```bash
git add -A backend/xingyun-settle frontend/src
git commit -m "refactor: remove legacy customer settlement flow"
```

### Task 6: 全量验证

**Files:**
- Modify: 仅限修复本计划引入的编译、类型或 lint 问题；不得恢复旧客户结算链路。

- [ ] **Step 1: 运行后端结算模块测试**

Run: `cd backend && mvn -pl xingyun-settle test`

Expected: BUILD SUCCESS。

- [ ] **Step 2: 运行完整后端编译**

Run: `cd backend && mvn clean compile -DskipTests`

Expected: BUILD SUCCESS。

- [ ] **Step 3: 运行前端检查**

Run: `cd frontend && pnpm run lint`

Expected: 退出码 0。

- [ ] **Step 4: 检查变更范围**

Run: `git diff --check && git status --short`

Expected: 无空白错误；仅客户结算重构、测试和文档文件变更。

- [ ] **Step 5: 提交最终修复**

```bash
git add -A
git commit -m "test: verify customer settlement workbench refactor"
```

