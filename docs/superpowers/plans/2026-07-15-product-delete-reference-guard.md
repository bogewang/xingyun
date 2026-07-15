# 商品删除单据引用保护 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 阻止已被采购订单、采购收货单、销售订单或销售出库单引用的商品被逻辑删除。

**Architecture:** 在 `xingyun-core` 提供面向基础数据模块的引用检查接口，`xingyun-sc` 用四个单据明细 Mapper 实现该接口。商品 Service 在更新 `available=false` 前调用接口；任何一个明细表命中即抛出受控业务异常，避免跨模块循环依赖。

**Tech Stack:** Java 8、Spring Boot 2.2.2、MyBatis-Plus 3.4.2、TestNG。

## Global Constraints

- Controller 只做参数校验和响应包装，删除约束必须位于 Service 层。
- 业务失败必须抛出 `DefaultClientException`，禁止使用 `RuntimeException`。
- 不新增直接 SQL；引用判断使用 MyBatis-Plus `QueryWrapper`。
- 基础数据模块不可依赖仓储模块；跨模块契约置于 `xingyun-core`。
- 新增方法均添加中文注释。

---

### Task 1: 定义跨模块商品删除引用检查契约

**Files:**
- Create: `xingyun-core/src/main/java/com/lframework/xingyun/core/service/ProductDeleteReferenceChecker.java`
- Modify: `xingyun-core/pom.xml`（添加 `org.testng:testng:6.14.3` 测试依赖）
- Test: `xingyun-core/src/test/java/com/lframework/xingyun/core/service/ProductDeleteReferenceCheckerTest.java`

**Interfaces:**
- Produces: `boolean ProductDeleteReferenceChecker.isReferenced(String productId)`，供基础数据模块在删除商品前调用。

- [ ] **Step 1: 写入契约编译测试**

```java
package com.lframework.xingyun.core.service;

import org.testng.Assert;
import org.testng.annotations.Test;

class ProductDeleteReferenceCheckerTest {

  @Test
  void shouldExposeReferenceCheckMethod() throws NoSuchMethodException {
    Assert.assertEquals(ProductDeleteReferenceChecker.class
        .getMethod("isReferenced", String.class).getReturnType(), Boolean.TYPE);
  }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl xingyun-core -Dtest=ProductDeleteReferenceCheckerTest test`

Expected: 编译失败，提示 `ProductDeleteReferenceChecker` 不存在。

- [ ] **Step 3: 编写最小契约**

```java
package com.lframework.xingyun.core.service;

/**
 * 商品删除前的跨模块引用检查。
 */
public interface ProductDeleteReferenceChecker {

  /**
   * 判断商品是否仍被业务单据引用。
   *
   * @param productId 商品 ID
   * @return 已引用时返回 true
   */
  boolean isReferenced(String productId);
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -pl xingyun-core -Dtest=ProductDeleteReferenceCheckerTest test`

Expected: `Tests run: 1, Failures: 0`。

- [ ] **Step 5: 提交契约**

```bash
git add xingyun-core/pom.xml xingyun-core/src/main/java/com/lframework/xingyun/core/service/ProductDeleteReferenceChecker.java xingyun-core/src/test/java/com/lframework/xingyun/core/service/ProductDeleteReferenceCheckerTest.java
git commit -m "feat: add product delete reference checker contract"
```

### Task 2: 实现四类单据明细引用检查

**Files:**
- Create: `xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/ProductDeleteReferenceCheckerImpl.java`
- Modify: `xingyun-sc/pom.xml`（添加 `org.mockito:mockito-core:3.9.0` 测试依赖）
- Test: `xingyun-sc/src/test/java/com/lframework/xingyun/sc/impl/ProductDeleteReferenceCheckerImplTest.java`

**Interfaces:**
- Consumes: `ProductDeleteReferenceChecker.isReferenced(String productId)`。
- Produces: Spring `@Service` 实现；采购订单、采购收货单、销售订单、销售出库单任一明细表含该 `product_id` 时返回 `true`。

- [ ] **Step 1: 写入失败的命中规则测试**

```java
@Test(dataProvider = "referencedDocumentCounts")
void shouldReportReferencedWhenAnyDocumentDetailExists(int purchaseOrderCount,
    int receiveSheetCount, int saleOrderCount, int saleOutSheetCount) {
  PurchaseOrderDetailMapper purchaseOrderMapper = Mockito.mock(PurchaseOrderDetailMapper.class);
  ReceiveSheetDetailMapper receiveSheetMapper = Mockito.mock(ReceiveSheetDetailMapper.class);
  SaleOrderDetailMapper saleOrderMapper = Mockito.mock(SaleOrderDetailMapper.class);
  SaleOutSheetDetailMapper saleOutSheetMapper = Mockito.mock(SaleOutSheetDetailMapper.class);
  Mockito.when(purchaseOrderMapper.selectCount(Mockito.any())).thenReturn(purchaseOrderCount);
  Mockito.when(receiveSheetMapper.selectCount(Mockito.any())).thenReturn(receiveSheetCount);
  Mockito.when(saleOrderMapper.selectCount(Mockito.any())).thenReturn(saleOrderCount);
  Mockito.when(saleOutSheetMapper.selectCount(Mockito.any())).thenReturn(saleOutSheetCount);
  ProductDeleteReferenceCheckerImpl checker = new ProductDeleteReferenceCheckerImpl(
      purchaseOrderMapper, receiveSheetMapper, saleOrderMapper, saleOutSheetMapper);

  Assert.assertTrue(checker.isReferenced("product-1"));
}

@DataProvider
Object[][] referencedDocumentCounts() {
  return new Object[][] {{1, 0, 0, 0}, {0, 1, 0, 0},
      {0, 0, 1, 0}, {0, 0, 0, 1}};
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl xingyun-sc -am -Dtest=ProductDeleteReferenceCheckerImplTest test`

Expected: 编译失败，提示 `ProductDeleteReferenceCheckerImpl` 不存在。

- [ ] **Step 3: 实现最小检查器**

```java
@Service
public class ProductDeleteReferenceCheckerImpl implements ProductDeleteReferenceChecker {

  private final PurchaseOrderDetailMapper purchaseOrderDetailMapper;
  private final ReceiveSheetDetailMapper receiveSheetDetailMapper;
  private final SaleOrderDetailMapper saleOrderDetailMapper;
  private final SaleOutSheetDetailMapper saleOutSheetDetailMapper;

  @Autowired
  public ProductDeleteReferenceCheckerImpl(PurchaseOrderDetailMapper purchaseOrderDetailMapper,
      ReceiveSheetDetailMapper receiveSheetDetailMapper, SaleOrderDetailMapper saleOrderDetailMapper,
      SaleOutSheetDetailMapper saleOutSheetDetailMapper) {
    this.purchaseOrderDetailMapper = purchaseOrderDetailMapper;
    this.receiveSheetDetailMapper = receiveSheetDetailMapper;
    this.saleOrderDetailMapper = saleOrderDetailMapper;
    this.saleOutSheetDetailMapper = saleOutSheetDetailMapper;
  }

  @Override
  public boolean isReferenced(String productId) {
    return purchaseOrderDetailMapper.selectCount(Wrappers.lambdaQuery(PurchaseOrderDetail.class)
        .eq(PurchaseOrderDetail::getProductId, productId)) > 0
        || receiveSheetDetailMapper.selectCount(Wrappers.lambdaQuery(ReceiveSheetDetail.class)
        .eq(ReceiveSheetDetail::getProductId, productId)) > 0
        || saleOrderDetailMapper.selectCount(Wrappers.lambdaQuery(SaleOrderDetail.class)
        .eq(SaleOrderDetail::getProductId, productId)) > 0
        || saleOutSheetDetailMapper.selectCount(Wrappers.lambdaQuery(SaleOutSheetDetail.class)
        .eq(SaleOutSheetDetail::getProductId, productId)) > 0;
  }
}
```

```java
@Test
void shouldReportNotReferencedWhenAllDocumentDetailsAreAbsent() {
  PurchaseOrderDetailMapper purchaseOrderMapper = Mockito.mock(PurchaseOrderDetailMapper.class);
  ReceiveSheetDetailMapper receiveSheetMapper = Mockito.mock(ReceiveSheetDetailMapper.class);
  SaleOrderDetailMapper saleOrderMapper = Mockito.mock(SaleOrderDetailMapper.class);
  SaleOutSheetDetailMapper saleOutSheetMapper = Mockito.mock(SaleOutSheetDetailMapper.class);
  Mockito.when(purchaseOrderMapper.selectCount(Mockito.any())).thenReturn(0);
  Mockito.when(receiveSheetMapper.selectCount(Mockito.any())).thenReturn(0);
  Mockito.when(saleOrderMapper.selectCount(Mockito.any())).thenReturn(0);
  Mockito.when(saleOutSheetMapper.selectCount(Mockito.any())).thenReturn(0);

  ProductDeleteReferenceCheckerImpl checker = new ProductDeleteReferenceCheckerImpl(
      purchaseOrderMapper, receiveSheetMapper, saleOrderMapper, saleOutSheetMapper);

  Assert.assertFalse(checker.isReferenced("product-1"));
  Mockito.verify(saleOutSheetMapper).selectCount(Mockito.any());
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -pl xingyun-sc -am -Dtest=ProductDeleteReferenceCheckerImplTest test`

Expected: 所有四个单据来源及无引用场景通过。

- [ ] **Step 5: 提交检查器**

```bash
git add xingyun-sc/pom.xml xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/ProductDeleteReferenceCheckerImpl.java xingyun-sc/src/test/java/com/lframework/xingyun/sc/impl/ProductDeleteReferenceCheckerImplTest.java
git commit -m "feat: check product references before deletion"
```

### Task 3: 在商品删除前执行引用保护

**Files:**
- Modify: `xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImpl.java:141-150`
- Modify: `xingyun-basedata/pom.xml`（添加 `org.mockito:mockito-core:3.9.0` 测试依赖）
- Modify: `xingyun-basedata/src/test/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImplTest.java`

**Interfaces:**
- Consumes: `ProductDeleteReferenceChecker.isReferenced(String productId)`。
- Produces: `ProductServiceImpl.deleteById(String id)` 在存在引用时抛出 `DefaultClientException("商品已被采购或销售单据引用，不能删除！")`，且不执行商品状态更新。

- [ ] **Step 1: 写入商品删除失败测试**

```java
@Test(expectedExceptions = DefaultClientException.class,
    expectedExceptionsMessageRegExp = "商品已被采购或销售单据引用，不能删除！")
void shouldRejectDeleteWhenProductIsReferenced() {
  ProductDeleteReferenceChecker checker = Mockito.mock(ProductDeleteReferenceChecker.class);
  Mockito.when(checker.isReferenced("product-1")).thenReturn(true);

  ProductServiceImpl service = new ProductServiceImpl();
  Field field = ProductServiceImpl.class.getDeclaredField("productDeleteReferenceChecker");
  field.setAccessible(true);
  field.set(service, checker);
  service.deleteById("product-1");
}
```

测试方法声明 `throws Exception`，并验证 `checker.isReferenced("product-1")` 被调用。该测试不注入 Mapper；若代码越过引用检查而尝试状态更新，会因 Mapper 未注入而无法得到预期业务异常，从而证明状态更新未发生在引用检查之前。

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl xingyun-basedata -am -Dtest=ProductServiceImplTest test`

Expected: 测试失败，因为删除逻辑尚未调用引用检查器。

- [ ] **Step 3: 在更新状态前增加检查**

```java
@Autowired
private ProductDeleteReferenceChecker productDeleteReferenceChecker;

@Override
public void deleteById(String id) {
  if (productDeleteReferenceChecker.isReferenced(id)) {
    throw new DefaultClientException("商品已被采购或销售单据引用，不能删除！");
  }
  Wrapper<Product> updateWrapper = Wrappers.lambdaUpdate(Product.class)
      .set(Product::getAvailable, Boolean.FALSE).eq(Product::getId, id);
  getBaseMapper().update(updateWrapper);
  Product product = this.findById(id);
  DataChangeEventBuilder.publishLogicDelete(this, DeleteProductEvent.class, product);
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -pl xingyun-basedata -am -Dtest=ProductServiceImplTest test`

Expected: 既有默认单位测试与新增删除保护测试均通过。

- [ ] **Step 5: 执行模块编译和提交**

Run: `mvn clean compile -DskipTests`

Expected: 所有 Maven 模块编译成功。

```bash
git add xingyun-basedata/pom.xml xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImpl.java xingyun-basedata/src/test/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImplTest.java
git commit -m "feat: prevent deleting referenced products"
```

### Task 4: 回归验证

**Files:**
- Modify: 无。

**Interfaces:**
- Consumes: Tasks 1-3 的完整实现。
- Produces: 可复现的构建与测试结果。

- [ ] **Step 1: 运行相关模块测试**

Run: `mvn -pl xingyun-core,xingyun-sc,xingyun-basedata -am test`

Expected: Maven 退出码为 `0`，无测试失败。

- [ ] **Step 2: 运行完整编译**

Run: `mvn clean compile -DskipTests`

Expected: Maven 退出码为 `0`。

- [ ] **Step 3: 检查工作区变更**

Run: `git status --short && git diff --check HEAD`

Expected: 除用户已有的未跟踪 `.codex/` 外，无未提交的实现文件和空白错误。
