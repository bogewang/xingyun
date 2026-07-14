# Task 3：Excel 导入非负校验报告

## 变更

- 移除收货单和销售出库单 Excel 导入模型中“数量”的 `@ExcelRequired`。
- 将数量规则改为：允许空值和 0；负数报 `第{seq}行“数量”不允许小于0`；保留 8 位小数限制。
- 为采购单价、销售单价增加：允许空值和 0；负数报 `第{seq}行“单价”不允许小于0`；保留 6 位小数限制。
- 将数值规则提取为同包可见纯方法，两个既有 `checkImport()` 路径继续通过 `checkImportData()` 复用该校验；空单价默认填充逻辑未改动。

## TDD 证据

### RED

新增 `ReceiveSheetServiceImplTest` 与 `SaleOutSheetServiceImplTest`，覆盖空值、0、负数和精度限制。先运行：

```powershell
mvn -pl xingyun-sc '-Dtest=ReceiveSheetServiceImplTest,SaleOutSheetServiceImplTest' test
```

结果：失败。`testCompile` 报两个 Service 均不存在 `validateImportNumbers(...)`，共 8 个“找不到符号”错误，表明测试所需的纯校验入口尚未实现。

### GREEN

最小实现两个 Service 的包可见 `validateImportNumbers(...)` 并接入既有导入校验后，以相同命令验证：

```text
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 提交

`fix: reject negative values in sheet imports`

## Concerns

- Maven 编译输出包含既有 `QuerySaleOrderBo.java` 的 unchecked 操作提示；本任务的聚焦测试无失败或错误。
- `.superpowers/` 下的其他任务文件为既有未跟踪协作资料，提交时仅纳入本任务报告。
