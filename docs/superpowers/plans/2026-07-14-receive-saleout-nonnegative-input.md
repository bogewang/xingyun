# 采购收货与销售出库非负数校验实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 采购收货和销售出库的单价、数量在新增、修改、录入导入和查询页导入中允许空值或 0，但拒绝负数。

**Architecture:** 前端编辑页在提交前仅拒绝负数，并保留当前“至少一条正数量明细”的提交限制。后端普通保存通过现有 VO 的 `validate()` 校验，Excel 导入通过 Service 的 `checkImportData()` 校验；两者都对非空数值执行非负与精度判断。

**Tech Stack:** Vue 3 Options API、TypeScript、Spring Boot 2.2、Java 8、JUnit 5、EasyExcel。

## Global Constraints

- 单价、数量的 `null`、空字符串与 `0` 合法，任何负数非法。
- 保留既有至少一条正数量明细的单据创建约束；空值或 0 行不提交为明细，避免进入库存变动链路。
- 查询列表页不新增导入 UI、路由或权限。
- 不新增 Controller 业务逻辑；沿用现有 Service 事务边界与输入异常类型。

---

### Task 1: 为普通保存校验建立失败测试

**Files:**
- Create: `xingyun-sc/src/test/java/com/lframework/xingyun/sc/vo/purchase/receive/CreateReceiveSheetVoTest.java`
- Create: `xingyun-sc/src/test/java/com/lframework/xingyun/sc/vo/sale/out/CreateSaleOutSheetVoTest.java`

**Interfaces:**
- Consumes: `CreateReceiveSheetVo.validate(boolean)`、`CreateSaleOutSheetVo.validate(boolean)`
- Produces: 空值/0 通过、负数失败的单元测试

- [ ] **Step 1: 写采购收货的失败测试**

```java
@Test
void shouldAllowNullAndZeroReceiveProductValues() {
  assertDoesNotThrow(() -> validReceiveSheet(receiveProduct(null, null)).validate(false));
  assertDoesNotThrow(() -> validReceiveSheet(receiveProduct(BigDecimal.ZERO, BigDecimal.ZERO)).validate(false));
}
@Test
void shouldRejectNegativeReceiveProductValues() {
  assertThrows(InputErrorException.class,
      () -> validReceiveSheet(receiveProduct(BigDecimal.ONE, new BigDecimal("-0.01"))).validate(false));
}
```

- [ ] **Step 2: 写销售出库的失败测试**

```java
@Test
void shouldAllowNullAndZeroSaleOutProductValues() {
  assertDoesNotThrow(() -> validSaleOutSheet(saleOutProduct(null, null)).validate(false));
  assertDoesNotThrow(() -> validSaleOutSheet(saleOutProduct(BigDecimal.ZERO, BigDecimal.ZERO)).validate(false));
}
@Test
void shouldRejectNegativeSaleOutProductValues() {
  assertThrows(InputErrorException.class,
      () -> validSaleOutSheet(saleOutProduct(new BigDecimal("-0.01"), BigDecimal.ONE)).validate(false));
}
```

- [ ] **Step 3: 验证测试为红**

Run: `mvn -pl xingyun-sc -Dtest=CreateReceiveSheetVoTest,CreateSaleOutSheetVoTest test`

Expected: FAIL；当前实现要求单价、数量非空且大于 0。

### Task 2: 放宽普通保存的 VO 校验

**Files:**
- Modify: `xingyun-sc/src/main/java/com/lframework/xingyun/sc/vo/purchase/receive/CreateReceiveSheetVo.java`
- Modify: `xingyun-sc/src/main/java/com/lframework/xingyun/sc/vo/sale/out/CreateSaleOutSheetVo.java`
- Test: Task 1 的两个测试文件

**Interfaces:**
- Consumes: `purchasePrice`、`receiveNum`、`taxPrice`、`orderNum`
- Produces: 非空值的非负与精度校验

- [ ] **Step 1: 修改采购收货明细规则**

```java
if (product.getReceiveNum() != null) {
  if (NumberUtil.lt(product.getReceiveNum(), BigDecimal.ZERO)) {
    throw new InputErrorException("第" + orderNo + "行商品收货数量不允许小于0！");
  }
  if (!NumberUtil.isNumberPrecision(product.getReceiveNum(), 8)) {
    throw new InputErrorException("第" + orderNo + "行商品收货数量最多允许8位小数！");
  }
}
if (product.getPurchasePrice() != null) {
  if (NumberUtil.lt(product.getPurchasePrice(), BigDecimal.ZERO)) {
    throw new InputErrorException("第" + orderNo + "行商品采购价不允许小于0！");
  }
  if (!NumberUtil.isNumberPrecision(product.getPurchasePrice(), 6)) {
    throw new InputErrorException("第" + orderNo + "行商品采购价最多允许6位小数！");
  }
}
```

- [ ] **Step 2: 对销售出库的 `orderNum`、`taxPrice` 应用同一结构**

删除必填及“大于 0”判断；仅在非空时拒绝负数并执行原精度检查。成本价保留现有的非空校验与非负规则。

- [ ] **Step 3: 验证绿灯并提交**

Run: `mvn -pl xingyun-sc -Dtest=CreateReceiveSheetVoTest,CreateSaleOutSheetVoTest test`

Expected: PASS。

```bash
git add xingyun-sc/src/main/java/com/lframework/xingyun/sc/vo xingyun-sc/src/test/java/com/lframework/xingyun/sc/vo
git commit -m "fix: allow empty or zero sheet product values"
```

### Task 3: 覆盖两类 Excel 导入

**Files:**
- Modify: `xingyun-sc/src/main/java/com/lframework/xingyun/sc/excel/purchase/receive/ReceiveSheetImportModel.java`
- Modify: `xingyun-sc/src/main/java/com/lframework/xingyun/sc/excel/sale/out/SaleOutSheetImportModel.java`
- Modify: `xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/purchase/ReceiveSheetServiceImpl.java`
- Modify: `xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetServiceImpl.java`
- Test: `xingyun-sc/src/test/java/com/lframework/xingyun/sc/impl/purchase/ReceiveSheetServiceImplTest.java`
- Test: `xingyun-sc/src/test/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetServiceImplTest.java`

**Interfaces:**
- Consumes: `checkImport(List<ReceiveSheetImportModel>)`、`checkImport(List<SaleOutSheetImportModel>)`
- Produces: 录入页和查询页导入共用的非负校验

- [ ] **Step 1: 写导入校验的失败测试**

将 `seq=2` 的导入模型送入提取出的包可见纯校验方法，断言空数量和 0 不生成错误，负数量与负单价生成：

```java
assertThat(errors).contains("第2行“数量”不允许小于0");
assertThat(errors).contains("第2行“单价”不允许小于0");
```

- [ ] **Step 2: 验证为红**

Run: `mvn -pl xingyun-sc -Dtest=ReceiveSheetServiceImplTest,SaleOutSheetServiceImplTest test`

Expected: FAIL；当前代码将空数量/0 判为必填或必须大于 0，未校验负单价。

- [ ] **Step 3: 实现导入规则**

- 删除两个 Excel 模型数量字段的 `@ExcelRequired`。
- 在两个 `checkImportData()` 中删除数量必填和 `le(..., ZERO)` 分支。
- 非空数量使用 `lt(..., ZERO)`，错误为 `第{seq}行“数量”不允许小于0`。
- 非空单价增加同样的 `lt(..., ZERO)` 校验，错误为 `第{seq}行“单价”不允许小于0`。
- 保留数量 8 位、单价 6 位小数精度规则和现有空单价默认价格填充。

- [ ] **Step 4: 验证绿灯并提交**

Run: `mvn -pl xingyun-sc -Dtest=ReceiveSheetServiceImplTest,SaleOutSheetServiceImplTest test`

Expected: PASS；查询页导入会调用同一 `checkImport`，无需新 UI 测试。

```bash
git add xingyun-sc/src/main/java/com/lframework/xingyun/sc/excel xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl xingyun-sc/src/test/java/com/lframework/xingyun/sc/impl
git commit -m "fix: reject negative values in sheet imports"
```

### Task 4: 对齐八个编辑页

**Files:**
- Modify: `frontend/src/views/sc/purchase/receive/add-require.vue`
- Modify: `frontend/src/views/sc/purchase/receive/add-un-require.vue`
- Modify: `frontend/src/views/sc/purchase/receive/modify-require.vue`
- Modify: `frontend/src/views/sc/purchase/receive/modify-un-require.vue`
- Modify: `frontend/src/views/sc/sale/out/add-require.vue`
- Modify: `frontend/src/views/sc/sale/out/add-un-require.vue`
- Modify: `frontend/src/views/sc/sale/out/modify-require.vue`
- Modify: `frontend/src/views/sc/sale/out/modify-un-require.vue`

**Interfaces:**
- Consumes: 各页面 `validData()`、`buildParams()`、`isFloatGeZero()`
- Produces: 行级非负校验与现有正数量提交过滤

- [ ] **Step 1: 修改采购收货四页**

把采购价、收货数量的“不能为空/必须大于0”替换为：空值直接通过；非空值必须为数字、不得小于 0、并符合现有精度。错误格式为 `第N行商品采购价不允许小于0！` 或 `第N行商品收货数量不允许小于0！`。

- [ ] **Step 2: 修改销售出库四页**

把价格、出库数量的“不能为空/必须大于0”替换为相同规则，错误格式为 `第N行商品价格不允许小于0！` 或 `第N行商品出库数量不允许小于0！`。

- [ ] **Step 3: 保留提交安全边界**

不改 `buildParams()` 中的 `isFloatGtZero(receiveNum/outNum)` 过滤和“至少一条正数量”检查，确保空值或 0 可编辑但不会形成库存明细。

- [ ] **Step 4: 运行前端检查并提交**

Run: `pnpm --dir frontend run lint`

Expected: PASS。

```bash
git add frontend/src/views/sc/purchase/receive frontend/src/views/sc/sale/out
git commit -m "fix: allow zero sheet product input values"
```

### Task 5: 集成验证

**Files:**
- Modify: 无

- [ ] **Step 1: 运行所有目标测试**

Run: `mvn -pl xingyun-sc -Dtest=CreateReceiveSheetVoTest,CreateSaleOutSheetVoTest,ReceiveSheetServiceImplTest,SaleOutSheetServiceImplTest test`

Expected: PASS。

- [ ] **Step 2: 编译后端**

Run: `mvn clean compile -DskipTests`

Expected: BUILD SUCCESS。

- [ ] **Step 3: 最终前端检查**

Run: `pnpm --dir frontend run lint`

Expected: PASS。

- [ ] **Step 4: 检查并提交**

```bash
git diff --check
git status --short
git add frontend/src/views/sc/purchase/receive frontend/src/views/sc/sale/out xingyun-sc/src/main/java/com/lframework/xingyun/sc/excel xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl xingyun-sc/src/main/java/com/lframework/xingyun/sc/vo xingyun-sc/src/test/java/com/lframework/xingyun/sc
git commit -m "test: verify nonnegative sheet validation"
```
