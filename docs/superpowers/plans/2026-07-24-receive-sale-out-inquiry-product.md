# 采购入库与销售出库询价商品标识展示 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在采购入库与销售出库的商品选择、批量添加、明细、查看、查询和明细导出中显示询价商品标识。

**Architecture:** 后端在既有商品关联查询中选择 `g.inquiry_product AS inquiry_product`，将字段通过 DTO、BO 和导出模型传递。前端复用一个纯函数将布尔值标准化成“是/否”及绿色/红色，所有表格使用相同插槽渲染。

**Tech Stack:** Spring Boot 2.2、MyBatis XML、EasyExcel、Vue 3、TypeScript、vxe-table、Vitest。

## Global Constraints

- 复用商品主数据 `inquiryProduct`，不修改采购收货或销售出库单据保存入参，也不新增冗余数据库列。
- `true` 显示绿色“是”；`false`、`null`、`undefined` 显示红色“否”。
- 后端异常使用 `DefaultClientException`；Controller 仅做参数校验和响应包装。
- 新增 Java 方法必须有中文注释；接口对象通过既有 MapStruct/DTO 映射链路返回。

---

### Task 1: 采购收货与销售出库的后端明细数据链路

**Files:**
- Modify: `backend/xingyun-sc/src/main/resources/mappers/purchase/ReceiveSheetMapper.xml`
- Modify: `backend/xingyun-sc/src/main/resources/mappers/sale/SaleOutSheetMapper.xml`
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/dto/purchase/receive/QueryReceiveSheetDetailDto.java`
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/dto/sale/out/QuerySaleOutSheetDetailDto.java`
- Modify: 对应 `ReceiveSheetFullDto`、`SaleOutSheetFullDto` 内的明细 DTO，以及前端生成的 `getReceiveSheetBo.ts`、`getSaleOutSheetBo.ts`、`queryReceiveSheetDetailBo.ts`、`querySaleOutSheetDetailBo.ts`
- Test: 现有 `backend/xingyun-sc/src/test/java` 下采购收货/销售出库 Mapper 或 Service 测试；若不存在则新增 `ReceiveSheetInquiryProductMappingTest` 与 `SaleOutSheetInquiryProductMappingTest`。

**Interfaces:**
- Produces: 所有采购收货、销售出库明细响应均包含 `Boolean inquiryProduct`。

- [ ] **Step 1: 写入失败测试**

```java
@Test
public void shouldMapInquiryProductFromProductForReceiveDetail() {
  QueryReceiveSheetDetailDto detail = receiveSheetMapper.queryDetail(queryVo).get(0);
  assertTrue(detail.getInquiryProduct());
}
```

为销售出库建立同构测试，并为 `false` 商品断言 `assertFalse`。

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend && mvn -pl xingyun-sc -Dtest=ReceiveSheetInquiryProductMappingTest,SaleOutSheetInquiryProductMappingTest test`

Expected: FAIL，因为 DTO 尚无 `getInquiryProduct()` 或 SQL 尚未返回该列。

- [ ] **Step 3: 最小化实现字段映射**

在所有返回商品明细的 SQL 片段中，紧邻商品名称列增加：

```xml
g.inquiry_product AS inquiry_product,
```

在采购收货及销售出库的查询明细 DTO、完整单据的内层明细 DTO/BO 增加：

```java
/** 是否询价商品 */
private Boolean inquiryProduct;
```

同步更新前端 API 接口：

```ts
/** 是否询价商品 */
inquiryProduct: boolean;
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend && mvn -pl xingyun-sc -Dtest=ReceiveSheetInquiryProductMappingTest,SaleOutSheetInquiryProductMappingTest test`

Expected: PASS，两个测试都从商品表读取正确布尔值。

- [ ] **Step 5: 提交**

```powershell
git add backend/xingyun-sc/src/main/resources/mappers/purchase/ReceiveSheetMapper.xml backend/xingyun-sc/src/main/resources/mappers/sale/SaleOutSheetMapper.xml backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/dto frontend/src/api/sc/purchase/receive frontend/src/api/sc/sale/out
git commit -m "feat: expose inquiry flag in receive and sale out details"
```

### Task 2: 明细导出增加“是否询价商品”

**Files:**
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/excel/purchase/receive/ReceiveSheetDetailExportModel.java`
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/excel/purchase/receive/ReceiveSheetDetailExportTaskWorker.java`
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/excel/sale/out/SaleOutSheetDetailExportModel.java`
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/excel/sale/out/SaleOutSheetDetailExportTaskWorker.java`
- Test: 对应明细导出模型单元测试。

**Interfaces:**
- Consumes: Task 1 的 `inquiryProduct` 查询结果。
- Produces: Excel 列 `是否询价商品`，值只可能为“是”或“否”。

- [ ] **Step 1: 写入失败测试**

```java
@Test
public void shouldFormatInquiryProductForDetailExport() {
  assertEquals("是", ReceiveSheetDetailExportModel.formatInquiryProduct(true));
  assertEquals("否", SaleOutSheetDetailExportModel.formatInquiryProduct(null));
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend && mvn -pl xingyun-sc -Dtest=ReceiveSheetDetailExportModelTest,SaleOutSheetDetailExportModelTest test`

Expected: FAIL，因为格式化方法与导出列不存在。

- [ ] **Step 3: 最小化实现导出字段**

在两个明细导出模型增加：

```java
/** 是否询价商品 */
@ExcelProperty("是否询价商品")
private String inquiryProduct;

/** 将询价标识转换为导出文本。 */
public static String formatInquiryProduct(Boolean inquiryProduct) {
  return Boolean.TRUE.equals(inquiryProduct) ? "是" : "否";
}
```

在两个 TaskWorker 将 `detail.getInquiryProduct()` 格式化后写入该字段。

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend && mvn -pl xingyun-sc -Dtest=ReceiveSheetDetailExportModelTest,SaleOutSheetDetailExportModelTest test`

Expected: PASS，真值导出“是”，假值和空值导出“否”。

- [ ] **Step 5: 提交**

```powershell
git add backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/excel/purchase/receive backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/excel/sale/out
git commit -m "feat: export inquiry flag in sheet details"
```

### Task 3: 前端统一询价商品显示工具与采购收货页面

**Files:**
- Create: `frontend/src/views/sc/components/inquiryProduct.ts`
- Modify: `frontend/src/views/sc/purchase/receive/add-require.vue`
- Modify: `frontend/src/views/sc/purchase/receive/add-un-require.vue`
- Modify: `frontend/src/views/sc/purchase/receive/modify-require.vue`
- Modify: `frontend/src/views/sc/purchase/receive/modify-un-require.vue`
- Modify: `frontend/src/views/sc/purchase/receive/detail.vue`
- Modify: `frontend/src/views/sc/purchase/receive/components/detail-list.vue`
- Modify: 采购收货所使用的批量添加商品组件。
- Test: `frontend/src/views/sc/components/__tests__/inquiryProduct.test.ts`。

**Interfaces:**
- Produces: `formatInquiryProduct(value: boolean | null | undefined): { text: '是' | '否'; className: 'inquiry-product-yes' | 'inquiry-product-no' }`。

- [ ] **Step 1: 写入失败测试**

```ts
import { formatInquiryProduct } from '../inquiryProduct';

it('将空值和假值显示为红色否', () => {
  expect(formatInquiryProduct(null)).toEqual({ text: '否', className: 'inquiry-product-no' });
  expect(formatInquiryProduct(false)).toEqual({ text: '否', className: 'inquiry-product-no' });
});

it('将真值显示为绿色是', () => {
  expect(formatInquiryProduct(true)).toEqual({ text: '是', className: 'inquiry-product-yes' });
});
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd frontend && pnpm vitest run src/views/sc/components/__tests__/inquiryProduct.test.ts`

Expected: FAIL，模块尚不存在。

- [ ] **Step 3: 实现工具和采购列**

```ts
export function formatInquiryProduct(value: boolean | null | undefined) {
  return value
    ? { text: '是' as const, className: 'inquiry-product-yes' as const }
    : { text: '否' as const, className: 'inquiry-product-no' as const };
}
```

在每个采购收货商品候选表、批量添加弹窗、可编辑明细表、详情表与查询明细表加入 `field: 'inquiryProduct', title: '是否询价商品'`；使用插槽输出 `formatInquiryProduct(row.inquiryProduct)` 的文字和类名。为两个类分别声明绿色与红色文本样式。

- [ ] **Step 4: 运行测试确认通过**

Run: `cd frontend && pnpm vitest run src/views/sc/components/__tests__/inquiryProduct.test.ts`

Expected: PASS，三种输入均产生预期文本和样式类。

- [ ] **Step 5: 提交**

```powershell
git add frontend/src/views/sc/components frontend/src/views/sc/purchase/receive
git commit -m "feat: show inquiry flag in receive pages"
```

### Task 4: 销售出库页面与批量添加弹窗

**Files:**
- Modify: `frontend/src/views/sc/sale/out/add-require.vue`
- Modify: `frontend/src/views/sc/sale/out/add-un-require.vue`
- Modify: `frontend/src/views/sc/sale/out/modify-require.vue`
- Modify: `frontend/src/views/sc/sale/out/modify-un-require.vue`
- Modify: `frontend/src/views/sc/sale/out/detail.vue`
- Modify: `frontend/src/views/sc/sale/out/components/detail-list.vue`
- Modify: 销售出库所使用的批量添加商品组件。
- Test: 对应销售出库组件测试，断言列定义含 `inquiryProduct`。

**Interfaces:**
- Consumes: Task 1 的 `inquiryProduct` 和 Task 3 的 `formatInquiryProduct`。

- [ ] **Step 1: 写入失败测试**

```ts
it('销售出库明细列包含是否询价商品', () => {
  expect(tableColumn.some((column) => column.field === 'inquiryProduct')).toBe(true);
});
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd frontend && pnpm vitest run src/views/sc/sale/out/components/__tests__/saleOutInquiryProduct.test.ts`

Expected: FAIL，列定义尚未包含该字段。

- [ ] **Step 3: 最小化实现销售列**

在每个销售出库的商品候选表、批量添加弹窗、明细表、详情和查询明细表中增加同名列，并复用：

```vue
<span :class="formatInquiryProduct(row.inquiryProduct).className">
  {{ formatInquiryProduct(row.inquiryProduct).text }}
</span>
```

确保没有把该字段写入 `SaleOutProductVo`，其仅用于展示。

- [ ] **Step 4: 运行测试确认通过**

Run: `cd frontend && pnpm vitest run src/views/sc/sale/out/components/__tests__/saleOutInquiryProduct.test.ts`

Expected: PASS，销售出库明细与批量添加列都具备 `inquiryProduct`。

- [ ] **Step 5: 提交**

```powershell
git add frontend/src/views/sc/sale/out
git commit -m "feat: show inquiry flag in sale out pages"
```

### Task 5: 全量回归验证

**Files:**
- Verify: 本计划涉及的全部文件。

- [ ] **Step 1: 后端编译与测试**

Run: `cd backend && mvn -pl xingyun-sc -am test`

Expected: BUILD SUCCESS。

- [ ] **Step 2: 前端测试与检查**

Run: `cd frontend && pnpm vitest run && pnpm run lint`

Expected: 全部测试通过，lint 无 error。

- [ ] **Step 3: 静态范围检查**

Run: `git diff --check HEAD~4..HEAD; rg -n "inquiryProduct" frontend/src/views/sc/purchase/receive frontend/src/views/sc/sale/out backend/xingyun-sc/src/main/java backend/xingyun-sc/src/main/resources/mappers`

Expected: 无空白错误；采购收货、销售出库的查询、查看、修改、批量添加和导出链路均能检索到字段。
