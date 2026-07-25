# 采购收货明细生产日期 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在采购收货商品明细中支持可选生产日期，并在收货单全链路严格校验、持久化、展示和导入导出。

**Architecture:** `productionDate` 是收货明细字符串属性，数据库使用 `VARCHAR(10)`。Service 在创建、修改和导入入口复用严格日期解析；已有 DTO、MyBatis 查询和前端接口模型透传到表格和 Excel。

**Tech Stack:** Java 8、Spring Boot 2.2、MyBatis-Plus、EasyExcel、Vue 3、TypeScript、ant-design-vue、vxe-table。

## Global Constraints

- 非空值必须严格为 `yyyy.MM.dd` 且为真实日历日期；空值允许。
- 前端仅提供手动文本输入，不做格式校验；后端以 `DefaultClientException` 统一拒绝非法值。
- 不新增接口，不修改采购退货或库存链路；所有 Java 新方法添加中文注释。

---

## File Structure

- `xingyun-api/src/main/resources/db/migration/tenant/V2.3-receive-sheet-production-date.sql`：新增租户库字段。
- `xingyun-sc/src/main/java/com/lframework/xingyun/sc/{entity/ReceiveSheetDetail.java,vo/purchase/receive/ReceiveProductVo.java,dto/purchase/receive/ReceiveSheetFullDto.java,dto/purchase/receive/QueryReceiveSheetDetailDto.java}`：持久化、请求、详情和查询类型。
- `xingyun-sc/src/main/resources/mappers/purchase/{ReceiveSheetMapper.xml,ReceiveSheetDetailMapper.xml}`：结果映射和查询列。
- `xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/purchase/ReceiveSheetServiceImpl.java`：严格校验、创建修改写库、导入校验。
- `xingyun-sc/src/main/java/com/lframework/xingyun/sc/excel/purchase/receive/{ReceiveSheetImportModel.java,ReceiveSheetExportModel.java,ReceiveSheetDetailExportModel.java}`：模板和两类导出。
- `frontend/src/api/sc/purchase/receive/model/{receiveProductVo.ts,getReceiveSheetBo.ts,queryReceiveSheetDetailBo.ts}` 与 `frontend/src/views/sc/purchase/receive/`：接口类型与全部收货明细表格。
- `xingyun-sc/src/test/java/com/lframework/xingyun/sc/impl/purchase/ReceiveSheetServiceImplTest.java`：核心日期校验和导入错误测试。

### Task 1: 生产日期严格校验（TDD）

**Files:**
- Modify: `xingyun-sc/src/test/java/com/lframework/xingyun/sc/impl/purchase/ReceiveSheetServiceImplTest.java`
- Modify: `xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/purchase/ReceiveSheetServiceImpl.java`

**Interfaces:** 产出 `static List<String> validateProductionDate(String productionDate, String errorPrefix)`。

- [ ] **Step 1: 写失败测试**

```java
@Test
void shouldValidateProductionDateStrictly() {
  Assert.assertTrue(ReceiveSheetServiceImpl.validateProductionDate(null, "第2行").isEmpty());
  Assert.assertTrue(ReceiveSheetServiceImpl.validateProductionDate("2024.02.29", "第2行").isEmpty());
  Assert.assertEquals("第2行商品生产日期格式错误，应为yyyy.MM.dd且必须是有效日期",
      ReceiveSheetServiceImpl.validateProductionDate("2026.02.30", "第2行").get(0));
  Assert.assertEquals("第2行商品生产日期格式错误，应为yyyy.MM.dd且必须是有效日期",
      ReceiveSheetServiceImpl.validateProductionDate("2026-07-16", "第2行").get(0));
}
```

- [ ] **Step 2: 运行失败测试**

Run: `mvn -pl xingyun-sc -Dtest=ReceiveSheetServiceImplTest test`

Expected: FAIL，方法尚不存在。

- [ ] **Step 3: 实现最小严格校验**

在 Service 增加以下 import：`java.time.format.DateTimeFormatter`、`java.time.format.DateTimeParseException`、`java.time.format.ResolverStyle`；并添加：

```java
private static final DateTimeFormatter PRODUCTION_DATE_FORMATTER =
    DateTimeFormatter.ofPattern("uuuu.MM.dd").withResolverStyle(ResolverStyle.STRICT);

/** 校验生产日期格式和日历有效性。 */
static List<String> validateProductionDate(String productionDate, String errorPrefix) {
  if (StringUtils.isBlank(productionDate)) {
    return Lists.newArrayList();
  }
  try {
    LocalDate.parse(productionDate, PRODUCTION_DATE_FORMATTER);
    return Lists.newArrayList();
  } catch (DateTimeParseException ex) {
    return Lists.newArrayList(errorPrefix + "商品生产日期格式错误，应为yyyy.MM.dd且必须是有效日期");
  }
}
```

- [ ] **Step 4: 验证并提交**

Run: `mvn -pl xingyun-sc -Dtest=ReceiveSheetServiceImplTest test`

Expected: PASS。

```powershell
git add xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/purchase/ReceiveSheetServiceImpl.java xingyun-sc/src/test/java/com/lframework/xingyun/sc/impl/purchase/ReceiveSheetServiceImplTest.java
git commit -m "test: cover receive production date validation"
```

### Task 2: 持久化、请求响应和 Mapper

**Files:**
- Create: `xingyun-api/src/main/resources/db/migration/tenant/V2.3-receive-sheet-production-date.sql`
- Modify: `ReceiveSheetDetail.java`, `ReceiveProductVo.java`, `ReceiveSheetFullDto.java`, `QueryReceiveSheetDetailDto.java`, `ReceiveSheetMapper.xml`, `ReceiveSheetDetailMapper.xml`, `ReceiveSheetServiceImpl.java`

**Interfaces:** 产出每个收货明细类型的 `String productionDate`，以及从请求到表字段的保存与详情查询。

- [ ] **Step 1: 新增迁移和类型字段**

```sql
ALTER TABLE `tbl_receive_sheet_detail`
    ADD COLUMN `production_date` varchar(10) NULL DEFAULT NULL COMMENT '生产日期' AFTER `actual_date`;
```

在 `ReceiveSheetDetail`、`ReceiveProductVo`、`ReceiveSheetFullDto.OrderDetailDto` 和 `QueryReceiveSheetDetailDto` 均添加带中文注释的：

```java
private String productionDate;
```

- [ ] **Step 2: 补全 MyBatis 字段**

在详情映射和 SQL 使用别名：

```xml
<result column="detail_production_date" property="productionDate"/>
d.production_date AS detail_production_date,
```

在 `ReceiveSheetDetailMapper.xml` 使用原字段名：

```xml
<result column="production_date" property="productionDate"/>
d.production_date,
```

在 `ReceiveSheetMapper.xml` 的 `ReceiveSheetDetailDto_sql` 选择 `d.production_date`，使 `QueryReceiveSheetDetailDto.productionDate` 自动映射。

- [ ] **Step 3: 创建/修改校验并保存**

在 `ReceiveSheetServiceImpl#create` 的 `for (ReceiveProductVo productVo ...)` 循环中、创建 detail 前插入：

```java
List<String> productionDateErrors = validateProductionDate(productVo.getProductionDate(),
    "第" + productVo.getSeq() + "行");
if (!productionDateErrors.isEmpty()) {
  throw new DefaultClientException(productionDateErrors.get(0));
}
```

紧随 `detail.setActualDate(productVo.getActualDate());` 插入：

```java
detail.setProductionDate(StringUtils.trimToNull(productVo.getProductionDate()));
```

- [ ] **Step 4: 编译并提交**

Run: `mvn -pl xingyun-sc -am compile -DskipTests`

Expected: BUILD SUCCESS。

```powershell
git add xingyun-api/src/main/resources/db/migration/tenant/V2.3-receive-sheet-production-date.sql xingyun-sc/src/main/java xingyun-sc/src/main/resources/mappers/purchase
git commit -m "feat: persist receive production date"
```

### Task 3: Excel 模板、导入和两类导出

**Files:**
- Modify: `ReceiveSheetImportModel.java`, `ReceiveSheetExportModel.java`, `ReceiveSheetDetailExportModel.java`, `ReceiveSheetServiceImpl.java`, `ReceiveSheetServiceImplTest.java`

**Interfaces:** 模板和导出列标题固定为“生产日期”；导入返回行级错误。

- [ ] **Step 1: 增加导入失败测试**

```java
@Test
void shouldRejectInvalidProductionDateWhenImporting() {
  ReceiveSheetImportModel model = createModel(BigDecimal.ONE, BigDecimal.ONE);
  model.setSeq(2);
  model.setProductionDate("2026.02.30");
  Assert.assertTrue(ReceiveSheetServiceImpl.validateImportNumbers(model).contains(
      "第2行商品生产日期格式错误，应为yyyy.MM.dd且必须是有效日期"));
}
```

- [ ] **Step 2: 运行失败测试**

Run: `mvn -pl xingyun-sc -Dtest=ReceiveSheetServiceImplTest test`

Expected: FAIL，模型或导入校验尚未接入。

- [ ] **Step 3: 接入模型、校验和导出**

在导入模型增加：

```java
/** 生产日期，格式：yyyy.MM.dd。 */
@ExcelProperty("生产日期")
private String productionDate;
```

在 `validateImportNumbers` 返回前增加：

```java
errors.addAll(validateProductionDate(data.getProductionDate(), "第" + rowIndex + "行"));
```

在两个导出模型增加：

```java
@ExcelProperty("生产日期")
private String productionDate;
```

并在各自 `afterInit` 或 `convert` 中从明细的 `getProductionDate()` 赋值；该列放在备注前，保持其它现有列顺序。

- [ ] **Step 4: 验证并提交**

Run: `mvn -pl xingyun-sc -Dtest=ReceiveSheetServiceImplTest test`

Expected: PASS。

```powershell
git add xingyun-sc/src/main/java/com/lframework/xingyun/sc/excel/purchase/receive xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/purchase/ReceiveSheetServiceImpl.java xingyun-sc/src/test/java/com/lframework/xingyun/sc/impl/purchase/ReceiveSheetServiceImplTest.java
git commit -m "feat: import and export receive production date"
```

### Task 4: 前端类型与新增、修改页面

**Files:**
- Modify: `frontend/src/api/sc/purchase/receive/model/{receiveProductVo.ts,getReceiveSheetBo.ts,queryReceiveSheetDetailBo.ts}`
- Modify: `frontend/src/views/sc/purchase/receive/{add-require,add-un-require,modify-require,modify-un-require}.vue`

**Interfaces:** 四个可编辑页面可手动录入并提交 `productionDate: string`。

- [ ] **Step 1: 补充客户端模型**

在 `ReceiveProductVo`、`OrderDetailBo`、`QueryReceiveSheetDetailBo` 增加：

```ts
/** 生产日期，格式由后端校验为 yyyy.MM.dd。 */
productionDate: string;
```

- [ ] **Step 2: 在四个编辑表格加入普通文本框**

在每个 `tableColumn` 的备注列前新增：

```ts
{ field: 'productionDate', title: '生产日期', width: 120, slots: { default: 'productionDate_default' } },
```

新增对应插槽：

```vue
<template #productionDate_default="{ row, rowIndex }">
  <a-input :ref="'productionDateInputRef' + rowIndex" v-model:value="row.productionDate" />
</template>
```

新增行、商品选择和详情回填对象均初始化 `productionDate: ''`；每个提交的商品对象增加 `productionDate: t.productionDate`。不得增加任何前端日期格式校验或格式化。

- [ ] **Step 3: 前端检查并提交**

Run: `pnpm --dir frontend run lint`

Expected: 成功且没有新增 lint 错误。

```powershell
git add frontend/src/api/sc/purchase/receive/model frontend/src/views/sc/purchase/receive/add-require.vue frontend/src/views/sc/purchase/receive/add-un-require.vue frontend/src/views/sc/purchase/receive/modify-require.vue frontend/src/views/sc/purchase/receive/modify-un-require.vue
git commit -m "feat: edit receive production date in frontend"
```

### Task 5: 查看、查询和回归验证

**Files:**
- Modify: `frontend/src/views/sc/purchase/receive/detail.vue`
- Modify: `frontend/src/views/sc/purchase/receive/approve.vue`
- Modify: `frontend/src/views/sc/purchase/receive/components/detail-list.vue`

**Interfaces:** 详情、审核查看、查询明细表均原样显示后端返回的生产日期。

- [ ] **Step 1: 加入只读列**

在以上三个文件的 `tableColumn` 备注列前加入：

```ts
{ field: 'productionDate', title: '生产日期', width: 120 },
```

不使用日期格式化器。

- [ ] **Step 2: 自动化验证**

Run: `mvn clean compile -DskipTests`

Expected: BUILD SUCCESS。

Run: `mvn -pl xingyun-sc -Dtest=ReceiveSheetServiceImplTest test`

Expected: PASS。

Run: `pnpm --dir frontend run lint`

Expected: 成功且无新增 lint 错误。

- [ ] **Step 3: 人工验收**

1. 输入 `2024.02.29` 保存后，详情、审核查看、查询明细原样显示。
2. 创建/修改输入 `2026.02.30` 返回“第 N 行商品生产日期格式错误，应为yyyy.MM.dd且必须是有效日期”。
3. 导入模板有“生产日期”列；导入非法日期返回 Excel 行号；两类导出有该列。
4. 历史明细的生产日期为空，查询和导出不报错。

- [ ] **Step 4: 提交**

```powershell
git add frontend/src/views/sc/purchase/receive/detail.vue frontend/src/views/sc/purchase/receive/approve.vue frontend/src/views/sc/purchase/receive/components/detail-list.vue
git commit -m "feat: display receive production date"
```

## Self-Review

- 需求覆盖：任务 2 覆盖存储、接口和详情/查询；任务 3 覆盖导入/导出；任务 4 覆盖新增/修改；任务 5 覆盖查看、查询与回归。
- 类型一致：数据库 `production_date`、Java `productionDate: String`、TypeScript `productionDate: string`、Excel 标题“生产日期”一致。
- 实施信息完整：迁移、校验规则、错误文案、页面范围与验证命令均明确。
