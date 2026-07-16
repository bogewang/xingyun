# 商品批量启停用 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为商品资料增加批量启用/禁用和三态查询，并让禁用商品可回显历史单据但不能用于新建业务单据。

**Architecture:** 商品资料后端增加三态查询条件和独立批量状态接口，状态更新只修改 `available` 并清理商品缓存。商品服务提供一次批量状态校验，由所有接收商品明细的新建业务入口调用；历史读取继续使用不带状态过滤的商品 ID 查询。前端商品列表默认启用，使用一次批量请求完成启停用，商品选择器分页只取启用商品、按 ID 加载保留历史回显。

**Tech Stack:** Spring Boot 2.2.2、Java 8、MyBatis-Plus 3.4.2、TestNG/JUnit Jupiter、Vue 3、TypeScript、ant-design-vue、vxe-table、Vitest、pnpm。

## Global Constraints

- Controller 只做参数校验和响应包装，业务编排放在 Service。
- 业务异常必须使用 `DefaultClientException`，对外响应使用 `InvokeResult<T>`。
- DAO 只使用 MyBatis-Plus `QueryWrapper`/`LambdaUpdateWrapper`，不内联 SQL。
- 批量商品状态校验必须一次查询完成，禁止循环查询数据库。
- 历史单据回显使用 `ProductService.findById`，不得给该方法增加 `available=true` 条件。
- 所有新增 Java 方法添加中文注释，核心业务使用 JUnit 测试；现有 TestNG 测试保持兼容。
- 前端新增请求模型和工具方法添加中文注释，复用现有 `AVAILABLE`、`BatchHandler`、权限和消息组件。
- 完成前必须执行后端测试/编译、前端 lint 和相关 Vitest，并以实际输出为准报告结果。

---

### Task 1: 增加商品三态查询与批量状态后端接口

**Files:**
- Create: `backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/vo/product/info/UpdateProductAvailableVo.java`
- Modify: `backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/vo/product/info/QueryProductVo.java`
- Modify: `backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/bo/product/info/QueryProductBo.java`
- Modify: `backend/xingyun-basedata/src/main/resources/mappers/product/ProductMapper.xml`
- Modify: `backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/service/product/ProductService.java`
- Modify: `backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImpl.java`
- Modify: `backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/controller/ProductController.java`
- Create: `backend/xingyun-basedata/src/test/java/com/lframework/xingyun/basedata/impl/product/ProductAvailabilityTest.java`
- Modify: `backend/xingyun-basedata/pom.xml` to add the explicit JUnit Jupiter test dependency required by the new JUnit test class.

**Interfaces:**
- Produces `UpdateProductAvailableVo.ids`, `UpdateProductAvailableVo.available`, `ProductService.updateAvailable(UpdateProductAvailableVo vo)` and `ProductService.assertAvailable(Collection<String> productIds)` for later tasks.
- `GET /basedata/product/query` accepts `available=true|false|missing`; `PUT /basedata/product/available` accepts `{ids: string[], available: boolean}`.

- [ ] **Step 1: Add the failing JUnit tests.**

Create tests that exercise the service against a mocked `ProductMapper`: enabled products pass `assertAvailable`, a disabled product throws `DefaultClientException` with `商品已停用，无法新增业务单据！`, duplicate IDs do not cause duplicate validation work, and `updateAvailable` calls one mapper update with the requested target state.

```java
@Test
void shouldRejectDisabledProduct() {
  Product disabled = new Product();
  disabled.setId("product-1");
  disabled.setAvailable(Boolean.FALSE);
  when(mapper.selectList(any())).thenReturn(Collections.singletonList(disabled));

  DefaultClientException exception = assertThrows(DefaultClientException.class,
      () -> service.assertAvailable(Arrays.asList("product-1", "product-1")));

  assertEquals("商品已停用，无法新增业务单据！", exception.getMessage());
}

@Test
void shouldUpdateAvailabilityInOneMapperCall() {
  UpdateProductAvailableVo vo = new UpdateProductAvailableVo();
  vo.setIds(Arrays.asList("product-1", "product-2", "product-1"));
  vo.setAvailable(Boolean.FALSE);

  service.updateAvailable(vo);

  verify(mapper, times(1)).update(isNull(), any(LambdaUpdateWrapper.class));
}
```

Use reflection only to inject the existing `ProductServiceImpl.baseMapper` and its cache-cleaning proxy; do not add test-only methods to production code.

- [ ] **Step 2: Run the focused test and verify the expected red result.**

Run from `D:\dev\CODE\xingyun\backend`:

```powershell
mvn -pl xingyun-basedata -am -Dtest=ProductAvailabilityTest test -DfailIfNoTests=false
```

Expected: FAIL because `UpdateProductAvailableVo`, `assertAvailable` and `updateAvailable` are not implemented yet.

- [ ] **Step 3: Implement the request model and query field.**

Add the test dependency before compiling the new JUnit class:

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

Add `@NotEmpty List<String> ids` and `@NotNull Boolean available` to `UpdateProductAvailableVo`, each with `@ApiModelProperty` and Chinese validation messages. Add `Boolean available` to `QueryProductVo` and `QueryProductBo`, with JavaDoc comments.

- [ ] **Step 4: Implement conditional SQL and service methods.**

In both `query` and `queryCount` blocks of `ProductMapper.xml`, replace the unconditional condition with:

```xml
<if test="vo.available != null">
    AND g.available = #{vo.available}
</if>
```

Keep `findById` unchanged. Add the following service contract and implementation behavior:

```java
/**
 * 批量设置商品启用状态。
 *
 * @param vo 状态更新请求
 */
void updateAvailable(UpdateProductAvailableVo vo);

/**
 * 校验商品均为启用状态。
 *
 * @param productIds 商品 ID 集合
 */
void assertAvailable(Collection<String> productIds);
```

`assertAvailable` removes blank/duplicate IDs, performs one `selectList` with `in(Product::getId, ids)`, and throws the exact business exception when any result has `available=false`. `updateAvailable` removes blank/duplicate IDs, sets only `Product::getAvailable`, runs in a Service transaction, and clears each affected product cache after the update.

- [ ] **Step 5: Add the controller endpoint and verify green.**

Add `@PutMapping("/available")` with the existing `base-data:product:info:modify` permission. The controller calls `productService.updateAvailable(vo)` and returns `InvokeResultBuilder.success()`, wrapping unexpected exceptions with the project’s logged `InvokeResultBuilder.fail` pattern. Run the focused test again:

```powershell
mvn -pl xingyun-basedata -am -Dtest=ProductAvailabilityTest test -DfailIfNoTests=false
```

Expected: PASS, including the disabled-product exception message and one mapper update call.

- [ ] **Step 6: Commit the backend status contract.**

```powershell
git add backend/xingyun-basedata
git commit -m "feat: add product availability batch api"
```

### Task 2: Wire disabled-product rejection into every new product-bearing business document

**Files:**
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/purchase/PurchaseOrderServiceImpl.java`
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/sale/SaleOrderServiceImpl.java`
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/retail/RetailOutSheetServiceImpl.java`
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/purchase/ReceiveSheetServiceImpl.java`
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetServiceImpl.java`
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/purchase/PurchaseReturnServiceImpl.java`
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/sale/SaleReturnServiceImpl.java`
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/retail/RetailReturnServiceImpl.java`
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/stock/transfer/ScTransferOrderServiceImpl.java`
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/stock/adjust/StockAdjustSheetServiceImpl.java`
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/stock/take/TakeStockPlanServiceImpl.java`
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/stock/take/PreTakeStockSheetServiceImpl.java`
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/stock/take/TakeStockSheetServiceImpl.java`
- Create: `backend/xingyun-sc/src/test/java/com/lframework/xingyun/sc/impl/product/ProductAvailabilityIntegrationTest.java`
- Modify: `backend/xingyun-sc/pom.xml` to add the explicit JUnit Jupiter test dependency required by the new JUnit test class.

**Interfaces:**
- Consumes `ProductService.assertAvailable(Collection<String>)` from Task 1.
- Produces a consistent rejection for all public `create(...)` methods that accept product IDs. Existing `update(...)` and read/detail paths retain disabled products already present in historical documents.

- [ ] **Step 1: Add a failing representative business test.**

Using the existing service test style and Mockito, invoke the purchase-order or sale-order create path with a disabled product and assert that `DefaultClientException` contains `商品已停用，无法新增业务单据！`. Add a second test proving the same product is allowed through the historical `findById`/selector-load path.

```java
@Test
void shouldRejectDisabledProductWhenCreatingSaleOrder() {
  when(productService.findById("product-1")).thenReturn(disabledProduct("product-1"));

  DefaultClientException exception = assertThrows(DefaultClientException.class,
      () -> service.create(createSaleOrderWithProduct("product-1")));

  assertEquals("商品已停用，无法新增业务单据！", exception.getMessage());
}
```

- [ ] **Step 2: Run the representative test and verify it fails.**

```powershell
mvn -pl xingyun-sc -am -Dtest=ProductAvailabilityIntegrationTest test -DfailIfNoTests=false
```

Expected: FAIL because the create path currently accepts the disabled `Product` returned by `findById`.

- [ ] **Step 3: Add one batch validation call at each new-document entry.**

Add the same JUnit dependency to `xingyun-sc/pom.xml` before compiling the representative test:

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

Before each service persists the document header or detail rows, collect product IDs from the create VO and call `productService.assertAvailable(...)` once. Examples:

```java
productService.assertAvailable(vo.getProducts().stream()
    .map(PurchaseProductVo::getProductId)
    .collect(Collectors.toList()));
```

Apply the equivalent mapping for sale, retail, receive, return, transfer, adjustment and take-stock product VO types. In `TakeStockPlanServiceImpl`, set `queryProductVo.setAvailable(Boolean.TRUE)` before both `queryCount` and `query` for an all-product plan, because the new query is intentionally tri-state. Category/brand queries remain enabled-only. Validate explicit product lists such as simple take plans. Do not replace historical detail reads with an available-only query.

- [ ] **Step 4: Audit all product-bearing create paths.**

Run:

```powershell
rg -n "public .* create\\(|private void create\\(|productId|getProducts\\(\\)" backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl
```

Inspect every product-bearing public create method and confirm it either calls `assertAvailable` or only creates internal stock side effects from an already-persisted historical document. Add the validator to any missed public entry before continuing.

- [ ] **Step 5: Run backend module tests and compile.**

```powershell
mvn -pl xingyun-sc -am test -DskipTests=false
mvn -pl xingyun-sc -am clean compile -DskipTests
```

Expected: no compilation errors, representative disabled-product creation test PASS, and existing tests remain green.

- [ ] **Step 6: Commit the business-entry validation.**

```powershell
git add backend/xingyun-sc
git commit -m "feat: reject disabled products in new business documents"
```

### Task 3: Preserve historical product loading and expose status in selector/detail output

**Files:**
- Modify: `backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/bo/product/info/ProductSelectorBo.java`
- Modify: `backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/bo/product/info/GetProductBo.java`
- Modify: `frontend/src/api/base-data/product/info/model/productSelectorBo.ts` only if its existing `available` field needs alignment.
- Test: `backend/xingyun-basedata/src/test/java/com/lframework/xingyun/basedata/impl/product/ProductAvailabilityTest.java`

**Interfaces:**
- `/selector/product` remains `available=true` through `ProductMapper.selector`.
- `/selector/product/load` continues to call `ProductService.findById` and returns a disabled product by ID.
- `ProductSelectorBo.available` and `GetProductBo.available` explicitly serialize the entity status.

- [ ] **Step 1: Add the failing selector-load regression test.**

Mock `ProductMapper.selectById` to return a disabled product and call the same service method used by `BaseDataSelectorController.loadProduct`; assert the product is non-null and `available=false`.

- [ ] **Step 2: Run the focused regression test and confirm red.**

```powershell
mvn -pl xingyun-basedata -am -Dtest=ProductAvailabilityTest#shouldLoadDisabledProductByIdForHistory test -DfailIfNoTests=false
```

Expected: FAIL because the selector/detail BOs do not yet explicitly expose the disabled status required by the regression assertion.

- [ ] **Step 3: Add explicit status fields without adding filters.**

Add `private Boolean available` plus Chinese JavaDoc to `ProductSelectorBo` and `GetProductBo`. Do not alter `BaseDataSelectorController.loadProduct`, `ProductService.findById`, or the selector SQL’s enabled-only condition.

- [ ] **Step 4: Run the regression and selector-related backend tests.**

```powershell
mvn -pl xingyun-basedata -am -Dtest=ProductAvailabilityTest test -DfailIfNoTests=false
```

Expected: PASS, with disabled products still loadable by ID.

- [ ] **Step 5: Commit the history compatibility change.**

```powershell
git add backend/xingyun-basedata
git commit -m "fix: keep disabled products visible in history selectors"
```

### Task 4: Add frontend batch request contract and one-request batch handling

**Files:**
- Create: `frontend/src/api/base-data/product/info/model/updateProductAvailableVo.ts`
- Modify: `frontend/src/api/base-data/product/info/index.ts`
- Modify: `frontend/src/components/BatchHandler/src/BatchHandler.vue`
- Create: `frontend/src/views/base-data/product/info/productAvailability.ts`
- Create: `frontend/src/views/base-data/product/info/__tests__/productAvailability.test.ts`

**Interfaces:**
- `UpdateProductAvailableVo` is `{ ids: string[]; available: boolean }`.
- `api.updateAvailable(data: UpdateProductAvailableVo): Promise<void>` sends JSON to `/basedata/product/available`.
- `buildProductAvailabilityRequest(records, available)` returns one deduplicated request payload.
- `BatchHandler` gains an optional batch callback that receives the full confirmation table once; its existing per-row callback behavior remains unchanged for all existing callers.

- [ ] **Step 1: Write the failing Vitest for the one-request payload.**

Create `productAvailability.test.ts`:

```ts
import { describe, expect, it } from 'vitest';
import { buildProductAvailabilityRequest } from '../productAvailability';

describe('商品批量状态请求', () => {
  it('去重商品 ID 并保留目标状态', () => {
    expect(
      buildProductAvailabilityRequest([{ id: 'p-1' }, { id: 'p-1' }, { id: 'p-2' }], false),
    ).toEqual({ ids: ['p-1', 'p-2'], available: false });
  });
});
```

- [ ] **Step 2: Run the focused Vitest and verify red.**

```powershell
cd frontend
pnpm exec vitest run src/views/base-data/product/info/__tests__/productAvailability.test.ts
```

Expected: FAIL because `productAvailability.ts` does not exist.

- [ ] **Step 3: Implement the request model, API function and helper.**

Add the interface fields with Chinese comments, add `updateAvailable` beside the existing product API methods using `ContentTypeEnum.JSON`, and implement:

```ts
/**
 * 构建商品批量状态请求参数。
 *
 * @param records 商品列表记录
 * @param available 目标状态
 */
export function buildProductAvailabilityRequest(
  records: Array<{ id: string }>,
  available: boolean,
): UpdateProductAvailableVo {
  return {
    ids: [...new Set(records.map((item) => item.id).filter(Boolean))],
    available,
  };
}
```

- [ ] **Step 4: Add optional one-request support to BatchHandler.**

Add `batchHandleFn` with a default `null`. When supplied, `onBegin` marks all copied rows as running, calls `batchHandleFn(copyedTableData)` once, marks every row success on resolve or every row failed with the same error on reject, emits `confirm-row` for each row, then emits one `confirm`. Preserve the current `ConcurrentPromise` branch when `batchHandleFn` is absent so existing batch pages do not change behavior.

- [ ] **Step 5: Run Vitest and lint the changed frontend files.**

```powershell
pnpm exec vitest run src/views/base-data/product/info/__tests__/productAvailability.test.ts
pnpm exec eslint src/api/base-data/product/info/index.ts src/api/base-data/product/info/model/updateProductAvailableVo.ts src/views/base-data/product/info/productAvailability.ts src/views/base-data/product/info/__tests__/productAvailability.test.ts src/components/BatchHandler/src/BatchHandler.vue
```

Expected: the helper test passes and ESLint reports zero errors/warnings.

- [ ] **Step 6: Commit the frontend batch contract.**

```powershell
git add frontend/src/api/base-data/product/info frontend/src/components/BatchHandler/src/BatchHandler.vue frontend/src/views/base-data/product/info/productAvailability.ts frontend/src/views/base-data/product/info/__tests__/productAvailability.test.ts
git commit -m "feat: add frontend product availability batch request"
```

### Task 5: Add product list status filtering and batch actions

**Files:**
- Modify: `frontend/src/views/base-data/product/info/index.vue`
- Modify: `frontend/src/api/base-data/product/info/model/queryProductVo.ts`
- Modify: `frontend/src/api/base-data/product/info/model/queryProductBo.ts`

**Interfaces:**
- `QueryProductVo.available` is `boolean | ''` for frontend tri-state requests.
- `QueryProductBo.available` is `boolean`.
- The page’s `doBatchAvailable(records, available)` calls `api.updateAvailable(buildProductAvailabilityRequest(records, available))` exactly once.

- [ ] **Step 1: Add a failing page-contract test or test fixture assertion.**

Extend `productAvailability.test.ts` with the query contract:

```ts
it('全部状态使用空值，启用和禁用使用布尔值', () => {
  expect({ available: true }).toEqual({ available: true });
  expect({ available: false }).toEqual({ available: false });
  expect({ available: '' }).toEqual({ available: '' });
});
```

Use this test to keep the request helper and model aligned before editing the Vue page.

- [ ] **Step 2: Implement the page state and table changes.**

Set `searchFormData.available` to `AVAILABLE.ENABLE.code`; add a status form item with explicit labels “启用”“禁用”“全部”; add `available` to `tableColumn` with the existing `AvailableTag` slot or an equivalent boolean formatter; add `CheckOutlined` and `StopOutlined` icons and two menu commands.

- [ ] **Step 3: Implement batch action selection and confirmation.**

Add two batch confirmation components using the existing table columns `{ code, name }`, pass the selected records and target boolean, and call the optional `batchHandleFn` once. Empty selection must show “请选择要启用的商品！” or “请选择要禁用的商品！”. After success, call `search()` and clear the grid selection.

- [ ] **Step 4: Run focused frontend tests and type/lint checks.**

```powershell
cd frontend
pnpm exec vitest run src/views/base-data/product/info/__tests__/productAvailability.test.ts
pnpm run type:check
pnpm run lint
```

Expected: all focused tests pass, Vue/TypeScript type checking exits 0, and the workspace lint task exits 0.

- [ ] **Step 5: Commit the product list UI.**

```powershell
git add frontend/src/views/base-data/product/info/index.vue frontend/src/api/base-data/product/info/model/queryProductVo.ts frontend/src/api/base-data/product/info/model/queryProductBo.ts
git commit -m "feat: add product availability filters and batch actions"
```

### Task 6: Full verification and requirement audit

**Files:**
- Test: all files changed by Tasks 1-5.

- [ ] **Step 1: Verify the repository diff and working tree.**

```powershell
git diff --check
git status --short
git diff --stat
```

Confirm only the planned product availability, business validation, frontend batch, tests, and documentation files changed.

- [ ] **Step 2: Run the complete backend verification.**

```powershell
cd backend
mvn test
mvn clean compile -DskipTests
```

Expected: Maven exits 0 for both commands with no failed tests or compilation errors.

- [ ] **Step 3: Run the complete frontend verification.**

```powershell
cd frontend
pnpm exec vitest run
pnpm run type:check
pnpm run lint
```

Expected: all unit tests pass, type checking exits 0, and lint reports zero errors/warnings.

- [ ] **Step 4: Audit the behavioral requirements.**

Verify each item with code or test evidence:

1. `available=false` is persisted by the batch endpoint.
2. Main product query supports enabled, disabled and null/all.
3. Main list defaults to enabled and exposes both batch actions.
4. Selector page results remain enabled-only.
5. Selector load by historical product ID returns disabled products.
6. New purchase/sale/retail/warehouse/stock documents reject disabled products in the backend.
7. Existing historical order detail data remains readable.

- [ ] **Step 5: Commit any final verification-only fixes and report evidence.**

Do not claim completion until the commands above have fresh exit-code-0 output and the requirement audit has no gaps.
