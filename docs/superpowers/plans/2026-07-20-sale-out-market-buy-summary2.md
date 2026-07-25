# 销售出库买菜汇总2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增“买菜汇总2”，按日期、商品和客户动态列导出已勾选销售出库单，且原买菜汇总不变。

**Architecture:** 新增独立前端请求、Controller 路由和 Service 方法。Service 复用既有单据/明细批量查询和 `SummaryRow` 聚合，仅新建动态客户表头、客户数量单元格与行装配逻辑。

**Tech Stack:** Vue 3、TypeScript、Vitest、Spring Boot 2.2.2、Java 8、TestNG、EasyExcel。

## Global Constraints

- 不得改动原 `marketBuySummary`、`/export/marketBuySummary` 及其导出内容。
- 新接口为 JSON POST `/sale/out/sheet/export/marketBuySummary2`，权限 `sale:out:query`。
- 空 `idList` 抛出 `DefaultClientException("请选择要汇总的销售出库单！")`。
- 客户列按首次出现顺序；标题优先 `nickName`，空白时取 `name`。
- 表头严格为：`日期、分类、商品名称、单位、动态客户列、总计`。

---

### Task 1: 前端请求与入口

**Files:**
- Modify: `frontend/src/api/sc/sale/out/index.ts:33-35,224-239`
- Modify: `frontend/src/views/sc/sale/out/components/sheet-list.vue:167-172,924-936`
- Modify: `frontend/src/views/sc/sale/out/components/saleOutMarketBuySummary.ts:1-20`
- Test: `frontend/src/views/sc/sale/out/components/__tests__/saleOutMarketBuySummary.test.ts`

**Interfaces:** Produces `buildMarketBuySummary2Params(records)` and `api.exportMarketBuySummary2(params)`.

- [ ] **Step 1: 写失败测试**

```ts
it('买菜汇总2根据勾选单据构建单据ID筛选条件', () => {
  expect(buildMarketBuySummary2Params([{ id: 'sheet-1' }, { id: undefined }, { id: 'sheet-2' }]))
    .toEqual({ idList: ['sheet-1', 'sheet-2'] });
});
```

- [ ] **Step 2: 确认测试失败**

Run: `pnpm --dir frontend test:unit src/views/sc/sale/out/components/__tests__/saleOutMarketBuySummary.test.ts`

Expected: FAIL，`buildMarketBuySummary2Params` 未导出。

- [ ] **Step 3: 最小化实现**

```ts
export function buildMarketBuySummary2Params(records: SaleOutSheetSelection[]): MarketBuySummaryParams {
  return { idList: (records || []).map((item) => item.id).filter((id): id is string => !!id) };
}

export function exportMarketBuySummary2(params: MarketBuySummaryParams): Promise<void> {
  return defHttp.post<void>(
    { url: baseUrl + '/export/marketBuySummary2', data: params },
    { region, contentType: ContentTypeEnum.JSON, responseType: ResponseEnum.BLOB },
  );
}
```

在原按钮后增加“买菜汇总2”；`marketBuySummary2()` 复用原空选校验与 loading 结构，调用新 API 和新参数构建器。

- [ ] **Step 4: 确认前端测试通过**

Run: `pnpm --dir frontend test:unit src/views/sc/sale/out/components/__tests__/saleOutMarketBuySummary.test.ts`

Expected: PASS。

### Task 2: 客户数量单元格格式化

**Files:**
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetMarketBuySummaryFormatter.java`
- Test: `backend/xingyun-sc/src/test/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetMarketBuySummaryFormatterTest.java`

**Interfaces:** Produces `formatCustomerQuantity(BigDecimal, Collection<String>)`.

- [ ] **Step 1: 写失败测试**

```java
@Test
void formatCustomerQuantityShouldKeepQuantityAndRemarksWithoutCustomerOrUnit() {
    Assert.assertEquals(SaleOutSheetMarketBuySummaryFormatter.formatCustomerQuantity(
            new BigDecimal("0.1"), Arrays.asList("2条大", "2条大", "切好")),
            "0.1（2条大；切好）");
}
```

- [ ] **Step 2: 确认测试失败**

Run: `mvn -pl xingyun-sc -Dtest=SaleOutSheetMarketBuySummaryFormatterTest test`

Expected: FAIL，方法不存在。

- [ ] **Step 3: 最小化实现**

新增包级方法；复用现有数量格式化、备注去重和 `appendDescriptions`：

```java
static String formatCustomerQuantity(BigDecimal orderNum, Collection<String> descriptions) {
    Set<String> remarks = distinctDescriptions(descriptions);
    BigDecimal quantity = orderNum == null ? BigDecimal.ZERO : orderNum;
    if (quantity.compareTo(BigDecimal.ZERO) == 0 && remarks.isEmpty()) return "";
    StringBuilder result = new StringBuilder();
    if (quantity.compareTo(BigDecimal.ZERO) != 0) result.append(formatNumber(quantity));
    appendDescriptions(result, remarks);
    return result.toString();
}
```

- [ ] **Step 4: 确认测试通过**

Run: `mvn -pl xingyun-sc -Dtest=SaleOutSheetMarketBuySummaryFormatterTest test`

Expected: PASS。

### Task 3: 动态表头与独立 Service 导出

**Files:**
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/service/sale/SaleOutSheetService.java:231-235`
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetServiceImpl.java:330-371,595-816`
- Test: `backend/xingyun-sc/src/test/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetMarketBuySummaryFormatterTest.java`
- Test: `backend/xingyun-sc/src/test/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetMarketBuySummarySelectionTest.java`

**Interfaces:** Produces `marketBuySummary2(QuerySaleOutSheetVo vo)` and `buildMarketBuySummary2Headers(LinkedHashMap<String, String>)`.

- [ ] **Step 1: 写失败的动态表头测试**

```java
LinkedHashMap<String, String> customers = new LinkedHashMap<>();
customers.put("customer-1", "机关A");
customers.put("customer-2", "机关B");
Map<String, String> headers = SaleOutSheetServiceImpl.buildMarketBuySummary2Headers(customers);
Assert.assertEquals(new ArrayList<>(headers.keySet()), Arrays.asList(
    "date", "category", "productName", "unit", "customer-customer-1", "customer-customer-2", "total"));
Assert.assertEquals(new ArrayList<>(headers.values()), Arrays.asList(
    "日期", "分类", "商品名称", "单位", "机关A", "机关B", "总计"));
```

- [ ] **Step 2: 确认测试失败**

Run: `mvn -pl xingyun-sc -Dtest=SaleOutSheetMarketBuySummaryFormatterTest test`

Expected: FAIL，动态表头方法不存在。

- [ ] **Step 3: 最小化实现**

在 Service 接口增加 `void marketBuySummary2(QuerySaleOutSheetVo vo);`。实现方法复用 `validateMarketBuySummaryIds`、`query`、`buildCustomerNameMap`、`queryMarketBuySummaryDetails`、`buildProductMap`、`buildCategoryMap`、`buildProductUnitNameMap`、`buildSummaryRows`。

```java
map.put("date", SaleOutSheetMarketBuySummaryFormatter.formatOrderDate(row.orderDate));
map.put("category", row.categoryName);
map.put("productName", row.productName);
map.put("unit", row.unit);
for (String customerId : customerNameMap.keySet()) {
    SummaryCell cell = row.cells.get(customerId);
    map.put("customer-" + customerId, cell == null ? "" :
        SaleOutSheetMarketBuySummaryFormatter.formatCustomerQuantity(cell.orderNum, cell.descriptions));
}
map.put("total", formatNumber(row.total));
```

`buildMarketBuySummary2Headers` 依次加入固定列、`customer-<id>` 客户列和 `total -> 总计`。无单据或无明细也以新表头导出空表。保留原方法原样不动。

- [ ] **Step 4: 补充校验与回退测试**

测试 `resolveCustomerName` 的 `nickName` 优先与空白回退 `name`（现有测试保持）；新增空 `idList` 调用新入口共用校验仍抛出原 `DefaultClientException` 文案。

- [ ] **Step 5: 确认后端单测通过**

Run: `mvn -pl xingyun-sc -Dtest=SaleOutSheetMarketBuySummaryFormatterTest,SaleOutSheetMarketBuySummarySelectionTest test`

Expected: PASS。

### Task 4: Controller 路由与完整验证

**Files:**
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/controller/sale/SaleOutSheetController.java:303-317`

**Interfaces:** Produces `POST /sale/out/sheet/export/marketBuySummary2`.

- [ ] **Step 1: 实现独立 Controller 路由**

```java
@ApiOperation("买菜汇总2导出")
@HasPermission({ "sale:out:query" })
@PostMapping("/export/marketBuySummary2")
public void exportMarketBuySummary2(@RequestBody @Valid QuerySaleOutSheetVo vo) {
    try {
        saleOutSheetService.marketBuySummary2(vo);
    } catch (DefaultClientException e) {
        throw e;
    } catch (Exception e) {
        log.error("导出买菜汇总2失败", e);
        throw new DefaultClientException(e.getMessage());
    }
}
```

- [ ] **Step 2: 运行完整验证**

Run: `mvn -pl xingyun-sc -Dtest=SaleOutSheetMarketBuySummaryFormatterTest,SaleOutSheetMarketBuySummarySelectionTest test`

Expected: PASS。

Run: `mvn -pl xingyun-sc -am test -DskipTests=false`

Expected: PASS。

Run: `pnpm --dir frontend test:unit src/views/sc/sale/out/components/__tests__/saleOutMarketBuySummary.test.ts`

Expected: PASS。

Run: `pnpm --dir frontend type:check`

Expected: exit code 0。

- [ ] **Step 3: 审核并提交**

Run: `git diff --check && git status --short`

Expected: 仅有本计划列出的源文件和测试文件变更，旧买菜汇总行为未改。

```bash
git add frontend/src/api/sc/sale/out/index.ts frontend/src/views/sc/sale/out/components/sheet-list.vue frontend/src/views/sc/sale/out/components/saleOutMarketBuySummary.ts frontend/src/views/sc/sale/out/components/__tests__/saleOutMarketBuySummary.test.ts backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/controller/sale/SaleOutSheetController.java backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/service/sale/SaleOutSheetService.java backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetServiceImpl.java backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetMarketBuySummaryFormatter.java backend/xingyun-sc/src/test/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetMarketBuySummaryFormatterTest.java backend/xingyun-sc/src/test/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetMarketBuySummarySelectionTest.java
git commit -m "feat: add market buy summary2 export"
```
