# 来源订单询价商品字段修复报告

## 状态

已完成采购收货 `getWithReceive` 与销售出库 `getWithOut` 来源订单固定明细的询价商品字段贯通。

- `PurchaseOrderWithReceiveBo.DetailBo` 新增可空 `Boolean inquiryProduct`，值来自 `PurchaseProductDto`。
- `SaleOrderWithOutBo.DetailBo` 新增可空 `Boolean inquiryProduct`，值来自已有的 `SaleProductDto.getInquiryProduct()`。
- 采购商品查询链路补充 `PurchaseProductDto.inquiryProduct`、MyBatis resultMap 和 `g.inquiry_product` SELECT 列。
- 前端 `purchaseOrderWithReceiveBo.ts` 与 `saleOrderWithOutBo.ts` 的 `DetailBo` 均声明为 `boolean | null`。
- 未修改采购收货、销售出库的保存入参，也未修改固定行合并逻辑。

## 根因

来源订单明细不是从收货单或出库单的只读明细查询链路加载，而是在 BO 初始化时再次查询商品并组装固定行。

- 销售商品 DTO 已经包含 `inquiryProduct`，但 `SaleOrderWithOutBo.DetailBo` 组装时未赋值。
- 采购商品 DTO、采购商品 MyBatis 映射及 SELECT 均缺少 `inquiryProduct`，`PurchaseOrderWithReceiveBo.DetailBo` 也未暴露和赋值该字段。
- 两个前端来源订单明细类型没有声明该响应字段，因此固定行展示虽然已调用 `formatInquiryProduct`，却收不到类型完整的数据。

## TDD 证据

### RED

先新增来源订单契约测试，再修改生产代码。

- 后端 `SourceOrderInquiryProductContractTest` 首次运行：3 项中 1 个失败、2 个错误。
  - 采购 MyBatis resultMap 缺少 `inquiryProduct`。
  - `PurchaseProductDto` 缺少 `setInquiryProduct(Boolean)`。
  - `SaleOrderWithOutBo.DetailBo` 缺少 `getInquiryProduct()`。
- 前端 `inquiryProductContract.test.ts` 首次运行：5 项中 1 项失败，两个来源订单 `DetailBo` 均缺少 `inquiryProduct: boolean | null`。

### GREEN

- 后端关联测试：
  - 命令：`mvn -pl xingyun-sc -am '-Dtest=SourceOrderInquiryProductContractTest,SaleProductInquiryProductContractTest,ReceiveSheetInquiryProductMappingTest,SaleOutSheetInquiryProductMappingTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`
  - 结果：15 项通过，0 失败，0 错误。
- 前端关联测试：
  - 命令：`pnpm test:unit src/views/sc/components/__tests__/inquiryProductContract.test.ts src/views/sc/components/__tests__/inquiryProduct.test.ts --run`
  - 结果：7 项通过，0 失败。
  - 其中 `formatInquiryProduct(true)` 明确验证为绿色“是”。
- 前端变更文件 ESLint：
  - 命令：`pnpm exec eslint --max-warnings 0 ...`
  - 结果：通过。
- `git diff --check`：通过。

## 全量检查与疑虑

- `mvn -pl xingyun-sc -am test` 中本次新增及关联询价测试全部通过，但仓库已有销售出库测试仍有 4 项失败：
  - `SaleOutSheetConfirmCalculatorTest.calculateSheetShouldSumDetailConfirmValuesAndIgnoreExistingHeaderValues`
  - `SaleOutSheetMarketBuySummaryFormatterTest.buildMarketBuySummaryHeadersShouldUseOneDetailColumn`
  - `SaleOutSheetServiceImplTest.resolveCostNumShouldPreferPositiveConfirmNum`
  - `SaleOutSheetServiceImplTest.resolveCostNumShouldUseZeroConfirmNum`
  - 本次差异未触及上述实现或测试。
- `pnpm test:unit --run` 结果为 70 项通过、1 项失败；失败项是未改动的 `saleOutProfit.test.ts`，期望 `31.88%`、实际 `32.89%`。
- `pnpm type:check` 未进入项目类型分析：`vue-tsc 1.8.27` 在当前 TypeScript 5.5.4 / Node 20.20.0 环境启动时抛出 `Search string not found: "for (const existingRoot of buildInfoVersionMap.roots) {"`。
- 首次 `mvn clean` 在 JDK 1.8.0_101 下出现一次上游 `ProductUnit.class` 瞬时不可访问和 javac 内部断言异常；确认 class 产物存在后原命令重跑成功，新增契约 3 项通过。
