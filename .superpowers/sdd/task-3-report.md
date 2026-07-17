# Task 3 报告：重算销售出库验收数据

## RED

- 命令：
  `mvn -pl xingyun-sc -am '-Dtest=SaleOutSheetServiceImplTest,SaleOutSheetConfirmCalculatorTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`
- 结果：
  失败。`SaleOutSheetServiceImplTest` 编译报错，`SaleOutSheetImportModel` / `SaleOutSheetQueryImportModel` 缺少 `confirmNum` 的 getter/setter，证明新增测试先于实现生效。

## GREEN

- 命令：
  `mvn -pl xingyun-sc -am '-Dtest=SaleOutSheetServiceImplTest,SaleOutSheetConfirmCalculatorTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`
- 结果：
  成功。`TestSuite` 共运行 10 个测试，失败 0、错误 0、跳过 0；Maven Reactor `BUILD SUCCESS`。

## 变更文件

- `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetServiceImpl.java`
- `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/excel/sale/out/SaleOutSheetImportModel.java`
- `backend/xingyun-sc/src/test/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetServiceImplTest.java`
- `.superpowers/sdd/task-3-report.md`

## 实现说明

- 导入模型新增 `confirmNum`，查询导入模型直接继承该字段，未新增 `confirmAmt`。
- 导入数量校验新增 `confirmNum` 的非负与 6 位小数限制；查询导入的空 `confirmNum` 归零。
- 销售出库单创建时将交易单位的 `confirmNum` 写入明细，逐条调用 `SaleOutSheetConfirmCalculator.calculateDetail`，并在明细构建完成后调用 `calculateSheet` 覆盖单头验收汇总。
- 调价时明细改价后重算 `confirmAmt`，再按单据重新汇总并回写 `confirmNum`/`confirmAmt`。
- 普通导入与分组导入通过现有 `BeanUtil.copyProperties` 自动透传 `confirmNum`，`confirmAmt` 始终由 Service 重算。

## 提交

- 待提交：`feat: 重算销售出库验收数据`

## Concerns

- `SaleOutSheetQueryImportModel.java` 无需单独改动：其通过继承 `SaleOutSheetImportModel` 已获得 `confirmNum`，并且没有新增 `confirmAmt` 字段。
- worktree 中存在与本任务无关的未提交文件：`.superpowers/sdd/task-2-report.md`，本次不会纳入提交。
