# 报价单管理与销售定价 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在商品中心新增带有效期的报价单管理，并在唯一报价模式下以报价单商品和价格驱动销售出库单。

**Architecture:** 报价单归属基础资料模块，由主表、商品明细和销售明细中的来源报价单 ID 组成。基础资料服务负责报价单生命周期和按日期查询；销售出库服务仅调用该查询并在保存事务中重新计算价格与金额，前端只负责受限选择和展示。

**Tech Stack:** Spring Boot 2.2、MyBatis-Plus、MapStruct、MySQL 5.7、Vue 3、TypeScript、ant-design-vue、vxe-table、TestNG。

---

## 文件结构

- 新建 `backend/xingyun-api/src/main/resources/db/migration/tenant/V2.9-quote-sheet.sql`：报价表、报价明细和销售明细来源字段。
- 新建 `backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/{entity,mapper,service,impl,controller,vo,bo,converter}/quote/**`：报价单的分层实现。
- 修改 `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/{entity,impl/sale}/**`：销售报价校验及来源保存。
- 新建 `frontend/src/api/base-data/quote/**` 和 `frontend/src/views/base-data/product/quote/**`：报价单 API、列表、详情和编辑页。
- 修改 `frontend/src/views/sc/sale/out/{add,modify}-*.vue` 及销售商品选择组件：按报价单筛选与带价。
- 修改 `frontend/src/router/routes/index.ts`、租户菜单迁移：访问报价单页面和权限。

### Task 1: 数据库与报价单领域模型

**Files:**
- Create: `backend/xingyun-api/src/main/resources/db/migration/tenant/V2.9-quote-sheet.sql`
- Create: `backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/entity/quote/QuoteSheet.java`
- Create: `backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/entity/quote/QuoteSheetDetail.java`
- Create: `backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/enums/quote/QuoteSheetStatus.java`
- Create: `backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/mappers/quote/QuoteSheetMapper.java`
- Create: `backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/mappers/quote/QuoteSheetDetailMapper.java`
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/entity/SaleOutSheetDetail.java`

- [ ] **Step 1: 写迁移脚本和实体字段测试**

在 `backend/xingyun-basedata/src/test/java/com/lframework/xingyun/basedata/impl/quote/QuoteSheetServiceImplTest.java` 写入：

```java
@Test
void shouldTreatAdjacentDateRangesAsNonOverlapping() {
  Assert.assertFalse(QuoteSheetServiceImpl.isDateRangeOverlapped(
      LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31),
      LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)));
}
```

- [ ] **Step 2: 运行测试确认失败**

Run (在 `backend` 目录): `mvn -pl xingyun-basedata -Dtest=QuoteSheetServiceImplTest test`

Expected: 编译失败，提示 `QuoteSheetServiceImpl` 不存在。

- [ ] **Step 3: 创建最小数据结构**

迁移脚本创建 `tbl_quote_sheet`（`id`、`code`、`name`、`start_date`、`end_date`、`status`、`description`、租户和审计字段）及 `tbl_quote_sheet_detail`（`id`、`quote_sheet_id`、`product_id`、`sale_price`、租户和审计字段），并执行：

```sql
ALTER TABLE `tbl_sale_out_sheet_detail`
  ADD COLUMN `quote_sheet_id` varchar(32) DEFAULT NULL COMMENT '报价单ID' AFTER `product_id`;
CREATE UNIQUE INDEX `uk_quote_sheet_detail_product`
  ON `tbl_quote_sheet_detail` (`quote_sheet_id`, `product_id`);
CREATE INDEX `idx_quote_sheet_date`
  ON `tbl_quote_sheet` (`tenant_id`, `start_date`, `end_date`);
```

实体使用 `@TableName`、`@TableId(type = IdType.ASSIGN_ID)`、`LocalDate` 和 `BigDecimal`；枚举仅含 `ENABLED`、`DISABLED`。

- [ ] **Step 4: 运行领域测试确认通过**

Run (在 `backend` 目录): `mvn -pl xingyun-basedata -Dtest=QuoteSheetServiceImplTest test`

Expected: `Tests run: 1, Failures: 0`。

- [ ] **Step 5: 提交数据库模型**

```bash
git add backend/xingyun-api/src/main/resources/db/migration/tenant/V2.9-quote-sheet.sql backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/entity/quote backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/enums/quote backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/mappers/quote backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/entity/SaleOutSheetDetail.java
git commit -m "feat: add quote sheet data model"
```

### Task 2: 报价单服务、校验和接口

**Files:**
- Create: `backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/service/quote/QuoteSheetService.java`
- Create: `backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/impl/quote/QuoteSheetServiceImpl.java`
- Create: `backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/controller/quote/QuoteSheetController.java`
- Create: `backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/vo/quote/{CreateQuoteSheetVo,UpdateQuoteSheetVo,QueryQuoteSheetVo,QuoteSheetProductVo,QueryQuoteProductVo}.java`
- Create: `backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/bo/quote/{GetQuoteSheetBo,QueryQuoteSheetBo,QuoteProductBo}.java`
- Create: `backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/converter/quote/QuoteSheetConverter.java`
- Modify: `backend/xingyun-basedata/src/test/java/com/lframework/xingyun/basedata/impl/quote/QuoteSheetServiceImplTest.java`

- [ ] **Step 1: 写失败的服务规则测试**

覆盖以下断言：重叠区间（含端点相等）抛出 `DefaultClientException`；同单商品重复抛异常；已引用报价单删除抛异常；停用报价单不被 `getActiveQuoteProducts(LocalDate)` 返回。

```java
@Test(expectedExceptions = DefaultClientException.class,
    expectedExceptionsMessageRegExp = ".*定价周期.*冲突.*")
void shouldRejectOverlappedQuotePeriod() {
  QuoteSheetServiceImpl.assertNoDateRangeOverlap("quote-2", LocalDate.of(2026, 8, 1),
      LocalDate.of(2026, 8, 31), Collections.singletonList(sheet("quote-1", "2026-08-31", "2026-09-30")));
}
```

- [ ] **Step 2: 运行测试确认失败**

Run (在 `backend` 目录): `mvn -pl xingyun-basedata -Dtest=QuoteSheetServiceImplTest test`

Expected: FAIL，缺少 `assertNoDateRangeOverlap` 或业务异常。

- [ ] **Step 3: 实现最小服务与接口**

服务提供 `create`、`update`、`deleteById`、`enable`、`disable`、`get`、`query`、`getActiveQuoteProducts`。保存和启用均使用下列闭区间规则，排除自身 ID：

```java
private static boolean isDateRangeOverlapped(LocalDate startA, LocalDate endA,
    LocalDate startB, LocalDate endB) {
  return !endA.isBefore(startB) && !startA.isAfter(endB);
}
```

Controller 的所有读写端点均采用 `@PostMapping`，使用 `InvokeResult` 包装；删除、启停和保存捕获异常并记录日志；转换只由 `QuoteSheetConverter` 完成。

- [ ] **Step 4: 运行服务测试确认通过**

Run (在 `backend` 目录): `mvn -pl xingyun-basedata -Dtest=QuoteSheetServiceImplTest test`

Expected: 全部报价单规则测试通过。

- [ ] **Step 5: 提交报价单后端功能**

```bash
git add backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/{service,impl,controller,vo,bo,converter}/quote backend/xingyun-basedata/src/test/java/com/lframework/xingyun/basedata/impl/quote/QuoteSheetServiceImplTest.java
git commit -m "feat: add quote sheet management api"
```

### Task 3: 销售出库唯一报价定价

**Files:**
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetServiceImpl.java`
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/service/sale/SaleOutSheetService.java`
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/controller/sale/SaleOutSheetController.java`
- Modify: `backend/xingyun-sc/src/test/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetServiceImplTest.java`

- [ ] **Step 1: 写销售报价失败测试**

测试唯一报价模式开启时：指定日期无有效报价单、报价中缺商品时均抛 `DefaultClientException`；商品存在时销售明细的单价和 `quoteSheetId` 被覆盖；日期改为另一报价周期时重新使用新价格；关闭模式时 `quoteSheetId` 为 `null` 并继续商品基础售价逻辑。

```java
@Test
void shouldRepriceDetailsWhenOrderDateChanges() {
  SaleOutSheetDetail detail = detail("product-1", new BigDecimal("10"));
  QuoteProductBo quoteProduct = quoteProduct("quote-sep", "product-1", new BigDecimal("12"));
  SaleOutSheetServiceImpl.applyQuotePrice(detail, quoteProduct);
  Assert.assertEquals(detail.getSalePrice(), new BigDecimal("12"));
  Assert.assertEquals(detail.getQuoteSheetId(), "quote-sep");
}
```

- [ ] **Step 2: 运行测试确认失败**

Run (在 `backend` 目录): `mvn -pl xingyun-sc -Dtest=SaleOutSheetServiceImplTest test`

Expected: FAIL，缺少报价定价方法。

- [ ] **Step 3: 在保存事务内实现服务端定价**

在销售出库 `create`、`update` 进入明细金额计算前调用 `applyQuotePricing(orderDate, details)`：开关开启时只查询一次已启用报价单和其商品映射，对每条明细校验商品存在，调用现有金额计算路径；关闭时清空 `quoteSheetId` 并使用原有 `getDefaultSalePrice`。新增 `POST /sale/out/quote-products/query` 仅返回当前日期可售报价商品，供前端查询。

- [ ] **Step 4: 运行销售单元测试确认通过**

Run (在 `backend` 目录): `mvn -pl xingyun-sc -Dtest=SaleOutSheetServiceImplTest test`

Expected: 唯一报价、日期改价和回退逻辑测试全部通过。

- [ ] **Step 5: 提交销售报价联动**

```bash
git add backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/{entity,service,impl,controller}/sale backend/xingyun-sc/src/test/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetServiceImplTest.java
git commit -m "feat: price sale out sheets from quote sheets"
```

### Task 4: 报价单管理前端

**Files:**
- Create: `frontend/src/api/base-data/quote/index.ts`
- Create: `frontend/src/api/base-data/quote/model/*.ts`
- Create: `frontend/src/views/base-data/product/quote/index.vue`
- Create: `frontend/src/views/base-data/product/quote/detail.vue`
- Create: `frontend/src/views/base-data/product/quote/modify.vue`
- Modify: `frontend/src/router/routes/index.ts`
- Modify: `backend/xingyun-api/src/main/resources/db/migration/tenant/V2.9-quote-sheet.sql`

- [ ] **Step 1: 写前端失败测试**

创建 `frontend/src/views/base-data/product/quote/__tests__/quoteSheetPeriod.test.ts`，验证编辑参数包含 `startDate`、`endDate`、商品 ID 与 `salePrice`，并拒绝开始日晚于结束日的本地提交。

- [ ] **Step 2: 运行测试确认失败**

Run (在 `frontend` 目录): `pnpm exec vitest run src/views/base-data/product/quote/__tests__/quoteSheetPeriod.test.ts`

Expected: FAIL，缺少页面参数构造方法。

- [ ] **Step 3: 实现报价单页面和菜单**

列表页显示编号、名称、周期、状态、创建人，提供新增、查看、修改、删除、启用、停用操作；编辑页使用日期范围和商品明细表，批量选择商品、录入销售单价且禁止重复。新增 `/product/quote/detail/:id`、`/product/quote/modify/:id` 静态路由，并在租户菜单迁移新增“商品中心/报价单管理”及 `base-data:quote:*` 权限。

- [ ] **Step 4: 运行前端测试确认通过**

Run (在 `frontend` 目录): `pnpm exec vitest run src/views/base-data/product/quote/__tests__/quoteSheetPeriod.test.ts`

Expected: PASS。

- [ ] **Step 5: 提交报价单前端**

```bash
git add frontend/src/api/base-data/quote frontend/src/views/base-data/product/quote frontend/src/router/routes/index.ts backend/xingyun-api/src/main/resources/db/migration/tenant/V2.9-quote-sheet.sql
git commit -m "feat: add quote sheet management pages"
```

### Task 5: 销售出库商品选择与端到端验证

**Files:**
- Modify: `frontend/src/api/sc/sale/out/index.ts`
- Create: `frontend/src/api/sc/sale/out/model/queryQuoteProductsVo.ts`
- Modify: `frontend/src/views/sc/sale/out/add-un-require.vue`
- Modify: `frontend/src/views/sc/sale/out/add-require.vue`
- Modify: `frontend/src/views/sc/sale/out/modify-un-require.vue`
- Modify: `frontend/src/views/sc/sale/out/modify-require.vue`
- Modify: `frontend/src/views/sc/sale/out/components/saleOutProductParams.ts`
- Create: `frontend/src/views/sc/sale/out/components/__tests__/saleOutQuoteProduct.test.ts`

- [ ] **Step 1: 写前端销售报价失败测试**

```ts
it('日期变化时仅保留当前报价单商品并带入报价价', () => {
  const result = applyQuoteProducts(rows, [{ productId: 'p-1', salePrice: 12, quoteSheetId: 'q-2' }]);
  expect(result[0]).toMatchObject({ productId: 'p-1', oriPrice: 12, quoteSheetId: 'q-2' });
  expect(result[1].invalidQuoteProduct).toBe(true);
});
```

- [ ] **Step 2: 运行测试确认失败**

Run (在 `frontend` 目录): `pnpm exec vitest run src/views/sc/sale/out/components/__tests__/saleOutQuoteProduct.test.ts`

Expected: FAIL，缺少 `applyQuoteProducts`。

- [ ] **Step 3: 实现日期驱动商品范围和价格刷新**

开关开启后，订单日期变化调用报价商品查询接口；商品搜索、批量添加和已选行均使用该结果。对仍存在的商品覆盖单价和报价单 ID；不存在的行标红并阻止保存。关闭开关后清空报价状态，恢复原始商品查询和价格选择。

- [ ] **Step 4: 运行前端测试与质量检查**

Run (在 `frontend` 目录): `pnpm exec vitest run src/views/sc/sale/out/components/__tests__/saleOutQuoteProduct.test.ts`

Expected: PASS。

Run (在 `frontend` 目录): `pnpm run lint`

Expected: 退出码 0。

- [ ] **Step 5: 运行后端构建并提交**

Run (在 `backend` 目录): `mvn clean compile -DskipTests`

Expected: `BUILD SUCCESS`。

```bash
git add frontend/src/api/sc/sale/out frontend/src/views/sc/sale/out
git commit -m "feat: restrict sale products by active quote sheet"
```

### Task 6: 完整回归与交付检查

**Files:**
- Modify: `docs/superpowers/specs/2026-08-29-quote-sheet-design.md`（仅在实现与已确认设计不一致时更新）

- [ ] **Step 1: 运行报价和销售后端测试**

Run (在 `backend` 目录): `mvn -pl xingyun-basedata,xingyun-sc -Dtest=QuoteSheetServiceImplTest,SaleOutSheetServiceImplTest test`

Expected: 所有测试通过。

- [ ] **Step 2: 执行人工验收场景**

1. 创建 8 月报价单并启用，尝试创建任何交叠报价单，页面显示周期冲突。
2. 在唯一报价模式下创建 8 月销售单，只能选择 8 月报价商品且价格与报价一致。
3. 将销售日期改到 9 月，商品存在时价格自动变为 9 月报价；商品不存在时不能保存。
4. 关闭唯一报价模式，销售单恢复商品档案基础售价。
5. 对已生成销售单的报价单执行删除，系统拒绝并保留历史明细来源。

- [ ] **Step 3: 检查改动并提交回归结果**

Run: `git diff --check`

Run: `git status --short`

Expected: 无空白错误；仅包含本功能的已提交变更。

```bash
git commit --allow-empty -m "test: verify quote sheet pricing flow"
```
