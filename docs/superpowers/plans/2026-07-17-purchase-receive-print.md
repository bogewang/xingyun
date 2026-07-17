# 采购入库打印功能 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为采购入库增加与销售出库一致的列表/详情打印入口，并让打印接口和模板字段说明完整覆盖采购入库单据模型。

**Architecture:** 采购入库继续使用独立的 `PrintReceiveSheetBo` 和 `/purchase/receive/sheet/print` 接口，不转换为销售出库 DTO。模板字段说明接口按 `bizType` 选择对应打印 BO，模板设置接口把 `bizType` 传给设计器；前端列表和详情复用现有打印预览运行时。

**Tech Stack:** Spring Boot 2.2.2、Java 8、JUnit 5、Vue 3、TypeScript、Vitest、vg-print、pnpm。

## Global Constraints

- Controller 只做参数校验和响应包装，业务编排放在 Service 层。
- 对外响应统一使用 `InvokeResult<T>`。
- 业务异常必须使用 `DefaultClientException`。
- Entity 映射使用 MapStruct，禁止新增手写重复转换逻辑。
- 所有新增方法添加中文注释，代码注释和解释使用中文。
- 不修改 `receive-sheet.ftl` 的模板视觉内容，模板由用户设计。
- 保留现有采购入库展示字段和已有模板字段路径，新增字段只能扩展响应，不破坏已有绑定。

---

### Task 1: 补齐采购入库打印 DTO 字段

**Files:**
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/bo/purchase/receive/PrintReceiveSheetBo.java`
- Test: `backend/xingyun-sc/src/test/java/com/lframework/xingyun/sc/bo/purchase/receive/PrintReceiveSheetBoTest.java`
- Reference: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/dto/purchase/receive/ReceiveSheetFullDto.java`

**Interfaces:**
- Consumes: `ReceiveSheetFullDto` root fields and `ReceiveSheetFullDto.OrderDetailDto` fields.
- Produces: `PrintReceiveSheetBo` with all current DTO fields plus existing display aliases.

- [ ] **Step 1: Write the failing parity test**

Create a TestNG test that reflects over the two DTO classes and asserts every non-static field in `ReceiveSheetFullDto` and its nested `OrderDetailDto` exists in the corresponding print BO. Keep the test aware of the existing transformed aliases: `orderDate`, `paymentDate`, `receiveDate` remain printable strings, while `receiveNum`/`purchasePrice`/`receiveAmount` remain display aliases for `orderNum`/`taxPrice`/`taxAmount`.

The test must explicitly require these missing root fields:

```java
assertThat(rootFieldNames).containsExactlyInAnyOrder(
    "id", "code", "scId", "supplierId", "purchaserId", "orderDate",
    "purchaseOrderId", "paymentDate", "receiveDate", "totalNum",
    "totalGiftNum", "totalAmount", "paidAmount", "description", "createBy",
    "createTime", "updateBy", "updateTime", "approveBy", "approveTime",
    "status", "refuseReason", "settleStatus", "details", "scCode", "scName",
    "supplierCode", "supplierName", "purchaserName", "purchaseOrderCode",
    "unpaidAmount"
);
```

The nested assertion must require the original fields `id`, `productId`, `orderNum`, `unitId`, `unitName`, `conversionRate`, `businessNum`, `taxPrice`, `taxAmount`, `isGift`, `taxRate`, `description`, `orderNo`, `purchaseOrderDetailId`, `productionDate`, plus `productCode`, `productName`, `skuCode`, `externalCode`, `receiveNum`, `purchasePrice`, and `receiveAmount`.

- [ ] **Step 2: Run the focused test and verify the expected failure**

Run:

```powershell
cd backend
mvn -pl xingyun-sc -Dtest=PrintReceiveSheetBoTest test
```

Expected: FAIL because the current print BO does not declare the missing `ReceiveSheetFullDto` fields.

- [ ] **Step 3: Add the missing root and detail fields**

Add the fields with the same names and compatible types as `ReceiveSheetFullDto`; keep converted date fields as `String` because the current print response formats dates before serialization. Add `@ApiModelProperty` Chinese descriptions to every new field. Do not remove or rename current display fields.

The nested `OrderDetailBo` must copy the original DTO fields through the existing `BaseBo` initialization path, then continue assigning the existing product display fields and aliases in `afterInit`.

- [ ] **Step 4: Run the focused test and verify it passes**

Run the same Maven command. Expected: PASS with zero failures.

- [ ] **Step 5: Commit the DTO change**

```powershell
git add backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/bo/purchase/receive/PrintReceiveSheetBo.java backend/xingyun-sc/src/test/java/com/lframework/xingyun/sc/bo/purchase/receive/PrintReceiveSheetBoTest.java
git commit -m "feat: 补齐采购入库打印字段"
```

### Task 2: 按业务类型返回打印模板字段说明

**Files:**
- Modify: `backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/service/print/PrintTemplateService.java`
- Modify: `backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/impl/print/PrintTemplateServiceImpl.java`
- Modify: `backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/controller/PrintTemplateController.java`
- Modify: `backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/bo/print/GetPrintTemplateSettingBo.java`
- Test: `backend/xingyun-basedata/src/test/java/com/lframework/xingyun/basedata/impl/print/PrintTemplateServiceImplTest.java`

**Interfaces:**
- Consumes: optional `bizType` query parameter on `GET /basedata/print/template/fieldDesc`.
- Produces: `PrintTemplateService#getFieldDesc(String bizType)` and a setting response containing `bizType`.

- [ ] **Step 1: Write the failing field-selection tests**

Add TestNG tests for the service’s public field-description method:

```java
@Test
void 采购收货单业务类型返回采购入库字段() {
    List<PrintTemplateColumnDescription> fields = service.getFieldDesc("2");
    List<String> names = fields.stream()
        .map(PrintTemplateColumnDescription::getColumnName)
        .collect(Collectors.toList());

    Assert.assertTrue(names.contains("supplierCode"));
    Assert.assertTrue(names.contains("purchaserName"));
    Assert.assertTrue(names.contains("purchaseOrderCode"));
    Assert.assertTrue(names.contains("details[].receiveNum"));
    Assert.assertTrue(names.contains("details[].productionDate"));
}

@Test
void 销售出库业务类型返回销售出库字段() {
    List<PrintTemplateColumnDescription> fields = service.getFieldDesc("7");
    List<String> names = fields.stream()
        .map(PrintTemplateColumnDescription::getColumnName)
        .collect(Collectors.toList());

    Assert.assertTrue(names.contains("customerName"));
    Assert.assertTrue(names.contains("details[].orderNum"));
}

@Test
void 未传业务类型保持销售字段兼容行为() {
    List<PrintTemplateColumnDescription> fields = service.getFieldDesc(null);
    List<String> names = fields.stream()
        .map(PrintTemplateColumnDescription::getColumnName)
        .collect(Collectors.toList());

    Assert.assertTrue(names.contains("customerName"));
}
```

Add `import java.util.stream.Collectors;`, `import org.testng.Assert;`, and `import org.testng.annotations.Test;`. Use the existing reflection-based field description generation; the test must not start the full application or access a database.

- [ ] **Step 2: Run the focused tests and verify the expected failure**

Run:

```powershell
cd backend
mvn -pl xingyun-basedata -Dtest=PrintTemplateServiceImplTest test
```

Expected: the focused test is red because the service currently has no `getFieldDesc(String)` overload and always reflects `PrintSaleOrderBo`; if the test does not compile after being added, correct only the test setup/imports before proceeding until the failure is caused by the missing behavior.

- [ ] **Step 3: Implement business-type class selection**

Change the service contract to:

```java
List<PrintTemplateColumnDescription> getFieldDesc(String bizType);
```

In `PrintTemplateServiceImpl`, map `"2"` to `com.lframework.xingyun.sc.bo.purchase.receive.PrintReceiveSheetBo`, map `"7"` and blank values to `com.lframework.xingyun.sc.bo.sale.PrintSaleOrderBo`, and throw `new DefaultClientException("不支持的打印业务类型！")` for other nonblank values. Keep class loading through `Class.forName` so `xingyun-basedata` does not create a compile-time dependency on `xingyun-sc`.

Split the existing hard-coded demo map into one method per supported BO. The purchase map must include examples for `supplierCode`, `purchaserName`, `purchaseOrderCode`, `receiveDate`, `details[].receiveNum`, `details[].purchasePrice`, `details[].receiveAmount`, and all newly exposed DTO fields. The existing sale demo values must remain unchanged.

Keep the existing `appendFieldDesc` reflection behavior so `@ApiModelProperty` descriptions and nested `details[]` paths are generated consistently.

- [ ] **Step 4: Pass the optional query parameter through the controller**

Change the controller method to accept `String bizType` as an optional query parameter and call `printTemplateService.getFieldDesc(bizType)`. Preserve the existing `try/catch`, error log, and `InvokeResultBuilder.fail` response wrapper.

- [ ] **Step 5: Include `bizType` in template setting response**

Add a `String bizType` field to `GetPrintTemplateSettingBo`, annotate it with `@ApiModelProperty("业务类型")`, and update both conversion stages so it cannot be lost while parsing the JSON fields:

```java
return super.convert(dto, GetPrintTemplateSettingBo::getTemplateJson,
    GetPrintTemplateSettingBo::getDemoData, GetPrintTemplateSettingBo::getBizType);
```

Then assign `this.bizType = dto.getBizType();` in `afterInit`. Do not add a separate database query.

- [ ] **Step 6: Run the focused tests and verify they pass**

Run:

```powershell
cd backend
mvn -pl xingyun-basedata -Dtest=PrintTemplateServiceImplTest test
```

Expected: PASS with zero failures.

- [ ] **Step 7: Commit the backend field-description change**

```powershell
git add backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/service/print/PrintTemplateService.java backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/impl/print/PrintTemplateServiceImpl.java backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/controller/PrintTemplateController.java backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/bo/print/GetPrintTemplateSettingBo.java backend/xingyun-basedata/src/test/java/com/lframework/xingyun/basedata/impl/print/PrintTemplateServiceImplTest.java
git commit -m "feat: 按业务类型返回打印字段"
```

### Task 3: 同步前端打印模型和模板设计器业务类型

**Files:**
- Modify: `frontend/src/api/sc/purchase/receive/model/printReceiveSheetBo.ts`
- Modify: `frontend/src/api/base-data/print-template/model/getPrintTemplateSettingBo.ts`
- Modify: `frontend/src/api/base-data/print-template/index.ts`
- Modify: `frontend/src/components/PrintDesigner/src/PrintDesigner.vue`
- Modify: `frontend/src/views/base-data/print-template/setting.vue`
- Test: `frontend/src/api/sc/purchase/receive/model/__tests__/printReceiveSheetBo.test.ts`
- Test: `frontend/src/api/base-data/print-template/__tests__/index.test.ts`

**Interfaces:**
- Consumes: backend `PrintReceiveSheetBo`, `GetPrintTemplateSettingBo.bizType`, and `/fieldDesc?bizType=...`.
- Produces: `PrintDesigner` prop `bizType?: string | number`; `getFieldDesc(bizType?: string | number)` sends the current type as a query parameter.

- [ ] **Step 1: Write the failing TypeScript model test**

Create a typed fixture using `satisfies PrintReceiveSheetBo`. Include all root fields from `ReceiveSheetFullDto`, all existing display aliases, and a detail containing every original and display field. Assert the fixture contains representative values such as `purchaseOrderId`, `totalGiftNum`, `settleStatus`, `details[0].productionDate`, and `details[0].receiveNum`.

This test must fail type-checking before implementation because the current interface does not declare the missing fields.

- [ ] **Step 2: Run the model test/type check and verify the expected failure**

Run:

```powershell
cd frontend
pnpm exec vitest run src/api/sc/purchase/receive/model/__tests__/printReceiveSheetBo.test.ts
pnpm run type:check
```

Expected: the fixture cannot satisfy the current `PrintReceiveSheetBo` interface.

- [ ] **Step 3: Add the complete frontend print response types**

Add the root fields `id`, `scId`, `supplierId`, `purchaserId`, `purchaseOrderId`, `orderDate`, `totalNum`, `totalGiftNum`, `totalAmount`, `paidAmount`, `unpaidAmount`, `updateBy`, `updateTime`, `status`, `refuseReason`, and `settleStatus`, while retaining current display fields. Add detail fields `id`, `productId`, `orderNum`, `unitId`, `unitName`, `conversionRate`, `businessNum`, `taxPrice`, `taxAmount`, `isGift`, `taxRate`, `description`, `orderNo`, `purchaseOrderDetailId`, and `productionDate`, while retaining current aliases.

Use `string` for formatted dates and IDs, `number` for monetary/quantity values, `boolean` for gift flags, and `number` for numeric backend enum codes, matching existing receive-sheet models.

- [ ] **Step 4: Add the failing field-description request test**

Mock `defHttp.get`, call `getFieldDesc(2)`, and assert it invokes:

```ts
expect(defHttp.get).toHaveBeenCalledWith(
  { url: '/basedata/print/template/fieldDesc', params: { bizType: '2' } },
  { region: 'cloud-api' },
);
```

The test should also call `getFieldDesc()` and assert the request omits `params` for backward compatibility. It must fail before the API function accepts a parameter.

- [ ] **Step 5: Implement the frontend API and designer prop**

Change `getFieldDesc` to accept `bizType?: string | number`; when provided, pass `params: { bizType: String(bizType) }`, otherwise preserve the current request shape.

Add `bizType` to `GetPrintTemplateSettingBo`. In `setting.vue`, pass `:biz-type="formData.bizType"` to `<print-designer>`. In `PrintDesigner.vue`, declare the prop and call `printTemplateApi.getFieldDesc(props.bizType)` when opening field documentation. The existing demo data normalization and template save behavior must remain unchanged.

- [ ] **Step 6: Run frontend focused tests and type check**

Run:

```powershell
cd frontend
pnpm exec vitest run src/api/sc/purchase/receive/model/__tests__/printReceiveSheetBo.test.ts src/api/base-data/print-template/__tests__/index.test.ts
pnpm run type:check
```

Expected: all focused tests pass and TypeScript reports no errors.

- [ ] **Step 7: Commit the frontend model/designer change**

```powershell
git add frontend/src/api/sc/purchase/receive/model/printReceiveSheetBo.ts frontend/src/api/base-data/print-template/model/getPrintTemplateSettingBo.ts frontend/src/api/base-data/print-template/index.ts frontend/src/components/PrintDesigner/src/PrintDesigner.vue frontend/src/views/base-data/print-template/setting.vue frontend/src/api/sc/purchase/receive/model/__tests__/printReceiveSheetBo.test.ts frontend/src/api/base-data/print-template/__tests__/index.test.ts
git commit -m "feat: 补齐采购入库打印模型字段"
```

### Task 4: 增加采购入库列表和详情打印入口

**Files:**
- Create: `frontend/src/views/sc/purchase/receive/print.ts`
- Modify: `frontend/src/views/sc/purchase/receive/components/sheet-list.vue`
- Modify: `frontend/src/views/sc/purchase/receive/detail.vue`
- Test: `frontend/src/views/sc/purchase/receive/__tests__/print.test.ts`

**Interfaces:**
- Consumes: `api.print(id)`, `printMix.vgPrintPreview`, `PRINT_TYPE.RECEIVE_SHEET.code`, and `PrintReceiveSheetBo`.
- Produces: list action and detail footer button that open the purchase receive print preview.

- [ ] **Step 1: Write the failing print-flow test**

Create the test against the new pure helper. The test must assert the actual flow contract:

```ts
await previewReceiveSheetPrint('receive-1', loadPrintData, preview);

expect(loadPrintData).toHaveBeenCalledWith('receive-1');
expect(preview).toHaveBeenCalledWith(PRINT_TYPE.RECEIVE_SHEET.code, printData);
```

Do not test only that a mock was called without checking the business type.

- [ ] **Step 2: Run the focused test and verify it fails**

Run:

```powershell
cd frontend
pnpm exec vitest run src/views/sc/purchase/receive/__tests__/print.test.ts
```

Expected: FAIL because `frontend/src/views/sc/purchase/receive/print.ts` and `previewReceiveSheetPrint` do not exist yet.

- [ ] **Step 3: Implement the pure print-flow helper**

Create `print.ts` with this contract:

```ts
import type { PrintReceiveSheetBo } from '@/api/sc/purchase/receive/model/printReceiveSheetBo';
import { PRINT_TYPE } from '@/enums/biz/printType';

export type ReceiveSheetPrintLoader = (id: string) => Promise<PrintReceiveSheetBo>;
export type ReceiveSheetPrintPreview = (
  type: number,
  data: PrintReceiveSheetBo,
) => Promise<void> | void;

/**
 * 加载采购入库打印数据并打开采购入库业务类型的打印预览。
 */
export async function previewReceiveSheetPrint(
  id: string,
  load: ReceiveSheetPrintLoader,
  preview: ReceiveSheetPrintPreview,
): Promise<void> {
  const data = await load(id);
  await preview(PRINT_TYPE.RECEIVE_SHEET.code, data);
}
```

Import `PRINT_TYPE` from `@/enums/biz/printType` in the actual file. The helper must not change the response data or convert it to a sale DTO.

- [ ] **Step 4: Add the list print action**

In `sheet-list.vue`, import `printMix` and `previewReceiveSheetPrint`, add `printMix` to the component mixins, and add a method equivalent to:

```js
async printOrder(row) {
  this.loading = true;
  try {
    await previewReceiveSheetPrint(row.id, api.print, (type, data) => {
      return this.vgPrintPreview(type, data);
    });
  } finally {
    this.loading = false;
  }
}
```

Add a `label: '打印'` action after “导出明细” in `createActions(row)`. Use the existing row action permission behavior; the backend query permission remains the authorization boundary.

- [ ] **Step 5: Add the detail footer button**

In `detail.vue`, import `previewReceiveSheetPrint` and update the existing `print()` method to call the helper with `api.print` and a callback to `this.vgPrintPreview`. Keep the existing `OrderPrintDialog` registration. Add:

```vue
<a-button type="primary" :loading="loading" @click="print">打印</a-button>
```

before “导出明细”; the helper supplies `PRINT_TYPE.RECEIVE_SHEET.code` and the existing `api.print(this.id)` supplies the purchase receive response.

- [ ] **Step 6: Run the focused print-flow test and lint**

Run:

```powershell
cd frontend
pnpm exec vitest run src/views/sc/purchase/receive/__tests__/print.test.ts
pnpm run lint
```

Expected: the focused test passes and lint finishes with zero errors.

- [ ] **Step 7: Commit the UI print入口 change**

```powershell
git add frontend/src/views/sc/purchase/receive/print.ts frontend/src/views/sc/purchase/receive/components/sheet-list.vue frontend/src/views/sc/purchase/receive/detail.vue frontend/src/views/sc/purchase/receive/__tests__/print.test.ts
git commit -m "feat: 增加采购入库打印入口"
```

### Task 5: Full verification and requirements audit

**Files:**
- Reference: `docs/superpowers/specs/2026-07-17-purchase-receive-print-design.md`
- Reference: `AGENTS.md`, `frontend/AGENTS.md`, `.claude/rules/*.md`

- [ ] **Step 1: Run backend module tests**

```powershell
cd backend
mvn -pl xingyun-basedata,xingyun-sc test
```

Expected: exit code 0 and zero test failures.

- [ ] **Step 2: Run frontend unit tests and type check**

```powershell
cd frontend
pnpm run test:unit
pnpm run type:check
```

Expected: exit code 0, all tests pass, and no TypeScript errors.

- [ ] **Step 3: Run frontend lint**

```powershell
cd frontend
pnpm run lint
```

Expected: exit code 0 with no lint errors or warnings.

- [ ] **Step 4: Inspect the final diff and verify scope**

```powershell
git diff HEAD~4..HEAD --stat
git status --short
```

Confirm that the changes include only the approved backend DTO/field-description changes, frontend types/designer changes, purchase receive print入口/tests, and that `.superpowers/sdd/final-review.md` remains untouched and untracked.

- [ ] **Step 5: Report verification evidence**

Report the exact test/lint/type-check commands and exit results. Do not claim completion if any command fails.
