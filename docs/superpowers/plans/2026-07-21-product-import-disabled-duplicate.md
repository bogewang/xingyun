# 商品导入停用重复商品处理 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 导入商品时忽略与已停用商品名称、规格、单位完全重复的行，并在导入弹窗说明该规则。

**Architecture:** `ProductServiceImpl` 在既有校验前批量查询同名停用商品，并调用一个包可见的纯过滤方法移除三元组重复的导入行。过滤后的列表继续沿用既有校验、构建与批量持久化流程；前端仅扩展 `ProductImporter` 的提示文案。

**Tech Stack:** Java 8、Spring Boot、MyBatis-Plus、TestNG、Vue 3、ant-design-vue。

## Global Constraints

- 仅名称+规格+单位匹配停用商品时忽略；匹配启用商品时保持既有行为。
- 停用重复行不进行后续字段校验，也不写入商品或商品单位数据。
- 数据库查询必须使用 MyBatis-Plus `QueryWrapper`，禁止逐行查询。
- 所有新增 Java 方法添加中文注释。
- 通过 TestNG 回归测试和前端 lint 验证。

---

### Task 1: 后端过滤规则与回归测试

**Files:**
- Modify: `backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImpl.java:680-696,1191-1199`
- Modify: `backend/xingyun-basedata/src/test/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImplTest.java`

**Interfaces:**
- Consumes: `ProductImportModel#getName()`, `getSpec()`, `getUnit()`；`Product#getAvailable()`。
- Produces: `static List<ProductImportModel> filterDisabledDuplicateImportRows(List<ProductImportModel> importRows, List<Product> disabledProducts)`，返回未匹配停用商品的原始导入行。

- [ ] **Step 1: 写入失败测试**

在 `ProductServiceImplTest` 添加如下测试和辅助构造方法：

```java
@Test
void shouldIgnoreImportRowMatchingDisabledProductByNameSpecAndUnit() {
  ProductImportModel ignored = importModel("可乐", "500ml", "瓶");
  ProductImportModel retained = importModel("雪碧", "500ml", "瓶");
  Product disabled = product("disabled-1", "unit-1");
  disabled.setName(" 可乐 ");
  disabled.setSpec("500ml");
  disabled.setUnit("瓶");
  disabled.setAvailable(Boolean.FALSE);

  List<ProductImportModel> actual = ProductServiceImpl.filterDisabledDuplicateImportRows(
      Arrays.asList(ignored, retained), Collections.singletonList(disabled));

  Assert.assertEquals(actual, Collections.singletonList(retained));
}

@Test
void shouldRetainImportRowMatchingAvailableProductByNameSpecAndUnit() {
  ProductImportModel row = importModel("可乐", "500ml", "瓶");
  Product available = product("available-1", "瓶");
  available.setName("可乐");
  available.setSpec("500ml");
  available.setAvailable(Boolean.TRUE);

  List<ProductImportModel> actual = ProductServiceImpl.filterDisabledDuplicateImportRows(
      Collections.singletonList(row), Collections.singletonList(available));

  Assert.assertEquals(actual, Collections.singletonList(row));
}

private static ProductImportModel importModel(String name, String spec, String unit) {
  ProductImportModel model = new ProductImportModel();
  model.setName(name);
  model.setSpec(spec);
  model.setUnit(unit);
  return model;
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl xingyun-basedata -Dtest=ProductServiceImplTest test`

Expected: 编译失败，提示 `filterDisabledDuplicateImportRows` 不存在。

- [ ] **Step 3: 实现最小过滤逻辑**

在 `ProductServiceImpl#importExcel` 的空列表检查后、`check(list)` 前替换待处理列表：

```java
List<ProductImportModel> importRows = filterDisabledDuplicateImportRows(list,
        queryDisabledProductsByNames(list.stream().map(ProductImportModel::getName)
                .filter(StringUtil::isNotBlank).collect(Collectors.toSet())));
this.check(importRows);
ProductImportPersistBatch persistBatch = this.buildProducts(importRows);
```

新增如下方法，复用现有 `buildNameSpecUnitKey(Product)` 与 `buildNameSpecUnitKey(ProductImportModel)`：

```java
/**
 * 过滤与停用商品名称、规格、单位完全相同的导入行。
 *
 * @param importRows 导入行
 * @param disabledProducts 已停用商品
 * @return 需要继续校验和持久化的导入行
 */
static List<ProductImportModel> filterDisabledDuplicateImportRows(List<ProductImportModel> importRows,
        List<Product> disabledProducts) {
    if (CollectionUtil.isEmpty(importRows) || CollectionUtil.isEmpty(disabledProducts)) {
        return importRows;
    }
    Set<String> disabledKeys = disabledProducts.stream()
            .filter(product -> Boolean.FALSE.equals(product.getAvailable()))
            .map(ProductServiceImpl::buildNameSpecUnitKey)
            .collect(Collectors.toSet());
    return importRows.stream().filter(row -> !disabledKeys.contains(buildNameSpecUnitKey(row)))
            .collect(Collectors.toList());
}

/**
 * 根据名称批量查询停用商品。
 *
 * @param names 商品名称集合
 * @return 停用商品列表
 */
private List<Product> queryDisabledProductsByNames(Set<String> names) {
    if (CollectionUtil.isEmpty(names)) {
        return CollectionUtil.emptyList();
    }
    return getBaseMapper().selectList(Wrappers.lambdaQuery(Product.class)
            .in(Product::getName, names).eq(Product::getAvailable, Boolean.FALSE));
}
```

将三个 `buildNameSpecUnitKey` 重载及其底层字符串方法改为 `static`，使过滤方法可复用它们且不改变键格式。

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -pl xingyun-basedata -Dtest=ProductServiceImplTest test`

Expected: `ProductServiceImplTest` 全部通过。

- [ ] **Step 5: 提交后端改动**

```bash
git add backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImpl.java backend/xingyun-basedata/src/test/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImplTest.java
git commit -m "feat: ignore disabled duplicate products during import"
```

### Task 2: 导入弹窗规则说明

**Files:**
- Modify: `frontend/src/components/Importor/ProductImporter.vue:3-9`

**Interfaces:**
- Consumes: `ExcelImporterNew` 的 `tipMsg: string` 属性。
- Produces: 商品导入弹窗展示停用重复行忽略规则。

- [ ] **Step 1: 写入失败断言**

在本地执行以下命令，确认当前组件尚未包含规则文本：

```powershell
rg -n -F '名称+规格+单位与已停用商品重复时，该行将被忽略。' frontend/src/components/Importor/ProductImporter.vue
```

Expected: 无匹配输出，命令退出码为 1。

- [ ] **Step 2: 修改提示文案**

将组件的 `tip-msg` 改为：

```vue
:tip-msg="'导入只支持新增商品信息。\n注：\n1、只支持导入普通商品。\n2、名称+规格+单位与已停用商品重复时，该行将被忽略。'"
```

- [ ] **Step 3: 验证规则文本与前端检查**

Run: `rg -n -F '名称+规格+单位与已停用商品重复时，该行将被忽略。' frontend/src/components/Importor/ProductImporter.vue`

Expected: 输出 `ProductImporter.vue` 中的 `tip-msg` 行。

Run: `pnpm run lint`

Expected: exit code 0。

- [ ] **Step 4: 提交前端改动**

```bash
git add frontend/src/components/Importor/ProductImporter.vue
git commit -m "feat: document disabled product import rule"
```

### Task 3: 集成验证

**Files:**
- Verify: `backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImpl.java`
- Verify: `frontend/src/components/Importor/ProductImporter.vue`

**Interfaces:**
- Consumes: 任务 1 的过滤方法与任务 2 的提示文案。
- Produces: 已验证的商品导入行为。

- [ ] **Step 1: 核对需求覆盖**

确认以下事实：停用三元组匹配行在 `check` 之前被过滤；启用商品查询仍只使用现有 `queryAvailableProductsByNames`；弹窗含完整忽略规则；未增加逐行数据库查询。

- [ ] **Step 2: 运行后端模块测试**

Run: `mvn -pl xingyun-basedata test`

Expected: exit code 0。

- [ ] **Step 3: 检查改动质量**

Run: `git diff --check`

Expected: 无输出且 exit code 0。

- [ ] **Step 4: 查看最终工作区状态**

Run: `git status --short`

Expected: 仅包含已知的计划文档忽略文件，或无未提交的源代码改动。
