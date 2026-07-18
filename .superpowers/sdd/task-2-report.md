# Task 2 报告

## 变更文件

- `backend/xingyun-api/src/main/resources/db/migration/tenant/V2.5-sale-out-confirm.sql`
- `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/entity/SaleOutSheet.java`
- `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/entity/SaleOutSheetDetail.java`
- `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/vo/sale/out/SaleOutProductVo.java`
- `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/dto/sale/out/SaleOutSheetFullDto.java`
- `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/dto/sale/out/QuerySaleOutSheetDetailDto.java`
- `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/dto/sale/out/SaleOutSheetWithReturnDto.java`
- `backend/xingyun-sc/src/main/resources/mappers/sale/SaleOutSheetMapper.xml`
- `backend/xingyun-sc/src/main/resources/mappers/sale/SaleOutSheetDetailMapper.xml`

## 变更说明

- 新增销售出库验收字段租户迁移脚本，按要求为单据头和单据明细增加 `confirm_num`、`confirm_amt`。
- 复用 Task 1 已存在的实体字段与 `SaleOutSheetConfirmCalculator`，仅补充实体中文注释说明。
- 在 `SaleOutProductVo` 中仅增加 `confirmNum` 请求字段，未新增 `confirmAmt` 可信入参。
- 为 `SaleOutSheetFullDto`、`QuerySaleOutSheetDetailDto`、`SaleOutSheetWithReturnDto` 补齐验收数量/金额字段。
- 扩展 `SaleOutSheetMapper.xml`、`SaleOutSheetDetailMapper.xml` 的 resultMap 与查询 SQL，补齐头/明细字段映射，并为嵌套明细使用 `detail_confirm_num`、`detail_confirm_amt` 别名。

## 验证命令

```powershell
cd backend
mvn -pl xingyun-sc -am '-DskipTests' compile
```

## 验证结果摘要

- 命令执行目录：`D:\dev\CODE\xingyun\.worktrees\sale-out-confirm\backend`
- 结果：`BUILD SUCCESS`
- 影响模块：`xingyun`、`xingyun-core`、`xingyun-basedata`、`xingyun-sc`

## 原始输出摘录

```text
[INFO] Reactor Summary for 【xingyun】星云ERP 1.0.0-SNAPSHOT:
[INFO] 【xingyun】星云ERP ..................................... SUCCESS
[INFO] 【xingyun-core】基础依赖 ................................. SUCCESS
[INFO] 【xingyun-basedata】基础数据服务层 .......................... SUCCESS
[INFO] 【xingyun-sc】仓库业务服务层 ................................ SUCCESS
[INFO] BUILD SUCCESS
```

## Concerns

- 编译过程中出现 `TakeStockSheetExportModel.java` 的既有 unchecked 提示，但本次 `compile` 成功，且与本任务改动无直接关联。

## Commit

- `b5bb4507`
