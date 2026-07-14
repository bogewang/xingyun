# 商品导入默认单位补齐 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 商品 Excel 导入后，为没有商品单位记录的商品补齐一条换算率为 1 的主单位记录。

**Architecture:** 在 `ProductServiceImpl#importExcel` 完成商品批量新增、更新后，批量读取商品单位和单位字典；仅为未配置任何单位的商品构建默认 `ProductUnit` 并批量写入。已有多单位配置不变。

**Tech Stack:** Java 8、Spring Boot 2.2、MyBatis-Plus 3.4、TestNG、Maven。

## Global Constraints

- 不修改 Excel 模板，不通过 Excel 配置多单位或换算率。
- 仅为没有任何 `base_data_product_unit` 行的商品新增默认单位；不得覆盖既有配置。
- 默认单位为 `conversionRate=BigDecimal.ONE`、`baseUnit=true`、`available=true`、`sortNo=0`。
- 读写使用批量操作，复用 `importExcel` 现有事务。
- 业务异常使用 `DefaultClientException`。

---

### Task 1: 建立默认单位构建规则的测试

**Files:**

- Create: `xingyun-basedata/src/test/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImplTest.java`
- Modify: `xingyun-basedata/pom.xml`

**Interfaces:**

- Consumes: `ProductServiceImpl.buildDefaultProductUnits(List<Product>, Set<String>, Map<String, String>)`。
- Produces: 对默认字段、已有单位保护和单位字典缺失的回归测试。

- [ ] **Step 1: 添加测试依赖和失败测试**

在 `xingyun-basedata/pom.xml` 的 `<dependencies>` 增加：

```xml
<dependency>
  <groupId>org.testng</groupId>
  <artifactId>testng</artifactId>
  <scope>test</scope>
</dependency>
```

新建 `ProductServiceImplTest`，使用以下三个测试调用尚不存在的构建方法：

```java
@Test
void shouldCreateBaseUnitForUnconfiguredProduct() {
  List<ProductUnit> units = ProductServiceImpl.buildDefaultProductUnits(
      Collections.singletonList(product("product-1", "unit-1")), Collections.emptySet(),
      Collections.singletonMap("unit-1", "瓶"));
  ProductUnit unit = units.get(0);
  Assert.assertEquals(unit.getProductId(), "product-1");
  Assert.assertEquals(unit.getUnitName(), "瓶");
  Assert.assertEquals(unit.getConversionRate(), BigDecimal.ONE);
  Assert.assertTrue(unit.getBaseUnit());
  Assert.assertTrue(unit.getAvailable());
  Assert.assertEquals(unit.getSortNo(), Integer.valueOf(0));
}

@Test
void shouldNotCreateUnitForConfiguredProduct() {
  List<ProductUnit> units = ProductServiceImpl.buildDefaultProductUnits(
      Collections.singletonList(product("product-1", "unit-1")), Collections.singleton("product-1"),
      Collections.singletonMap("unit-1", "瓶"));
  Assert.assertTrue(units.isEmpty());
}

@Test(expectedExceptions = DefaultClientException.class)
void shouldRejectMissingUnitDictionary() {
  ProductServiceImpl.buildDefaultProductUnits(Collections.singletonList(product("product-1", "unit-1")),
      Collections.emptySet(), Collections.emptyMap());
}
```

- [ ] **Step 2: 运行失败测试**

Run: `mvn -pl xingyun-basedata -Dtest=ProductServiceImplTest test`

Expected: 编译失败，提示 `buildDefaultProductUnits` 不存在。

- [ ] **Step 3: 提交测试基线**

```powershell
git add xingyun-basedata/pom.xml xingyun-basedata/src/test/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImplTest.java
git commit -m "test: cover product import default units"
```

### Task 2: 在导入事务内批量补齐单位

**Files:**

- Modify: `xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImpl.java:539-553`
- Modify: `xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImpl.java:948-1000`
- Test: `xingyun-basedata/src/test/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImplTest.java`

**Interfaces:**

- Produces: 包可见静态方法 `buildDefaultProductUnits(List<Product> products, Set<String> configuredProductIds, Map<String, String> unitNames)`。

- [ ] **Step 1: 实现最小构建方法使测试通过**

在 `ProductServiceImpl` 添加：

```java
static List<ProductUnit> buildDefaultProductUnits(List<Product> products,
    Set<String> configuredProductIds, Map<String, String> unitNames) {
  List<ProductUnit> units = new ArrayList<>();
  for (Product product : products) {
    if (configuredProductIds.contains(product.getId())) continue;
    String unitName = unitNames.get(product.getUnit());
    if (StringUtil.isBlank(product.getUnit()) || StringUtil.isBlank(unitName)) {
      throw new DefaultClientException("主单位不存在或已停用！");
    }
    ProductUnit unit = new ProductUnit();
    unit.setId(IdUtil.getId());
    unit.setProductId(product.getId());
    unit.setUnitName(unitName);
    unit.setConversionRate(BigDecimal.ONE);
    unit.setBaseUnit(Boolean.TRUE);
    unit.setAvailable(Boolean.TRUE);
    unit.setSortNo(0);
    units.add(unit);
  }
  return units;
}
```

- [ ] **Step 2: 确认测试转绿**

Run: `mvn -pl xingyun-basedata -Dtest=ProductServiceImplTest test`

Expected: `Tests run: 3, Failures: 0`。

- [ ] **Step 3: 从 `importExcel` 接入批量查询与写入**

在商品批量新增/更新后调用私有 `saveDefaultUnits`。该方法合并新增与更新商品，空集合直接返回；使用 `productUnitService.list(...in(ProductUnit::getProductId, productIds))` 收集已配置商品 ID；使用 `unitService.list(...in(Unit::getId, unitIds).eq(Unit::getAvailable, Boolean.TRUE))` 构建 ID 到名称映射；调用构建方法并在非空时 `productUnitService.saveBatch(defaultUnits)`。

- [ ] **Step 4: 编译和模块测试**

Run: `mvn -pl xingyun-basedata -Dtest=ProductServiceImplTest test`

Expected: `BUILD SUCCESS`，3 个测试通过。

Run: `mvn -pl xingyun-basedata -am compile -DskipTests`

Expected: `BUILD SUCCESS`。

- [ ] **Step 5: 提交实现**

```powershell
git add xingyun-basedata/pom.xml xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImpl.java xingyun-basedata/src/test/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImplTest.java
git commit -m "fix: create default units for imported products"
```

### Task 3: 最终回归

**Files:**

- Verify only: `xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImpl.java`
- Verify only: `xingyun-basedata/src/test/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImplTest.java`

- [ ] **Step 1: 执行模块全部测试**

Run: `mvn -pl xingyun-basedata test`

Expected: `BUILD SUCCESS`。

- [ ] **Step 2: 检查变更完整性**

Run: `git diff --check HEAD~1..HEAD` and `git status --short`

Expected: 无空白错误，且仅有预期实现、测试和依赖变更。
