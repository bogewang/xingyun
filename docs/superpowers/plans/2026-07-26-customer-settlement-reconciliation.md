# 客户结算对账工作台 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将客户结算工作台从直接结算改为“确认对账后再确认结算”，并保留两类可追溯记录。

**Architecture:** 恢复客户对账单及明细作为对账事实；工作台批量聚合销售出库/退货、对账明细及结算明细。对账和结算均由工作台发起直接确认，Service 在事务中校验、仅分摊差额、保存记录并用源单状态与版本号乐观锁更新状态。

**Tech Stack:** Java 8、Spring Boot 2.2.2、MyBatis-Plus 3.4.2、JUnit 4、Vue 3、TypeScript、ant-design-vue、vxe-table、Vitest。

## Global Constraints

- Controller 只做参数校验和 `InvokeResult` 响应包装；事务和业务编排仅在 Service。
- 业务拒绝一律抛 `DefaultClientException`；查询及写入使用批量操作。
- 单次勾选只允许同一客户、同一业务类型；销售出库（1）与销售退货（2）不可混选，退货金额按正数处理。
- 对账和结算金额大于零、最多两位小数；只分摊输入总额与业务基数的差额，最后一项补足尾差。
- 已结算单据不可对账或结算；源单更新必须按状态和 `settleVersion` 乐观锁校验受影响行数为 1。
- 不改供应商结算，不做数据库迁移，不恢复旧式对账独立页面。

---

### Task 1: 恢复精简的客户对账读写模型

**Files:**
- Create: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/entity/CustomerSettleCheckSheet.java`
- Create: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/entity/CustomerSettleCheckSheetDetail.java`
- Create: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/enums/CustomerSettleCheckSheetBizType.java`
- Create: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/mappers/CustomerSettleCheckSheetMapper.java`
- Create: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/mappers/CustomerSettleCheckSheetDetailMapper.java`
- Create: `backend/xingyun-settle/src/main/resources/mappers/CustomerSettleCheckSheetMapper.xml`
- Create: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/vo/check/customer/CreateCustomerSettleCheckSheetVo.java`
- Create: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/vo/check/customer/CustomerSettleCheckSheetItemVo.java`
- Create: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/service/CustomerSettleCheckSheetService.java`
- Create: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/impl/CustomerSettleCheckSheetServiceImpl.java`
- Create: `backend/xingyun-settle/src/test/java/com/lframework/xingyun/settle/impl/CustomerSettleCheckSheetServiceImplTest.java`

**Interfaces:**
- Consumes: `CreateCustomerSettleCheckSheetVo { String customerId; BigDecimal checkAmount; String description; List<CustomerSettleCheckSheetItemVo> items; }`.
- Produces: `String directApprovePass(CreateCustomerSettleCheckSheetVo vo)` and per-detail `payAmount`（已分摊差额后的对账金额）。

- [ ] **Step 1: 写失败测试**

```java
@Test(expected = DefaultClientException.class)
public void directApprovePass_shouldRejectMixedBizTypes() {
  CreateCustomerSettleCheckSheetVo vo = checkVo("C1", new BigDecimal("200"),
      item("O1", 1), item("R1", 2));
  service.directApprovePass(vo);
}

@Test
public void directApprovePass_shouldAllocateOnlyCheckDifference() {
  CreateCustomerSettleCheckSheetVo vo = checkVo("C1", new BigDecimal("101"),
      item("O1", 1), item("O2", 1));
  stubSaleOut("O1", "C1", "50");
  stubSaleOut("O2", "C1", "50");
  service.directApprovePass(vo);
  assertDetailAmounts("50.50", "50.50");
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend && mvn -pl xingyun-settle -Dtest=CustomerSettleCheckSheetServiceImplTest test`

Expected: FAIL，客户对账服务与接口不存在。

- [ ] **Step 3: 实现最小对账服务**

```java
@Transactional(rollbackFor = Exception.class)
public String directApprovePass(CreateCustomerSettleCheckSheetVo vo) {
  List<CheckBiz> bizList = validateAndLoad(vo);
  List<BigDecimal> checkedAmounts = allocateDifference(vo.getCheckAmount(),
      bizList.stream().map(CheckBiz::getOriginalAmount).collect(Collectors.toList()));
  CustomerSettleCheckSheet sheet = saveApprovedSheet(vo);
  List<CustomerSettleCheckSheetDetail> details = buildDetails(sheet.getId(), bizList, checkedAmounts);
  if (!detailService.saveBatch(details)) {
    throw new DefaultClientException("保存客户对账单明细失败！");
  }
  updateSourceSheetsToUnSettle(bizList);
  return sheet.getId();
}
```

`allocateDifference` 使用 `SettleAmountAllocationUtil.allocate(checkAmount, bases)` 取得已分摊差额后的最终金额；该工具内部计算“确认总额减基数合计”的差额。校验每条结果非负且总额等于 `checkAmount`。`validateAndLoad` 批量查询源单，要求状态为 `UN_CHECK_BILL`、客户和类型一致、金额正数。

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend && mvn -pl xingyun-settle -Dtest=CustomerSettleCheckSheetServiceImplTest test`

Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle backend/xingyun-settle/src/main/resources/mappers/CustomerSettleCheckSheetMapper.xml backend/xingyun-settle/src/test/java/com/lframework/xingyun/settle/impl/CustomerSettleCheckSheetServiceImplTest.java
git commit -m "feat: restore customer settlement reconciliation"
```

### Task 2: 以已对账金额驱动客户结算和工作台聚合

**Files:**
- Modify: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/bo/sheet/customer/CustomerSaleSettleInfoBo.java`
- Modify: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/vo/sheet/customer/CreateCustomerSettleSheetVo.java`
- Modify: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/vo/sheet/customer/CustomerSettleSheetItemVo.java`
- Modify: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/service/CustomerSettleSheetService.java`
- Modify: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/impl/CustomerSettleSheetServiceImpl.java`
- Modify: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/controller/CustomerSettleSheetController.java`
- Modify: `backend/xingyun-settle/src/test/java/com/lframework/xingyun/settle/impl/CustomerSettleSheetServiceImplTest.java`
- Modify: `backend/xingyun-settle/src/test/java/com/lframework/xingyun/settle/controller/CustomerSettleSheetControllerTest.java`

**Interfaces:**
- Produces `POST /customer/settle/check/approve/pass/direct` and retains `POST /customer/settle/sheet/approve/pass/direct`.
- `CustomerSaleSettleInfoBo` adds `checkAmount, checkTime, checkDescription, settleTime, settleDescription`.
- `directApprovePass` accepts only `UN_SETTLE` / `PART_SETTLE` source documents and uses each document's checked amount minus prior settled amount as the base.

- [ ] **Step 1: 写失败测试**

```java
@Test(expected = DefaultClientException.class)
public void directApprovePass_shouldRejectUncheckedSource() {
  stubSaleOut("O1", "C1", SettleStatus.UN_CHECK_BILL, "100");
  service.directApprovePass(settleVo("C1", "100", item("O1", 1)));
}

@Test
public void directApprovePass_shouldAllocateOnlySettleDifference() {
  stubCheckedAmount("O1", "60");
  stubCheckedAmount("O2", "40");
  service.directApprovePass(settleVo("C1", "90", item("O1", 1), item("O2", 1)));
  assertSettleDetailAmounts("55.00", "-5.00", "35.00", "-5.00");
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend && mvn -pl xingyun-settle -Dtest=CustomerSettleSheetServiceImplTest,CustomerSettleSheetControllerTest test`

Expected: FAIL，当前实现允许绕过对账并以源单金额或负数退货金额直接结算。

- [ ] **Step 3: 实现聚合和结算校验**

```java
private BigDecimal currentUnSettleAmount(String bizId, BigDecimal checkedAmount,
    Map<String, BigDecimal> settledAmountMap) {
  return checkedAmount.subtract(settledAmountMap.getOrDefault(bizId, BigDecimal.ZERO));
}

private void validateSameCustomerAndBizType(List<DirectSettleBiz> bizList) {
  if (bizList.stream().map(DirectSettleBiz::getCustomerId).distinct().count() != 1
      || bizList.stream().map(DirectSettleBiz::getBizType).distinct().count() != 1) {
    throw new DefaultClientException("一次只能结算同一客户、同一业务类型的单据！");
  }
}
```

工作台以单次对账明细查询按 `bizId` 聚合最近确认对账金额及备注/时间，以结算明细查询按 `bizId` 聚合已结算金额；销售退货 `totalAmount` 使用绝对值。结算明细的 `payAmount` 写入“当前未结算额 + 分摊结算差额”，`discountAmount` 写入该差额。状态更新仍使用 `setPartSettle(id, status, version)` 或 `setSettled(id, status, version)`，返回值非 1 抛出 `DefaultClientException`。

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend && mvn -pl xingyun-settle -Dtest=CustomerSettleSheetServiceImplTest,CustomerSettleSheetControllerTest test`

Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add backend/xingyun-settle/src/main/java backend/xingyun-settle/src/test/java
git commit -m "feat: settle customers from reconciled amounts"
```

### Task 3: 工作台前端支持确认对账与同类型限制

**Files:**
- Create: `frontend/src/api/customer-settle/check/index.ts`
- Create: `frontend/src/api/customer-settle/check/model/createCustomerSettleCheckSheetVo.ts`
- Modify: `frontend/src/api/customer-settle/sheet/model/customerSaleSettleInfoBo.ts`
- Modify: `frontend/src/views/customer-settle/sheet/customerSettleWorkbench.ts`
- Modify: `frontend/src/views/customer-settle/sheet/customerSettleWorkbench.test.ts`
- Modify: `frontend/src/views/customer-settle/sheet/settle.vue`

**Interfaces:**
- `canConfirmCheck(rows)` accepts only same customer, same `bizType`, status `7`.
- `canConfirmSettle(rows)` accepts only same customer, same `bizType`, status `0` or `1`.
- `buildCheckPayload(rows, checkAmount, description)` posts `{ customerId, checkAmount, description, items: [{ bizId, bizType }] }`.

- [ ] **Step 1: 写失败测试**

```ts
it('rejects a selection containing sale-out and sale-return rows', () => {
  expect(canConfirmCheck([{ customerId: 'C1', bizType: 1, settleStatus: 7 },
    { customerId: 'C1', bizType: 2, settleStatus: 7 }])).toBe(false);
});

it('builds a positive check request from pending-check rows', () => {
  expect(buildCheckPayload([{ id: 'O1', customerId: 'C1', bizType: 1 }], 100, '核对完成'))
    .toEqual({ customerId: 'C1', checkAmount: 100, description: '核对完成',
      items: [{ bizId: 'O1', bizType: 1 }] });
});
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd frontend && pnpm exec vitest run src/views/customer-settle/sheet/customerSettleWorkbench.test.ts`

Expected: FAIL，确认对账工具函数不存在且旧逻辑允许跨类型直接结算。

- [ ] **Step 3: 实现工作台交互**

```ts
export function canConfirmCheck(rows: DirectSettleRow[]): boolean {
  return hasOneCustomerAndBizType(rows)
    && rows.every((row) => Number(row.settleStatus) === 7);
}

export function canConfirmSettle(rows: DirectSettleRow[]): boolean {
  return hasOneCustomerAndBizType(rows)
    && rows.every((row) => [0, 1].includes(Number(row.settleStatus)));
}
```

在 `settle.vue` 增加“确认对账”按钮与金额弹窗；初始值为所选 `totalAmount` 合计，提交调用 `customer-settle/check` 的直接确认接口。结算弹窗的初始值为所选 `unSettleAmount` 合计。表格增加对账金额、结算金额、对账/结算时间和备注列；所有金额提示按正数显示。

- [ ] **Step 4: 运行测试确认通过**

Run: `cd frontend && pnpm exec vitest run src/views/customer-settle/sheet/customerSettleWorkbench.test.ts`

Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add frontend/src/api/customer-settle frontend/src/views/customer-settle/sheet
git commit -m "feat: add customer reconciliation workbench action"
```

### Task 4: 记录、导出和回归验证

**Files:**
- Modify: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/excel/sheet/customer/CustomerSaleSettleInfoExportModel.java`
- Modify: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/excel/sheet/customer/CustomerSaleSettleInfoExportTaskWorker.java`
- Create: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/controller/CustomerSettleCheckSheetController.java`
- Create: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/excel/check/customer/CustomerSettleCheckSheetExportModel.java`
- Create: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/excel/check/customer/CustomerSettleCheckSheetExportTaskWorker.java`
- Modify: `frontend/src/views/customer-settle/sheet/record.vue`
- Modify: `frontend/src/router/routes/index.ts`

**Interfaces:**
- 工作台导出字段包含状态、对账金额、结算金额、对账/结算时间和备注。
- 对账记录仅提供查询、详情、导出，不提供旧增改审路由。

- [ ] **Step 1: 写失败测试**

```java
@Test
public void exportSaleSettleInfos_shouldIncludeReconciliationColumns() {
  CustomerSaleSettleInfoExportModel model = new CustomerSaleSettleInfoExportModel();
  model.setCheckAmount(new BigDecimal("100.00"));
  Assert.assertEquals(new BigDecimal("100.00"), model.getCheckAmount());
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend && mvn -pl xingyun-settle -Dtest=CustomerSettleSheetControllerTest test`

Expected: FAIL，导出模型没有对账字段或对账记录端点不存在。

- [ ] **Step 3: 实现记录和导出**

将 `checkAmount/checkTime/checkDescription/settleAmount/settleTime/settleDescription` 映射到工作台导出模型。对账 Controller 只公开 `/customer/settle/check/query`、`/{id}`、`/export-record`、`/approve/pass/direct`；记录页增加“对账记录”入口并只使用这些接口，不引入旧 `add.vue`、`modify.vue`、`approve.vue` 路由。

- [ ] **Step 4: 执行完整回归**

Run: `cd backend && mvn -pl xingyun-settle test`

Expected: PASS。

Run: `cd frontend && pnpm exec vitest run src/views/customer-settle/sheet/customerSettleWorkbench.test.ts`

Expected: PASS。

Run: `cd frontend && pnpm run lint`

Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add backend/xingyun-settle/src/main/java backend/xingyun-settle/src/test/java frontend/src/views/customer-settle frontend/src/router/routes/index.ts
git commit -m "feat: expose customer reconciliation records"
```
