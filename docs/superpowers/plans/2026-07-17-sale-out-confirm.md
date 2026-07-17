# 销售出库验收字段 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为销售出库单及明细增加可录入的验收数量、后端计算的验收金额，并贯通数据库、后端接口、前端页面和 Excel 导入导出。

**Architecture:** 在租户数据库中持久化单据头和明细的 `confirm_num`/`confirm_amt`。新增独立的验收计算器处理空值、6 位精度和明细汇总，销售出库 Service 在创建、修改、批量调价和导入入口调用它，前端通过共享纯函数即时显示但不作为金额可信来源。

**Tech Stack:** Spring Boot 2.2.2、Java 8、MyBatis-Plus 3.4.2、MySQL 5.7、EasyExcel 2.2.10、Vue 3、TypeScript、vxe-table、Vitest/现有前端测试工具。

## Global Constraints

- `confirmAmt = confirmNum × taxPrice`，其中 `confirmNum` 是界面交易单位数量，金额保存 6 位小数。
- 单据头 `confirmNum`、`confirmAmt` 只能由明细汇总，不能信任客户端传入的头部值或金额值。
- 历史数据两个验收字段初始化为 0，不从 `orderNum` 回填。
- Controller 只做参数校验和响应包装，业务编排放在 Service，事务只放 Service 层。
- 对外响应统一使用 `InvokeResult<T>`，业务异常使用 `DefaultClientException`，禁止 `RuntimeException`。
- Entity 映射使用 MapStruct 或现有对象转换链路，禁止新增重复手写转换逻辑。
- 所有新增 Java/TypeScript 方法添加中文注释；代码注释和解释使用中文。
- 后端核心业务先写失败测试，再写最小实现；每个任务独立运行测试并提交。
- 仅修改销售出库验收字段相关文件，不改变销售金额、库存扣减、结算金额和退货数量的业务含义。

## File Map

### Create

- `backend/xingyun-api/src/main/resources/db/migration/tenant/V2.5-sale-out-confirm.sql`：租户表字段迁移。
- `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetConfirmCalculator.java`：明细金额计算和单据验收汇总。
- `backend/xingyun-sc/src/test/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetConfirmCalculatorTest.java`：计算器单元测试。
- `frontend/src/views/sc/sale/out/components/saleOutConfirm.ts`：前端验收金额/汇总纯函数。
- `frontend/src/views/sc/sale/out/components/__tests__/saleOutConfirm.test.ts`：前端验收计算测试。

### Modify — backend model/query

- `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/entity/SaleOutSheet.java`：单据头实体字段。
- `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/entity/SaleOutSheetDetail.java`：明细实体字段。
- `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/vo/sale/out/SaleOutProductVo.java`：新增/修改请求明细验收数量。
- `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/dto/sale/out/SaleOutSheetFullDto.java`：单据详情响应及嵌套明细。
- `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/dto/sale/out/QuerySaleOutSheetDetailDto.java`：明细查询响应。
- `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/dto/sale/out/SaleOutSheetWithReturnDto.java`：关联退货查询响应。
- `backend/xingyun-sc/src/main/resources/mappers/sale/SaleOutSheetMapper.xml`：单据、详情、退货和明细查询结果映射/字段选择。
- `backend/xingyun-sc/src/main/resources/mappers/sale/SaleOutSheetDetailMapper.xml`：明细实体查询映射/字段选择。

### Modify — backend service/import/export

- `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetServiceImpl.java`：保存、修改、调价、导入、导出数据组装和汇总。
- `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/excel/sale/out/SaleOutSheetImportModel.java`：普通导入验收数量列。
- `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/excel/sale/out/SaleOutSheetQueryImportModel.java`：按销售日期/客户导入继承验收数量列。
- `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/excel/sale/out/SaleOutSheetExportModel.java`：单据导出验收列。
- `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/excel/sale/out/SaleOutSheetDetailExportModel.java`：明细及按天汇总导出验收列。
- `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/excel/sale/out/SaleOutSheetSalesExportHelper.java`：销售单格式导出验收列。
- `backend/xingyun-sc/src/test/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetServiceImplTest.java`：导入验收数量校验测试。

### Modify — frontend API and views

- `frontend/src/api/sc/sale/out/model/saleOutProductVo.ts`：请求明细验收数量。
- `frontend/src/api/sc/sale/out/model/getSaleOutSheetBo.ts`：详情头和嵌套明细验收字段。
- `frontend/src/api/sc/sale/out/model/querySaleOutSheetBo.ts`：列表头验收字段。
- `frontend/src/api/sc/sale/out/model/querySaleOutSheetDetailBo.ts`：明细列表验收字段。
- `frontend/src/views/sc/sale/out/add-require.vue`、`add-un-require.vue`：新增页面验收输入/显示/汇总。
- `frontend/src/views/sc/sale/out/modify-require.vue`、`modify-un-require.vue`：修改页面验收输入/显示/汇总。
- `frontend/src/views/sc/sale/out/approve.vue`：审核查看页面验收显示/汇总。
- `frontend/src/views/sc/sale/out/detail.vue`、`components/detail-list.vue`：详情和明细查询显示/汇总。
- `frontend/src/views/sc/sale/out/components/sheet-list.vue`：单据列表验收列。
- `frontend/src/components/Importor/SaleOutSheetImporter.vue`、`SaleOutSheetQueryImporter.vue`：导入说明覆盖验收数量。

---

### Task 1: 建立验收计算器并用测试锁定规则

**Files:**
- Create: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetConfirmCalculator.java`
- Create: `backend/xingyun-sc/src/test/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetConfirmCalculatorTest.java`

**Interfaces:**
- Produces `SaleOutSheetConfirmCalculator.calculateAmount(BigDecimal, BigDecimal): BigDecimal`，空数量/空单价按 0 处理，返回 6 位小数。
- Produces `SaleOutSheetConfirmCalculator.calculateDetail(SaleOutSheetDetail): void`，将明细 `confirmNum` 规范化并写入 `confirmAmt`。
- Produces `SaleOutSheetConfirmCalculator.calculateSheet(SaleOutSheet, List<SaleOutSheetDetail>): void`，按明细汇总头部两个字段。

- [ ] **Step 1: Write failing calculator tests**

在 `SaleOutSheetConfirmCalculatorTest` 中写入以下测试，测试类与实现放在同一 package 以便验证所有公开行为：

```java
@Test
void calculateAmountShouldUseConfirmNumAndTaxPriceWithSixDecimals() {
  BigDecimal result = SaleOutSheetConfirmCalculator.calculateAmount(
      new BigDecimal("1.234567"), new BigDecimal("2.345678"));

  Assert.assertEquals(result, new BigDecimal("2.895897"));
}

@Test
void calculateAmountShouldTreatNullAsZero() {
  Assert.assertEquals(SaleOutSheetConfirmCalculator.calculateAmount(null, new BigDecimal("2")),
      BigDecimal.ZERO.setScale(6));
  Assert.assertEquals(SaleOutSheetConfirmCalculator.calculateAmount(new BigDecimal("2"), null),
      BigDecimal.ZERO.setScale(6));
}

@Test
void calculateSheetShouldSumDetailConfirmValuesAndIgnoreExistingHeaderValues() {
  SaleOutSheet sheet = new SaleOutSheet();
  sheet.setConfirmNum(new BigDecimal("99"));
  sheet.setConfirmAmt(new BigDecimal("999"));

  SaleOutSheetDetail first = new SaleOutSheetDetail();
  first.setConfirmNum(new BigDecimal("2"));
  first.setTaxPrice(new BigDecimal("3.25"));
  SaleOutSheetDetail second = new SaleOutSheetDetail();
  second.setConfirmNum(new BigDecimal("1.5"));
  second.setTaxPrice(new BigDecimal("4"));

  SaleOutSheetConfirmCalculator.calculateSheet(sheet, Arrays.asList(first, second));

  Assert.assertEquals(first.getConfirmAmt(), new BigDecimal("6.500000"));
  Assert.assertEquals(second.getConfirmAmt(), new BigDecimal("6.000000"));
  Assert.assertEquals(sheet.getConfirmNum(), new BigDecimal("3.500000"));
  Assert.assertEquals(sheet.getConfirmAmt(), new BigDecimal("12.500000"));
}
```

- [ ] **Step 2: Run the focused test and verify it fails**

Run:

```powershell
cd backend
mvn -pl xingyun-sc -am -Dtest=SaleOutSheetConfirmCalculatorTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because `SaleOutSheetConfirmCalculator` and its methods do not exist yet.

- [ ] **Step 3: Implement the minimal calculator**

实现以下核心逻辑，新增方法均添加中文 Javadoc：

```java
public final class SaleOutSheetConfirmCalculator {

  private static final int SCALE = 6;

  private SaleOutSheetConfirmCalculator() {
  }

  /** 计算明细验收金额，空值按零处理并保留六位小数。 */
  public static BigDecimal calculateAmount(BigDecimal confirmNum, BigDecimal taxPrice) {
    BigDecimal quantity = confirmNum == null ? BigDecimal.ZERO : confirmNum;
    BigDecimal price = taxPrice == null ? BigDecimal.ZERO : taxPrice;
    return quantity.multiply(price).setScale(SCALE, RoundingMode.HALF_UP);
  }

  /** 根据验收数量和销售单价刷新明细验收金额。 */
  public static void calculateDetail(SaleOutSheetDetail detail) {
    detail.setConfirmNum(detail.getConfirmNum() == null
        ? BigDecimal.ZERO.setScale(SCALE) : detail.getConfirmNum());
    detail.setConfirmAmt(calculateAmount(detail.getConfirmNum(), detail.getTaxPrice()));
  }

  /** 根据全部明细刷新单据头验收数量和验收金额。 */
  public static void calculateSheet(SaleOutSheet sheet, List<SaleOutSheetDetail> details) {
    BigDecimal totalNum = BigDecimal.ZERO.setScale(SCALE);
    BigDecimal totalAmt = BigDecimal.ZERO.setScale(SCALE);
    for (SaleOutSheetDetail detail : details) {
      calculateDetail(detail);
      totalNum = totalNum.add(detail.getConfirmNum());
      totalAmt = totalAmt.add(detail.getConfirmAmt());
    }
    sheet.setConfirmNum(totalNum);
    sheet.setConfirmAmt(totalAmt.setScale(SCALE, RoundingMode.HALF_UP));
  }
}
```

- [ ] **Step 4: Run the focused test and verify it passes**

运行同一 Maven 命令，预期 `SaleOutSheetConfirmCalculatorTest` 全部 PASS。

- [ ] **Step 5: Commit**

```powershell
git add backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetConfirmCalculator.java backend/xingyun-sc/src/test/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetConfirmCalculatorTest.java
git commit -m "feat: 增加销售出库验收计算器"
```

### Task 2: 增加数据库字段并贯通后端实体、DTO 与 MyBatis 查询

**Files:**
- Create: `backend/xingyun-api/src/main/resources/db/migration/tenant/V2.5-sale-out-confirm.sql`
- Modify: `SaleOutSheet.java`、`SaleOutSheetDetail.java`、`SaleOutProductVo.java`、`SaleOutSheetFullDto.java`、`QuerySaleOutSheetDetailDto.java`、`SaleOutSheetWithReturnDto.java`
- Modify: `SaleOutSheetMapper.xml`、`SaleOutSheetDetailMapper.xml`

**Interfaces:**
- Produces database columns `confirm_num`/`confirm_amt` in both tables, defaulting to zero.
- Produces Java properties `confirmNum`/`confirmAmt` on entity and response objects; request product object exposes only `confirmNum` as the editable value.

- [ ] **Step 1: Add the migration file**

创建 `V2.5-sale-out-confirm.sql`，内容为：

```sql
ALTER TABLE `tbl_sale_out_sheet`
  ADD COLUMN `confirm_num` decimal(24,6) NOT NULL DEFAULT '0.000000' COMMENT '验收数量',
  ADD COLUMN `confirm_amt` decimal(24,6) NOT NULL DEFAULT '0.000000' COMMENT '验收金额';

ALTER TABLE `tbl_sale_out_sheet_detail`
  ADD COLUMN `confirm_num` decimal(24,6) NOT NULL DEFAULT '0.000000' COMMENT '验收数量',
  ADD COLUMN `confirm_amt` decimal(24,6) NOT NULL DEFAULT '0.000000' COMMENT '验收金额';
```

- [ ] **Step 2: Add Java properties with Chinese comments**

在 `SaleOutSheet` 和 `SaleOutSheetDetail` 的金额/数量相关字段附近分别加入：

```java
/** 验收数量，使用明细交易单位。 */
private BigDecimal confirmNum;

/** 验收金额，由验收数量乘销售单价计算。 */
private BigDecimal confirmAmt;
```

在 `SaleOutProductVo` 只添加 `confirmNum`，并为其加 `@ApiModelProperty("验收数量")`；不把 `confirmAmt` 作为请求输入字段。

- [ ] **Step 3: Extend DTOs and nested detail DTOs**

在 `SaleOutSheetFullDto` 的单据字段和 `SheetDetailDto` 的明细字段、`QuerySaleOutSheetDetailDto`、`SaleOutSheetWithReturnDto` 的对应明细字段加入两个 `BigDecimal` 属性和中文注释，保持 Bean 映射属性名为 `confirmNum`/`confirmAmt`。

- [ ] **Step 4: Extend explicit MyBatis result maps and SELECT columns**

在两个 XML 的实体结果映射和 SQL 查询中加入字段；带前缀的嵌套明细必须使用唯一别名：

```xml
<result column="confirm_num" property="confirmNum"/>
<result column="confirm_amt" property="confirmAmt"/>
<result column="detail_confirm_num" property="confirmNum"/>
<result column="detail_confirm_amt" property="confirmAmt"/>
```

同时在 `SaleOutSheetMapper.xml` 的 `SaleOutSheetDto_sql`、`SaleOutSheetFullDto_sql`、明细查询 SQL，以及退货关联查询所需的明细 SELECT 中选择对应列，避免只改实体导致查询响应丢字段。

- [ ] **Step 5: Compile the backend module**

Run:

```powershell
cd backend
mvn -pl xingyun-sc -am -DskipTests compile
```

Expected: compile succeeds, and MyBatis XML parsing reports no unknown result properties.

- [ ] **Step 6: Commit**

```powershell
git add backend/xingyun-api/src/main/resources/db/migration/tenant/V2.5-sale-out-confirm.sql backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/entity/SaleOutSheet.java backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/entity/SaleOutSheetDetail.java backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/vo/sale/out/SaleOutProductVo.java backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/dto/sale/out backend/xingyun-sc/src/main/resources/mappers/sale/SaleOutSheetMapper.xml backend/xingyun-sc/src/main/resources/mappers/sale/SaleOutSheetDetailMapper.xml
git commit -m "feat: 增加销售出库验收字段模型"
```

### Task 3: 在 Service 保存、修改、调价和导入链路中统一重算

**Files:**
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetServiceImpl.java`
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/excel/sale/out/SaleOutSheetImportModel.java`
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/excel/sale/out/SaleOutSheetQueryImportModel.java`
- Modify: `backend/xingyun-sc/src/test/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetServiceImplTest.java`

**Interfaces:**
- Consumes: `SaleOutProductVo.confirmNum` and calculator methods from Task 1.
- Produces: every created/updated/imported `SaleOutSheet` has recalculated detail and header values; import models expose `confirmNum`.

- [ ] **Step 1: Write failing import validation tests**

在 `SaleOutSheetServiceImplTest` 中将测试工厂扩展为 `createModel(orderNum, taxPrice, confirmNum)`，并添加：

```java
@Test
void validateImportNumbersShouldRejectNegativeConfirmNum() {
  SaleOutSheetImportModel model = createModel(BigDecimal.ONE, BigDecimal.ONE,
      new BigDecimal("-0.000001"));

  List<String> errors = SaleOutSheetServiceImpl.validateImportNumbers(model);

  Assert.assertTrue(errors.contains("第2行“验收数量”不允许小于0"));
}

@Test
void validateImportNumbersShouldRejectConfirmNumWithMoreThanSixDecimals() {
  SaleOutSheetImportModel model = createModel(BigDecimal.ONE, BigDecimal.ONE,
      new BigDecimal("1.1234567"));

  List<String> errors = SaleOutSheetServiceImpl.validateImportNumbers(model);

  Assert.assertTrue(errors.contains("第2行“验收数量”最多允许6位小数"));
}
```

- [ ] **Step 2: Run the service test and verify it fails**

```powershell
cd backend
mvn -pl xingyun-sc -am -Dtest=SaleOutSheetServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because the import model and validation do not yet contain `confirmNum` rules.

- [ ] **Step 3: Add import fields and validation**

在 `SaleOutSheetImportModel` 加入：

```java
/** 验收数量，金额由后端根据验收数量和销售单价计算。 */
@ExcelProperty("验收数量")
private BigDecimal confirmNum;
```

`SaleOutSheetQueryImportModel` 继承该字段，不重复声明“验收金额”。在 `validateImportNumbers` 中追加非负和 6 位精度检查；在 `normalizeQueryImportNumbers` 中将空验收数量规范化为 `BigDecimal.ZERO`。保留现有“数量”“单价”校验。

- [ ] **Step 4: Recalculate during create and update**

在 `SaleOutSheetServiceImpl.create(SaleOutSheet, CreateSaleOutSheetVo)` 中，明细设置交易单位数量后加入：

```java
detail.setConfirmNum(productVo.getConfirmNum());
SaleOutSheetConfirmCalculator.calculateDetail(detail);
```

循环结束后调用 `SaleOutSheetConfirmCalculator.calculateSheet(sheet, details)`；由于现有创建流程逐条保存明细，先收集本次新建明细再保存或在保存完成后批量读取一次明细并汇总，保证单据头不使用客户端传入值。修改流程复用该内部创建方法，因此同样重算。

验收数量使用界面交易单位，与 `productVo.orderNum`/`detail.businessNum` 相同；不要乘 `conversionRate`，也不要使用主单位 `detail.orderNum` 参与验收金额公式。

- [ ] **Step 5: Recalculate after batch price update**

在 `batchUpdatePrice` 更新 `taxPrice` 后调用 `SaleOutSheetConfirmCalculator.calculateDetail(detail)`；按受影响的 `sheetId` 批量读取明细，调用 `calculateSheet` 更新单据头两个字段。保持已有销售金额、付款金额校验和客户金额调整逻辑不变。

- [ ] **Step 6: Preserve import confirmNum and reject client amount values**

`checkImport` 使用 BeanUtil 复制 `confirmNum` 到 `SaleOutProductVo`。`buildImportProducts` 继续通过导入模型复制字段。普通导入和按销售日期/客户分组导入都只读取 `confirmNum`；没有 `confirmAmt` 的 import model，因此金额只能在 Service 中计算。

无论新增、修改还是导入，禁止从请求头部设置 `confirmNum`/`confirmAmt`；头部只取计算器对明细的汇总结果。

- [ ] **Step 7: Run the service and calculator tests**

```powershell
cd backend
mvn -pl xingyun-sc -am -Dtest=SaleOutSheetConfirmCalculatorTest,SaleOutSheetServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: all focused tests PASS。

- [ ] **Step 8: Commit**

```powershell
git add backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetServiceImpl.java backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/excel/sale/out/SaleOutSheetImportModel.java backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/excel/sale/out/SaleOutSheetQueryImportModel.java backend/xingyun-sc/src/test/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetServiceImplTest.java
git commit -m "feat: 重算销售出库验收数据"
```

### Task 4: 补齐销售出库 Excel 导出

**Files:**
- Modify: `SaleOutSheetExportModel.java`
- Modify: `SaleOutSheetDetailExportModel.java`
- Modify: `SaleOutSheetSalesExportHelper.java`
- Modify: `SaleOutSheetServiceImpl.java`

**Interfaces:**
- Consumes: Task 2 的查询 DTO 字段和 Task 3 重算后的 Entity 值。
- Produces: 单据导出、明细/按天汇总导出、销售单格式导出均包含“验收数量”“验收金额”。

- [ ] **Step 1: Add export model properties**

在 `SaleOutSheetExportModel` 增加 `@ExcelProperty("验收数量") private BigDecimal confirmNum` 和 `@ExcelProperty("验收金额") private BigDecimal confirmAmt`，在 `afterInit` 从 `SaleOutSheet` 读取并以 0 兜底。

在 `SaleOutSheetDetailExportModel` 增加相同两个列，从 `QuerySaleOutSheetDetailDto` 读取；按天汇总时在 `buildDailySummaryExportModels` 合并同商品的验收数量和验收金额，金额使用明细已计算值，不再次按汇总后的数量乘平均价。

- [ ] **Step 2: Add acceptance columns to custom sales export**

在 `SaleOutSheetSalesExportHelper` 将表头、列数、列宽、明细行和合计行扩展为验收数量/验收金额；给 `DetailData`、`SheetData` 增加对应属性，并在 `buildSalesExportSheetData`/`buildSalesExportDetailData` 传入明细和头部的后端值。确保合计行仍使用明细汇总值。

- [ ] **Step 3: Run compile and inspect export model headers**

```powershell
cd backend
mvn -pl xingyun-sc -am -DskipTests compile
```

Expected: compile succeeds；通过 EasyExcel 模型反射的导出列顺序为原有列后追加“验收数量”“验收金额”，自定义销售单列数与合并区域一致。

- [ ] **Step 4: Commit**

```powershell
git add backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/excel/sale/out/SaleOutSheetExportModel.java backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/excel/sale/out/SaleOutSheetDetailExportModel.java backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/excel/sale/out/SaleOutSheetSalesExportHelper.java backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetServiceImpl.java
git commit -m "feat: 销售出库导出增加验收字段"
```

### Task 5: 增加前端验收计算纯函数和测试

**Files:**
- Create: `frontend/src/views/sc/sale/out/components/saleOutConfirm.ts`
- Create: `frontend/src/views/sc/sale/out/components/__tests__/saleOutConfirm.test.ts`

**Interfaces:**
- Produces `getConfirmAmount(row): number`、`syncConfirmAmount(row): number` 和 `sumConfirmFields(rows): { confirmNum: number; confirmAmt: number }`。
- 所有函数对空值按 0 处理，金额使用现有 `mul`/`getNumber` 工具保留 6 位；不修改 `taxAmount`。

- [ ] **Step 1: Write failing front-end tests**

```ts
import { describe, expect, it } from 'vitest';
import { getConfirmAmount, sumConfirmFields } from '../saleOutConfirm';

describe('saleOutConfirm', () => {
  it('calculates amount from confirm quantity and tax price', () => {
    expect(getConfirmAmount({ confirmNum: 1.234567, taxPrice: 2.345678 })).toBe(2.895897);
  });

  it('sums detail acceptance fields and treats empty values as zero', () => {
    expect(sumConfirmFields([
      { confirmNum: 2, confirmAmt: 6.5 },
      { confirmNum: null, confirmAmt: null },
    ])).toEqual({ confirmNum: 2, confirmAmt: 6.5 });
  });
});
```

- [ ] **Step 2: Run the focused front-end test and verify it fails**

```powershell
cd frontend
pnpm vitest run src/views/sc/sale/out/components/__tests__/saleOutConfirm.test.ts
```

Expected: FAIL because the helper module does not exist.

- [ ] **Step 3: Implement the helper**

使用现有 `@/utils` 数字工具，新增函数均添加中文注释：

```ts
/** 计算明细验收金额，不改动销售金额字段。 */
export function getConfirmAmount(row: { confirmNum?: number; taxPrice?: number }): number {
  return getNumber(mul(row.confirmNum || 0, row.taxPrice || 0), 6);
}

/** 计算并回写明细验收金额，供编辑页面即时展示。 */
export function syncConfirmAmount(row: { confirmNum?: number; taxPrice?: number; confirmAmt?: number }): number {
  row.confirmAmt = getConfirmAmount(row);
  return row.confirmAmt;
}

/** 汇总明细验收数量和验收金额。 */
export function sumConfirmFields(rows: Array<{ confirmNum?: number; confirmAmt?: number }>) {
  return rows.reduce((total, row) => ({
    confirmNum: add(total.confirmNum, Number(row.confirmNum || 0)),
    confirmAmt: add(total.confirmAmt, Number(row.confirmAmt || 0)),
  }), { confirmNum: 0, confirmAmt: 0 });
}
```

- [ ] **Step 4: Run the focused test and verify it passes**

运行同一 `pnpm vitest run` 命令，预期测试 PASS。

- [ ] **Step 5: Commit**

```powershell
git add frontend/src/views/sc/sale/out/components/saleOutConfirm.ts frontend/src/views/sc/sale/out/components/__tests__/saleOutConfirm.test.ts
git commit -m "feat: 增加销售出库验收前端计算"
```

### Task 6: 补齐前端类型、编辑/审核/详情/列表和导入说明

**Files:**
- Modify: `frontend/src/api/sc/sale/out/model/saleOutProductVo.ts`
- Modify: `frontend/src/api/sc/sale/out/model/getSaleOutSheetBo.ts`
- Modify: `frontend/src/api/sc/sale/out/model/querySaleOutSheetBo.ts`
- Modify: `frontend/src/api/sc/sale/out/model/querySaleOutSheetDetailBo.ts`
- Modify: `frontend/src/views/sc/sale/out/add-require.vue`
- Modify: `frontend/src/views/sc/sale/out/add-un-require.vue`
- Modify: `frontend/src/views/sc/sale/out/modify-require.vue`
- Modify: `frontend/src/views/sc/sale/out/modify-un-require.vue`
- Modify: `frontend/src/views/sc/sale/out/approve.vue`
- Modify: `frontend/src/views/sc/sale/out/detail.vue`
- Modify: `frontend/src/views/sc/sale/out/components/detail-list.vue`
- Modify: `frontend/src/views/sc/sale/out/components/sheet-list.vue`
- Modify: `frontend/src/components/Importor/SaleOutSheetImporter.vue`
- Modify: `frontend/src/components/Importor/SaleOutSheetQueryImporter.vue`

**Interfaces:**
- Consumes: Task 5 的 `getConfirmAmount`、`syncConfirmAmount`、`sumConfirmFields`。
- Produces: UI 可编辑 `confirmNum`、只读 `confirmAmt`，所有查询/详情/列表类型可承载后端字段。

- [ ] **Step 1: Extend TypeScript models**

在 `SaleOutProductVo` 添加 `confirmNum: number`；在 `GetSaleOutSheetBo`、`OrderDetailBo`、`QuerySaleOutSheetBo`、`QuerySaleOutSheetDetailBo` 添加 `confirmNum` 和 `confirmAmt`，注释分别标明“验收数量”“验收金额”。

- [ ] **Step 2: Add read-only acceptance columns and header totals**

四个新增/修改页都执行同一结构调整：

```vue
<template #confirmAmt_default="{ row }">
  <span>{{ getNumber(row.confirmAmt || getConfirmAmount(row), 6) }}</span>
</template>
```

验收数量列使用 `a-input`，输入事件仅规范化 `confirmNum` 后调用 `syncConfirmAmount(row)` 和 `calcSum()`；验收金额列不使用 `v-model` 或输入事件。`formData` 初始化和详情回显增加 `confirmNum: 0`、`confirmAmt: 0`，`calcSum()` 使用 `sumConfirmFields(this.tableData)` 写入头部只读字段。

保留现有销售金额 `taxAmount` 的人工金额逻辑，不让验收金额复用 `manualTaxAmount`。

- [ ] **Step 3: Add acceptance data to detail, approve, and list views**

在 `approve.vue`、`components/detail-list.vue` 的表格列中增加验收数量/验收金额，底部合计增加验收汇总；在 `components/sheet-list.vue` 增加单据头验收列。`detail.vue` 若通过明细列表组件渲染，只补齐传入/回显数据，不重复实现计算。

- [ ] **Step 4: Update import component descriptions**

在两个导入组件的提示中明确：Excel 可填写“验收数量”，“验收金额”由系统按验收数量和单价自动计算，不允许手工覆盖；保留现有日期、分组和字段说明。

- [ ] **Step 5: Run front-end unit tests and lint**

```powershell
cd frontend
pnpm vitest run src/views/sc/sale/out/components/__tests__/saleOutConfirm.test.ts src/views/sc/sale/out/components/__tests__/saleOutProductParams.test.ts
pnpm run lint
```

Expected: focused tests PASS and lint exits with code 0。

- [ ] **Step 6: Commit**

```powershell
git add frontend/src/api/sc/sale/out frontend/src/views/sc/sale/out frontend/src/components/Importor/SaleOutSheetImporter.vue frontend/src/components/Importor/SaleOutSheetQueryImporter.vue
git commit -m "feat: 销售出库页面支持验收字段"
```

### Task 7: 全链路验证并复核需求覆盖

**Files:**
- Test: all files changed in Tasks 1–6

- [ ] **Step 1: Run the complete backend test suite**

```powershell
cd backend
mvn test
```

Expected: exit code 0 with no test failures.

- [ ] **Step 2: Run the complete backend compile**

```powershell
cd backend
mvn clean compile -DskipTests
```

Expected: exit code 0; all modules compile against the new DTO/entity fields.

- [ ] **Step 3: Run the complete front-end lint**

```powershell
cd frontend
pnpm run lint
```

Expected: exit code 0 with no lint errors.

- [ ] **Step 4: Perform a requirement checklist against the diff**

Verify each item before claiming completion:

```text
[ ] 两张数据库表均有 confirm_num/confirm_amt，历史值为 0
[ ] 明细 confirmAmt 只能由 confirmNum × taxPrice 得到
[ ] 单据头两个验收字段均由明细汇总
[ ] 新增、修改、调价、普通导入、分组导入都重算
[ ] 后端 DTO/VO/查询映射均返回字段
[ ] 单据、明细、按天汇总、自定义销售单导出均覆盖
[ ] 四个编辑页金额只读，详情/审核/列表可查看
[ ] 后端和前端测试覆盖空值、精度、汇总和篡改防护
```

- [ ] **Step 5: Inspect final diff**

```powershell
git diff --check
git status --short
git diff --stat
```

Expected: no whitespace errors; only the intended database/backend/frontend files are modified; no unrelated files are changed.

