# 客户结算总览与单客户明细 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将客户结算入口改为按客户汇总的总览，并把单据级对账、结算迁移到新增的单客户明细路由。

**Architecture:** 后端批量读取受数据权限控制的销售出库、退货单据，按客户与结算状态聚合、排序和分页。前端总览只展示统计并跳转；明细页固定使用 URL 的客户 ID 复用既有单据级操作。

**Tech Stack:** Spring Boot 2.2.2、Java 8、MyBatis-Plus、Vue 3、TypeScript、vxe-table、JUnit 4、Vitest。

## Global Constraints

- Controller 仅校验和 `InvokeResult<T>` 包装；业务逻辑放在 Service，业务异常使用 `DefaultClientException`。
- 禁止直接 SQL 和按客户循环查询数据库；沿用带数据权限的销售单 Service 批量查询。
- 仅统计待对账、待结算、部分结算、已结算；退货金额保留负数。
- 总览一客户一行，每个状态均有单据数和金额。
- 单客户明细由 `customerId` 限定，不能在页面中切换客户。

---

## 文件结构

- 创建后端 `CustomerSettleOverviewBo`、`QueryCustomerSettleOverviewVo`、聚合测试和总览导出 Worker。
- 修改 `CustomerSettleSheetService`、`CustomerSettleSheetServiceImpl`、`CustomerSettleSheetController`。
- 创建前端总览 API 类型；修改 API 入口、总览页和路由；创建 `detail.vue` 明细页。
- 修改工作台工具与 Vitest 路由/客户约束测试。

### Task 1: 后端总览聚合

**Files:**
- Create: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/bo/sheet/customer/CustomerSettleOverviewBo.java`
- Create: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/vo/sheet/customer/QueryCustomerSettleOverviewVo.java`
- Modify: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/service/CustomerSettleSheetService.java`
- Modify: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/impl/CustomerSettleSheetServiceImpl.java`
- Test: `backend/xingyun-settle/src/test/java/com/lframework/xingyun/settle/impl/CustomerSettleSheetOverviewServiceImplTest.java`

**Interfaces:**
- Produces: `PageResult<CustomerSettleOverviewBo> querySettleOverviews(QueryCustomerSettleOverviewVo vo)`。

- [ ] **Step 1: 写失败测试并确认失败**

```java
@Test
public void shouldGroupBothBizTypesByCustomerAndStatus() {
  CustomerSettleOverviewBo row = service.querySettleOverviews(queryVo()).getDatas().get(0);
  Assert.assertEquals("customer-1", row.getCustomerId());
  Assert.assertEquals(Integer.valueOf(1), row.getUnCheckCount());
  Assert.assertEquals(new BigDecimal("100.00"), row.getUnCheckAmount());
  Assert.assertEquals(new BigDecimal("-20.00"), row.getUnSettleAmount());
}
```

Run: `cd backend; mvn -pl xingyun-settle -Dtest=CustomerSettleSheetOverviewServiceImplTest test`

Expected: FAIL，缺少总览 BO 或服务方法。

- [ ] **Step 2: 定义聚合契约**

```java
public PageResult<CustomerSettleOverviewBo> querySettleOverviews(
    QueryCustomerSettleOverviewVo vo);

private String customerId;
private String customerCode;
private String customerName;
private Integer unCheckCount; private BigDecimal unCheckAmount;
private Integer unSettleCount; private BigDecimal unSettleAmount;
private Integer partSettleCount; private BigDecimal partSettleAmount;
private Integer settledCount; private BigDecimal settledAmount;
```

VO 继承 `PageVo`，只含可选 `customerId`。

- [ ] **Step 3: 实现最小聚合**

```java
List<SaleOutSheet> saleOutSheets = saleOutSheetService.query(buildSaleOutQuery(vo));
List<SaleReturn> saleReturnSheets = saleReturnService.query(buildSaleReturnQuery(vo));
Map<String, CustomerSettleOverviewBo> rows = new LinkedHashMap<>();
// 两个集合均传入 accumulateOverview；按 customerId 初始化，按状态累加 count 与 totalAmount。
```

两个查询必须透传 `customerId` 且设置 `requireTxIdNull=true`。仅处理 `UN_CHECK_BILL`、`UN_SETTLE`、`PART_SETTLE`、`SETTLED`；空金额为零。批量补齐客户编号、名称，聚合后按客户名排序并按客户行分页。

- [ ] **Step 4: 增加边界测试并确认通过**

```java
@Test public void shouldKeepReturnAmountNegative() { }
@Test public void shouldExcludeTxOccupiedSheets() { }
@Test public void shouldPageGroupedCustomerRows() { }
```

Run: `cd backend; mvn -pl xingyun-settle -Dtest=CustomerSettleSheetOverviewServiceImplTest test`

Expected: PASS。

- [ ] **Step 5: 提交**

Run: `git add backend/xingyun-settle; git commit -m "feat: aggregate customer settlement overview"`

### Task 2: 后端 HTTP 与导出

**Files:**
- Modify: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/controller/CustomerSettleSheetController.java`
- Create: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/excel/sheet/customer/CustomerSettleOverviewExportTaskWorker.java`
- Test: `backend/xingyun-settle/src/test/java/com/lframework/xingyun/settle/controller/CustomerSettleSheetControllerTest.java`

**Interfaces:**
- Produces: `POST /customer/settle/sheet/settle-overviews`、`POST /customer/settle/sheet/export-settle-overviews`。

- [ ] **Step 1: 写 Controller 失败测试**

```java
Mockito.when(service.querySettleOverviews(vo)).thenReturn(pageResult);
InvokeResult<PageResult<CustomerSettleOverviewBo>> result = controller.querySettleOverviews(vo);
Mockito.verify(service).querySettleOverviews(vo);
Assert.assertTrue(result.isSuccess());
```

Run: `cd backend; mvn -pl xingyun-settle -Dtest=CustomerSettleSheetControllerTest test`

Expected: FAIL，方法不存在。

- [ ] **Step 2: 添加接口与导出 Worker**

```java
@HasPermission({"customer-settle:sheet:query"})
@PostMapping("/settle-overviews")
public InvokeResult<PageResult<CustomerSettleOverviewBo>> querySettleOverviews(
    @RequestBody @Valid QueryCustomerSettleOverviewVo vo) {
  try { return InvokeResultBuilder.success(customerSettleSheetService.querySettleOverviews(vo)); }
  catch (Exception e) { log.error("查询客户结算总览失败", e); return InvokeResultBuilder.fail(e.getMessage(), null); }
}
```

导出接口需使用 `customer-settle:sheet:export` 和 `CustomerSettleOverviewExportTaskWorker`，导出客户字段与 8 个统计字段。

- [ ] **Step 3: 运行并提交**

Run: `cd backend; mvn -pl xingyun-settle -Dtest=CustomerSettleSheetControllerTest,CustomerSettleSheetOverviewServiceImplTest test`

Expected: PASS。

Run: `git add backend/xingyun-settle; git commit -m "feat: expose customer settlement overview"`

### Task 3: 前端总览、API 与路由

**Files:**
- Create: `frontend/src/api/customer-settle/sheet/model/customerSettleOverviewBo.ts`
- Create: `frontend/src/api/customer-settle/sheet/model/queryCustomerSettleOverviewVo.ts`
- Modify: `frontend/src/api/customer-settle/sheet/index.ts`
- Modify: `frontend/src/views/customer-settle/sheet/settle.vue`
- Modify: `frontend/src/router/routes/index.ts`
- Modify: `frontend/src/router/routes/customerSettleRoute.test.ts`

- [ ] **Step 1: 写失败路由测试**

```ts
expect(routes.find((r) => r.path === '/settle/customer/sheet')?.name).toBe('CustomerSettleOverview');
expect(routes.find((r) => r.path === '/settle/customer/sheet-detail')?.name).toBe('CustomerSettleDetail');
```

Run: `cd frontend; pnpm test:unit src/router/routes/customerSettleRoute.test.ts`

Expected: FAIL。

- [ ] **Step 2: 新增 API 类型和调用**

```ts
export interface CustomerSettleOverviewBo {
  customerId: string; customerCode: string; customerName: string;
  unCheckCount: number; unCheckAmount: number;
  unSettleCount: number; unSettleAmount: number;
  partSettleCount: number; partSettleAmount: number;
  settledCount: number; settledAmount: number;
}
export function querySettleOverviews(data: QueryCustomerSettleOverviewVo) {
  return defHttp.post<PageResult<CustomerSettleOverviewBo>>(
    { url: baseUrl + '/settle-overviews', data }, { region, contentType: ContentTypeEnum.JSON },
  );
}
```

- [ ] **Step 3: 把 `settle.vue` 改为总览**

保留客户筛选、查询、结算记录、导出；移除复选框、单据筛选、弹窗和所有直接对账/结算调用。表格显示客户基础信息及四状态的 8 个列，页脚累加全部计数、金额列。操作列为：

```ts
this.openChildPage({ path: '/settle/customer/sheet-detail', query: { customerId: row.customerId } });
```

- [ ] **Step 4: 注册路由并验证**

将现有路径命名为 `CustomerSettleOverview`，新增 `CustomerSettleDetail` 指向 `views/customer-settle/sheet/detail.vue`。

Run: `cd frontend; pnpm test:unit src/router/routes/customerSettleRoute.test.ts`

Expected: PASS。

- [ ] **Step 5: 提交**

Run: `git add frontend/src/api/customer-settle frontend/src/views/customer-settle/sheet/settle.vue frontend/src/router/routes; git commit -m "feat: add customer settlement overview page"`

### Task 4: 单客户明细迁移与回归

**Files:**
- Create: `frontend/src/views/customer-settle/sheet/detail.vue`
- Modify: `frontend/src/views/customer-settle/sheet/customerSettleWorkbench.ts`
- Modify: `frontend/src/views/customer-settle/sheet/customerSettleWorkbench.test.ts`

- [ ] **Step 1: 写失败的路由客户约束测试**

```ts
expect(buildCustomerDetailQuery({ customerId: 'C1' }, { customerId: 'C2' }))
  .toMatchObject({ customerId: 'C1' });
expect(validateCustomerDetailRoute({})).toBe('客户参数不能为空！');
```

Run: `cd frontend; pnpm test:unit src/views/customer-settle/sheet/customerSettleWorkbench.test.ts`

Expected: FAIL。

- [ ] **Step 2: 创建固定客户的明细页**

将当前单据级 UI、筛选、对账/结算弹窗迁移到 `detail.vue`。路由参数缺失时显示 `createError('客户参数不能为空！')` 且不请求接口；所有查询和提交都以路由客户 ID 覆盖任何页面值：

```ts
const customerId = String(this.$route.query.customerId || '');
return { ...buildSortPageVo(this.pagerConfig, []), customerId, code, bizType };
```

不提供客户选择器。保留当前混合销售/退货查询、对账、结算、负数金额校验及关联单据跳转。

- [ ] **Step 3: 实现工具并运行回归验证**

增加 `validateCustomerDetailRoute`、`buildCustomerDetailQuery`；保留 `canDirectSettle`、`buildDirectSettlePayload`、`isDirectSettleAmountValid`。补全 payload 测试中 `unSettleAmount` 与 `checkAmount` 的断言。

Run: `cd frontend; pnpm type:check; pnpm test:unit src/router/routes/customerSettleRoute.test.ts src/views/customer-settle/sheet/customerSettleWorkbench.test.ts`

Expected: PASS。

Run: `cd backend; mvn -pl xingyun-settle -Dtest=CustomerSettleSheetOverviewServiceImplTest,CustomerSettleSheetControllerTest,CustomerSettleSheetServiceImplTest test`

Expected: PASS。

- [ ] **Step 4: 检查并提交**

Run: `git diff --check`

Expected: 无输出且退出码为 0。

Run: `git add frontend/src/views/customer-settle/sheet; git commit -m "feat: move customer settlement actions to detail page"`

## Plan Self-Review

- 规格覆盖：Task 1 实现四状态双指标、负数退货与按客户分页；Task 2 提供权限接口和导出；Task 3 实现总览与跳转；Task 4 限定客户明细并回归对账、结算。
- 类型一致性：后端统一使用 `QueryCustomerSettleOverviewVo`、`CustomerSettleOverviewBo`、`querySettleOverviews`；前端统一使用 `/settle/customer/sheet-detail`。
