# 供应商对账与结算金额分摊 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将供应商对账和结算的确认总额准确分摊到每张勾选收货单，并保证明细合计严格等于确认总额。

**Architecture:** 在 `xingyun-settle` 模块新增无状态金额分摊工具，负责两位小数等额差额分摊、尾差补齐和负数校验。供应商对账服务传入货流单金额作为基数，供应商结算服务传入本单未结算金额作为基数；两处均只消费工具返回的明细金额。

**Tech Stack:** Java 8、Spring Boot 2.2.2、Maven、JUnit 4、BigDecimal。

## Global Constraints

- Controller 不承担业务逻辑；金额计算放在后端服务/组件中。
- 业务异常使用 `DefaultClientException`，不得使用 `RuntimeException`。
- 所有新增方法必须添加中文注释。
- 金额按 `BigDecimal` 两位小数处理；不得依赖前端提交的单据明细金额。
- 对账基数为 `bizAmount`，结算基数为 `unSettleAmount`。

---

### Task 1: 新增可复用金额分摊工具及单元测试

**Files:**
- Create: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/utils/SettleAmountAllocationUtil.java`
- Create: `backend/xingyun-settle/src/test/java/com/lframework/xingyun/settle/utils/SettleAmountAllocationUtilTest.java`

**Interfaces:**
- Produces: `SettleAmountAllocationUtil.allocate(BigDecimal confirmedAmount, List<BigDecimal> baseAmounts): List<BigDecimal>`。
- Consumes: `DefaultClientException`。
- Guarantees: 返回列表顺序与基数列表一致、每项非负且合计为 `confirmedAmount.setScale(2, HALF_UP)`。

- [ ] **Step 1: 写入失败测试**

```java
@Test
public void allocate_shouldDistributePositiveDifferenceAndKeepTotal() {
  List<BigDecimal> result = SettleAmountAllocationUtil.allocate(
      new BigDecimal("1004.91"),
      Arrays.asList(new BigDecimal("930.10"), new BigDecimal("569.50"), new BigDecimal("1073.20")));

  Assert.assertEquals(Arrays.asList(new BigDecimal("407.47"), new BigDecimal("46.87"), new BigDecimal("550.57")), result);
  Assert.assertEquals(new BigDecimal("1004.91"), result.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
}

@Test
public void allocate_shouldDistributeNegativeDifferenceAndPutRemainderOnLastItem() {
  List<BigDecimal> result = SettleAmountAllocationUtil.allocate(
      new BigDecimal("99.99"),
      Arrays.asList(new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("50.00")));

  Assert.assertEquals(Arrays.asList(new BigDecimal("33.33"), new BigDecimal("33.33"), new BigDecimal("33.33")), result);
  Assert.assertEquals(new BigDecimal("99.99"), result.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
}

@Test(expected = DefaultClientException.class)
public void allocate_shouldRejectNegativeDetailAmount() {
  SettleAmountAllocationUtil.allocate(
      new BigDecimal("20.00"), Arrays.asList(new BigDecimal("50.00"), new BigDecimal("50.00")));
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend && mvn -pl xingyun-settle -Dtest=SettleAmountAllocationUtilTest test`

Expected: FAIL，提示 `SettleAmountAllocationUtil` 不存在。

- [ ] **Step 3: 实现最小分摊逻辑**

```java
public static List<BigDecimal> allocate(BigDecimal confirmedAmount, List<BigDecimal> baseAmounts) {
  BigDecimal total = normalize(confirmedAmount);
  BigDecimal baseTotal = baseAmounts.stream().map(SettleAmountAllocationUtil::normalize)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
  BigDecimal averageDifference = total.subtract(baseTotal)
      .divide(BigDecimal.valueOf(baseAmounts.size()), 2, RoundingMode.HALF_UP);
  List<BigDecimal> results = new ArrayList<>();
  BigDecimal allocated = BigDecimal.ZERO;
  for (int index = 0; index < baseAmounts.size(); index++) {
    BigDecimal amount = index == baseAmounts.size() - 1
        ? total.subtract(allocated) : normalize(baseAmounts.get(index)).add(averageDifference);
    if (amount.compareTo(BigDecimal.ZERO) < 0) {
      throw new DefaultClientException("确认金额过小，分摊后会出现负数单据，请调整确认金额！");
    }
    results.add(amount);
    allocated = allocated.add(amount);
  }
  return results;
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend && mvn -pl xingyun-settle -Dtest=SettleAmountAllocationUtilTest test`

Expected: PASS。

- [ ] **Step 5: 提交工具与测试**

```bash
git add backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/utils/SettleAmountAllocationUtil.java backend/xingyun-settle/src/test/java/com/lframework/xingyun/settle/utils/SettleAmountAllocationUtilTest.java
git commit -m "fix: add supplier settlement amount allocator"
```

### Task 2: 接入供应商对账与结算分摊

**Files:**
- Modify: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/impl/SettleCheckSheetServiceImpl.java:886-908`
- Modify: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/impl/SettleSheetServiceImpl.java:603-625`
- Test: `backend/xingyun-settle/src/test/java/com/lframework/xingyun/settle/utils/SettleAmountAllocationUtilTest.java`

**Interfaces:**
- Consumes: `SettleAmountAllocationUtil.allocate(BigDecimal, List<BigDecimal>)`。
- Produces: 对账明细 `SettleCheckSheetDetail.payAmount` 与结算明细 `SettleSheetDetail.payAmount` 的正确分摊值。

- [ ] **Step 1: 扩展失败测试，覆盖对账与结算的不同基数**

```java
@Test
public void allocate_shouldUseProvidedUnsettledAmountsForSettlement() {
  List<BigDecimal> result = SettleAmountAllocationUtil.allocate(
      new BigDecimal("120.00"), Arrays.asList(new BigDecimal("50.00"), new BigDecimal("100.00")));

  Assert.assertEquals(Arrays.asList(new BigDecimal("35.00"), new BigDecimal("85.00")), result);
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend && mvn -pl xingyun-settle -Dtest=SettleAmountAllocationUtilTest#allocate_shouldUseProvidedUnsettledAmountsForSettlement test`

Expected: FAIL（在 Task 1 尚未实现时）；若 Task 1 已通过，本步骤改为确认该测试先于服务接入存在。

- [ ] **Step 3: 将两个服务委托给工具**

```java
List<BigDecimal> amounts = SettleAmountAllocationUtil.allocate(
    vo.getCheckAmt(), vo.getItems().stream().map(SettleCheckSheetItemVo::getBizAmount)
        .collect(Collectors.toList()));
for (int index = 0; index < vo.getItems().size(); index++) {
  vo.getItems().get(index).setCheckAmt(amounts.get(index));
}
```

```java
List<BigDecimal> amounts = SettleAmountAllocationUtil.allocate(
    vo.getSettleAmount(), vo.getItems().stream().map(SettleSheetItemVo::getUnSettleAmount)
        .collect(Collectors.toList()));
for (int index = 0; index < vo.getItems().size(); index++) {
  vo.getItems().get(index).setSettleAmount(amounts.get(index));
}
```

- [ ] **Step 4: 运行模块测试与编译**

Run: `cd backend && mvn -pl xingyun-settle -Dtest=SettleAmountAllocationUtilTest test && mvn -pl xingyun-settle -am compile -DskipTests`

Expected: 两个命令均成功。

- [ ] **Step 5: 提交服务接入**

```bash
git add backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/impl/SettleCheckSheetServiceImpl.java backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/impl/SettleSheetServiceImpl.java backend/xingyun-settle/src/test/java/com/lframework/xingyun/settle/utils/SettleAmountAllocationUtilTest.java
git commit -m "fix: allocate supplier check and settlement amounts"
```

### Task 3: 最终验证

**Files:**
- Verify: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/utils/SettleAmountAllocationUtil.java`
- Verify: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/impl/SettleCheckSheetServiceImpl.java`
- Verify: `backend/xingyun-settle/src/main/java/com/lframework/xingyun/settle/impl/SettleSheetServiceImpl.java`

- [ ] **Step 1: 检查差异与格式**

Run: `git diff --check HEAD~2..HEAD && git status --short`

Expected: 无空白错误，工作区干净。

- [ ] **Step 2: 执行完整后端测试**

Run: `cd backend && mvn test`

Expected: PASS。
