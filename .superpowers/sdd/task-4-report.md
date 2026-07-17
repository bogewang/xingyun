# Task 4 Report

## Changed files

- `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/excel/sale/out/SaleOutSheetExportModel.java`
- `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/excel/sale/out/SaleOutSheetDetailExportModel.java`
- `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/excel/sale/out/SaleOutSheetSalesExportHelper.java`
- `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetServiceImpl.java`

## What changed

- 销售出库普通导出补充了“验收数量/验收金额”字段，并在初始化时默认空值为 0。
- 销售出库明细按天汇总导出补充了“验收数量/验收金额”字段，且按商品合并时对验收数量和验收金额做累加。
- 自定义销售单 Excel 布局扩展了验收列，保持原有列顺序不变，仅在末尾追加验收数量/验收金额。
- 服务层导出组装逻辑改为直接读取后端已计算好的验收值，不在导出层重新计算。

## Verification

- `mvn -pl xingyun-sc -am -DskipTests compile`：通过
- `git diff --check`：通过

## Concerns

- 无
