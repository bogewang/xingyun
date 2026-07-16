# 商品物理删除 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 仅允许删除未被采购、入库、销售出库或库存数据引用的商品，并将删除改为物理删除。

**Architecture:** 在基础数据模块定义商品引用检查扩展点，避免 `xingyun-basedata` 反向依赖 `xingyun-sc`。仓库模块实现该扩展点，通过 MyBatis-Plus `QueryWrapper` 检查五类实体；商品服务在物理删除前调用检查器，命中引用即抛出业务异常。

**Tech Stack:** Java 8、Spring Boot、MyBatis-Plus 3.4.2、TestNG。

## Global Constraints

- Controller 不承载业务逻辑，删除校验放在 Service 层。
- 业务拒绝必须抛出 `DefaultClientException`。
- 数据库访问只使用 MyBatis-Plus `QueryWrapper`，不新增内联 SQL。
- `@Transactional` 保留在 `ProductServiceImpl#deleteById`。
- 所有新增方法添加中文注释。

---

### Task 1: 定义跨模块商品引用检查扩展点

**Files:**
- Create: `xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/service/product/ProductReferenceChecker.java`
- Modify: `xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImpl.java:47-149`
- Test: `xingyun-basedata/src/test/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImplTest.java`

**Interfaces:**
- Produces: `boolean ProductReferenceChecker.hasReference(String productId)`，由业务模块实现。
- Consumes: `List<ProductReferenceChecker>`；空列表表示未注册业务引用检查器。

- [ ] **Step 1: 写出失败测试**

```java
@Test(expectedExceptions = DefaultClientException.class,
    expectedExceptionsMessageRegExp = "商品已被业务单据或库存数据引用，无法删除！")
void shouldRejectDeleteWhenProductHasBusinessReference() {
  ProductServiceImpl.assertNoProductReference("product-1",
      Collections.singletonList(productId -> true));
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `mvn -pl xingyun-basedata -am test "-Dtest=ProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false"`

Expected: 编译失败，提示找不到 `ProductReferenceChecker` 或 `assertNoProductReference`。

- [ ] **Step 3: 实现最小扩展点与拒绝逻辑**

```java
/** 商品业务引用检查扩展点。 */
public interface ProductReferenceChecker {

  /**
   * 判断商品是否被当前业务模块的数据引用。
   *
   * @param productId 商品 ID
   * @return 已被引用时返回 true
   */
  boolean hasReference(String productId);
}

/** 校验商品未被业务数据引用。 */
static void assertNoProductReference(String productId,
    List<ProductReferenceChecker> productReferenceCheckers) {
  if (productReferenceCheckers.stream().anyMatch(checker -> checker.hasReference(productId))) {
    throw new DefaultClientException("商品已被业务单据或库存数据引用，无法删除！");
  }
}
```

在 `ProductServiceImpl` 注入 `List<ProductReferenceChecker> productReferenceCheckers`。

- [ ] **Step 4: 运行测试，确认通过**

Run: `mvn -pl xingyun-basedata -am test "-Dtest=ProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false"`

Expected: `ProductServiceImplTest` 全部通过。

- [ ] **Step 5: 提交**

```bash
git add xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/service/product/ProductReferenceChecker.java xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImpl.java xingyun-basedata/src/test/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImplTest.java
git commit -m "feat: add product reference check extension"
```

### Task 2: 实现仓库业务引用检查器

**Files:**
- Create: `xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/product/ScProductReferenceChecker.java`
- Test: `xingyun-sc/src/test/java/com/lframework/xingyun/sc/impl/product/ScProductReferenceCheckerTest.java`

**Interfaces:**
- Consumes: `ProductReferenceChecker#hasReference(String)`。
- Produces: 对 `PurchaseOrderDetail`、`ReceiveSheetDetail`、`SaleOutSheetDetail`、`ProductStock`、`ProductStockLog` 的存在性判断。

- [ ] **Step 1: 写出失败测试**

```java
@Test
void shouldReturnTrueWhenAnyReferenceExists() {
  Assert.assertTrue(ScProductReferenceChecker.hasReference(
      Arrays.asList(0L, 0L, 1L, 0L, 0L)));
}

@Test
void shouldReturnFalseWhenNoReferenceExists() {
  Assert.assertFalse(ScProductReferenceChecker.hasReference(
      Arrays.asList(0L, 0L, 0L, 0L, 0L)));
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `mvn -pl xingyun-sc -am test "-Dtest=ScProductReferenceCheckerTest" "-Dsurefire.failIfNoSpecifiedTests=false"`

Expected: 编译失败，提示找不到 `ScProductReferenceChecker`。

- [ ] **Step 3: 使用五个 Mapper 的 `selectCount` 实现检查器**

```java
/** 仓库业务中的商品引用检查器。 */
@Service
public class ScProductReferenceChecker implements ProductReferenceChecker {

  /** 判断任一引用计数是否大于零。 */
  static boolean hasReference(List<Long> referenceCounts) {
    return referenceCounts.stream().anyMatch(count -> count > 0L);
  }

  /** 判断商品是否被采购、入库、销售出库或库存数据引用。 */
  @Override
  public boolean hasReference(String productId) {
    return hasReference(Arrays.asList(
        purchaseOrderDetailMapper.selectCount(Wrappers.lambdaQuery(PurchaseOrderDetail.class)
            .eq(PurchaseOrderDetail::getProductId, productId)),
        receiveSheetDetailMapper.selectCount(Wrappers.lambdaQuery(ReceiveSheetDetail.class)
            .eq(ReceiveSheetDetail::getProductId, productId)),
        saleOutSheetDetailMapper.selectCount(Wrappers.lambdaQuery(SaleOutSheetDetail.class)
            .eq(SaleOutSheetDetail::getProductId, productId)),
        productStockMapper.selectCount(Wrappers.lambdaQuery(ProductStock.class)
            .eq(ProductStock::getProductId, productId)),
        productStockLogMapper.selectCount(Wrappers.lambdaQuery(ProductStockLog.class)
            .eq(ProductStockLog::getProductId, productId))));
  }
}
```

使用构造器注入五个 Mapper，并为构造器添加中文注释。

- [ ] **Step 4: 运行测试，确认通过**

Run: `mvn -pl xingyun-sc -am test "-Dtest=ScProductReferenceCheckerTest" "-Dsurefire.failIfNoSpecifiedTests=false"`

Expected: `ScProductReferenceCheckerTest` 全部通过。

- [ ] **Step 5: 提交**

```bash
git add xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/product/ScProductReferenceChecker.java xingyun-sc/src/test/java/com/lframework/xingyun/sc/impl/product/ScProductReferenceCheckerTest.java
git commit -m "feat: block referenced product deletion"
```

### Task 3: 接入物理删除并验证回归

**Files:**
- Modify: `xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImpl.java:141-149`
- Modify: `xingyun-basedata/src/test/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImplTest.java`

**Interfaces:**
- Consumes: `ProductReferenceChecker#hasReference(String)`。
- Produces: `ProductService#deleteById(String)` 在无引用时删除商品物理记录。

- [ ] **Step 1: 写出失败测试**

```java
@Test
void shouldAllowDeleteWhenNoProductReferenceExists() {
  ProductServiceImpl.assertNoProductReference("product-1",
      Collections.singletonList(productId -> false));
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `mvn -pl xingyun-basedata -am test "-Dtest=ProductServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false"`

Expected: 在 Task 1 尚未完成时编译失败；Task 1 完成后该测试通过。

- [ ] **Step 3: 修改 `deleteById` 为校验后物理删除**

```java
assertNoProductReference(id, productReferenceCheckers);
Product product = getBaseMapper().selectById(id);
getBaseMapper().deleteById(id);
DataChangeEventBuilder.publishLogicDelete(this, DeleteProductEvent.class, product);
cleanCacheByKey(id);
```

删除当前更新 `Product::getAvailable` 的 `LambdaUpdateWrapper`。保留现有删除事件类型，以避免破坏依赖该事件的监听器；物理删除由 Mapper 调用保证。

- [ ] **Step 4: 执行模块回归与编译验证**

Run: `mvn -pl xingyun-basedata,xingyun-sc -am test "-Dtest=ProductServiceImplTest,ScProductReferenceCheckerTest" "-Dsurefire.failIfNoSpecifiedTests=false"`

Expected: 两个测试类全部通过，Maven reactor 以 `BUILD SUCCESS` 结束。

- [ ] **Step 5: 提交**

```bash
git add xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImpl.java xingyun-basedata/src/test/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImplTest.java
git commit -m "feat: physically delete unreferenced products"
```

