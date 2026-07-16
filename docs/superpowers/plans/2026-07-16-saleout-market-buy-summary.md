# 销售出库买菜汇总单列化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `/sale/out` 买菜汇总导出中的客户动态列合并为一个“明细数量”列，并按昵称优先、名称回退展示客户。

**Architecture:** 保留 `SaleOutSheetServiceImpl` 现有的出库单查询、商品批量加载、商品聚合和总计逻辑。新增同包纯格式化器负责客户名称回退和客户明细字符串拼接，服务层只负责按客户首次出现顺序组装格式化器输入并生成固定表头。

**Tech Stack:** Java 8、Spring Boot 2.2.2、MyBatis-Plus 3.4.2、TestNG、Maven。

## Global Constraints

- Controller 只做参数校验和响应包装，业务编排留在 Service 层。
- 业务异常使用 `DefaultClientException`，本次不新增异常分支。
- DAO 只负责数据访问，本次不新增 SQL。
- 客户基础数据使用 `customerService.listByIds` 批量加载，汇总循环内不得逐条查询。
- 生成的方法添加中文注释，使用 import，避免内联全限定类名。
- 保留前端请求地址 `/export/marketBuySummary` 不变。

---

### Task 1: 新增买菜汇总纯格式化器的失败测试

**Files:**
- Create: `xingyun-sc/src/test/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetMarketBuySummaryFormatterTest.java`
- Create later: `xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetMarketBuySummaryFormatter.java`

**Interfaces:**
- Consumes: 待实现的 `SaleOutSheetMarketBuySummaryFormatter.resolveCustomerName(Customer)`、`formatCustomerDetail(String, String, BigDecimal, Collection<String>)`、`mergeCustomerDetails(List<CustomerDetail>)`。
- Produces: 明确的名称回退、数量/单位/备注格式和多客户顺序行为。

- [ ] **Step 1: Write the failing tests**

```java
@Test
void resolveCustomerNameShouldPreferNicknameAndFallbackToName() {
  Customer customer = new Customer();
  customer.setName("客户名称");
  customer.setNickName("客户昵称");
  Assert.assertEquals(SaleOutSheetMarketBuySummaryFormatter.resolveCustomerName(customer), "客户昵称");

  customer.setNickName("  ");
  Assert.assertEquals(SaleOutSheetMarketBuySummaryFormatter.resolveCustomerName(customer), "客户名称");
}

@Test
void formatCustomerDetailShouldAppendUnitAndDeduplicatedRemarks() {
  String result = SaleOutSheetMarketBuySummaryFormatter.formatCustomerDetail(
      "绿春56", "公斤", new BigDecimal("3.5"), Arrays.asList("送老张", "送老张", "分两袋"));
  Assert.assertEquals(result, "(绿春56)3.5/公斤（送老张；分两袋）");
}

@Test
void mergeCustomerDetailsShouldKeepCustomerOrderAndOmitEmptyDetails() {
  List<SaleOutSheetMarketBuySummaryFormatter.CustomerDetail> details = Arrays.asList(
      new SaleOutSheetMarketBuySummaryFormatter.CustomerDetail(
          "绿春56", "公斤", new BigDecimal("1.5"), Collections.emptyList()),
      new SaleOutSheetMarketBuySummaryFormatter.CustomerDetail(
          "平河57", "公斤", new BigDecimal("2"), Collections.singletonList("上午送达")),
      new SaleOutSheetMarketBuySummaryFormatter.CustomerDetail(
          "空客户", "公斤", BigDecimal.ZERO, Collections.emptyList()));

  Assert.assertEquals(
      SaleOutSheetMarketBuySummaryFormatter.mergeCustomerDetails(details),
      "(绿春56)1.5/公斤+(平河57)2/公斤（上午送达）");
}
```

补充一个测试验证昵称为 `null` 和空字符串时都回退到客户名称，并使用 `Customer` 真实对象，不启动 Spring 容器。

- [ ] **Step 2: Run the focused test to verify it fails**

Run: `mvn -pl xingyun-sc -Dtest=SaleOutSheetMarketBuySummaryFormatterTest test`

Expected: FAIL，原因是格式化器类及其方法尚未实现；如果出现编译路径或测试框架错误，先修正测试文件本身后重新运行，不能将编译错误当作 RED 结果。

### Task 2: 实现格式化器并通过纯逻辑测试

**Files:**
- Create: `xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetMarketBuySummaryFormatter.java`
- Test: `xingyun-sc/src/test/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetMarketBuySummaryFormatterTest.java`

**Interfaces:**
- Consumes: 客户对象、客户名称、商品单位、数量和备注集合。
- Produces: 固定格式的单客户文本和 `+` 连接的单列文本。

- [ ] **Step 1: Implement the minimum formatter**

实现以下包可见静态接口：

```java
static String resolveCustomerName(Customer customer)
static String formatCustomerDetail(String customerName, String unit,
    BigDecimal orderNum, Collection<String> descriptions)
static String mergeCustomerDetails(List<CustomerDetail> details)
```

规则：昵称使用 `StringUtils.isNotBlank` 判断；数量去除无意义尾零；备注使用 `LinkedHashSet` 去重并以 `；` 连接；数量非零时输出 `数量/单位`；数量为零但存在备注时仅输出客户和备注；客户明细为空时不参与连接。

- [ ] **Step 2: Run the focused test to verify it passes**

Run: `mvn -pl xingyun-sc -Dtest=SaleOutSheetMarketBuySummaryFormatterTest test`

Expected: PASS，新增测试全部通过。

- [ ] **Step 3: Refactor only after green**

检查格式化器是否重复创建备注集合或重复处理空值；只做不改变行为的命名和结构整理，然后重新运行同一测试命令。

### Task 3: 接入销售出库买菜汇总单列导出

**Files:**
- Modify: `xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetServiceImpl.java:324-368, 509-705`
- Test: `xingyun-sc/src/test/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetMarketBuySummaryFormatterTest.java`

**Interfaces:**
- Consumes: 当前 `marketBuySummary(QuerySaleOutSheetVo)` 查询结果、`SummaryRow` 的客户聚合数据和格式化器接口。
- Produces: 固定表头 `分类、商品名称、单位、明细数量、总计`，单行内按客户首次出现顺序输出客户明细。

- [ ] **Step 1: Write/extend a failing regression assertion for the fixed header shape**

将表头构造抽成包可见静态方法 `buildMarketBuySummaryHeaders()`，先在测试中断言返回键顺序为 `category、productName、unit、detail、total`，运行测试确认当前代码不存在该方法或行为不满足。

- [ ] **Step 2: Implement the minimal service integration**

在 `marketBuySummary` 开始处构造固定表头，不再调用 `buildCustomerColumnMap`；新增 `detail` 表头“明细数量”和 `total` 表头“总计”。将客户映射改为 `LinkedHashMap<String, String>`，键为客户 ID，值为 `SaleOutSheetMarketBuySummaryFormatter.resolveCustomerName(customer)`，保持单据查询顺序。

将每个 `SummaryRow` 的客户单元格转换为格式化器的 `CustomerDetail`，把结果写入 `detail`，删除按客户循环写列的逻辑；`SummaryRow.cells` 仍按客户 ID 聚合数量和去重备注。空单据或空明细也输出固定五列表头。

- [ ] **Step 3: Run focused and existing service tests**

Run: `mvn -pl xingyun-sc -Dtest=SaleOutSheetMarketBuySummaryFormatterTest,SaleOutSheetServiceImplTest test`

Expected: PASS，新增格式化器测试和既有销售出库服务测试均通过。

### Task 4: 完整验证并检查变更范围

**Files:**
- Verify: `xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetMarketBuySummaryFormatter.java`
- Verify: `xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetServiceImpl.java`
- Verify: `xingyun-sc/src/test/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetMarketBuySummaryFormatterTest.java`

- [ ] **Step 1: Run module tests**

Run: `mvn -pl xingyun-sc test`

Expected: Exit code 0，模块测试无失败。

- [ ] **Step 2: Run project compile verification**

Run: `mvn clean compile -DskipTests`

Expected: Exit code 0，项目编译成功。

- [ ] **Step 3: Inspect diff and preserve unrelated files**

Run: `git diff --check; git status --short; git diff --stat`

Expected：只包含买菜汇总实现、测试和计划/设计文档；保留现有未跟踪的 `frontend/dist.zip`，不加入提交。
