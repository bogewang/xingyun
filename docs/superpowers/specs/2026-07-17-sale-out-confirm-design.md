# 销售出库验收数量与验收金额设计

## 目标

为销售出库单（`tbl_sale_out_sheet`）及销售出库明细（`tbl_sale_out_sheet_detail`）增加验收数量 `confirmNum` 和验收金额 `confirmAmt`，实现明细计算、单据汇总、前后端展示以及导入导出全链路支持。

## 业务规则

1. 明细验收数量 `confirmNum` 允许在销售出库单新增、编辑和导入时录入或修改。
2. 明细验收金额 `confirmAmt` 不允许人工修改，计算公式为：

   `confirmAmt = confirmNum × taxPrice`

   其中 `taxPrice` 是现有销售出库明细的销售单价。
3. 单据头验收数量和验收金额只由明细汇总：

   - `sheet.confirmNum = SUM(detail.confirmNum)`
   - `sheet.confirmAmt = SUM(detail.confirmAmt)`

4. 后端是计算规则的最终可信来源。客户端提交的验收金额和单据头汇总值不直接入库，保存、修改、导入时均重新计算。
5. 验收数量沿用现有数量字段的非负和小数精度校验，最多保留 6 位小数；本次不增加“验收数量不得超过出库数量”的额外限制。
6. 历史数据的两个验收字段初始化为 0；查询时对空值按 0 处理。
7. 金额字段按现有数据库金额精度 `decimal(24,6)` 保存，计算结果保留 6 位小数。

## 数据库设计

租户数据库迁移新增字段：

```sql
ALTER TABLE `tbl_sale_out_sheet`
  ADD COLUMN `confirm_num` decimal(24,6) NOT NULL DEFAULT '0.000000' COMMENT '验收数量',
  ADD COLUMN `confirm_amt` decimal(24,6) NOT NULL DEFAULT '0.000000' COMMENT '验收金额';

ALTER TABLE `tbl_sale_out_sheet_detail`
  ADD COLUMN `confirm_num` decimal(24,6) NOT NULL DEFAULT '0.000000' COMMENT '验收数量',
  ADD COLUMN `confirm_amt` decimal(24,6) NOT NULL DEFAULT '0.000000' COMMENT '验收金额';
```

字段默认值保证历史记录和新记录均可安全读取；历史记录的验收数量、验收金额均保持 0，不从原出库数量回填。

## 后端设计

### 数据对象

- `SaleOutSheet` 增加 `confirmNum`、`confirmAmt`。
- `SaleOutSheetDetail` 增加 `confirmNum`、`confirmAmt`。
- 销售出库新增/修改请求中的明细对象增加 `confirmNum`；响应 DTO/VO、查询 DTO、编辑回显对象增加两个字段。
- MapStruct 或现有对象转换链路同步补齐新字段，避免手写重复转换。

### 计算与保存

在销售出库 Service 内集中实现验收计算：

1. 遍历内存中的明细，空验收数量按 0 处理。
2. 使用明细销售单价 `taxPrice` 计算 `confirmAmt`，空单价按 0 处理，结果按 6 位小数保存。
3. 对明细验收数量和验收金额分别求和。
4. 将两个汇总值写入单据头，再通过现有批量保存/更新流程持久化。

该计算在新增、修改、导入和任何会重建销售出库明细的 Service 入口执行。Controller 只负责参数校验和统一响应包装，DAO/Mapper 不承载业务计算。

### 查询与导出数据

查询 SQL、结果映射及 DTO 显式返回两个字段。销售出库单据导出增加“验收数量”“验收金额”，销售出库明细及相关汇总导出也增加相同列，导出使用后端重算后的值。

## 前端设计

### 销售出库编辑页

覆盖按订单和非按订单的新增、修改页面：

- 明细表增加“验收数量”输入列。
- 明细表增加“验收金额”只读列，不绑定金额输入处理逻辑。
- 验收数量或销售单价变化时即时计算明细验收金额。
- 单据头增加只读的验收数量和验收金额，并由明细行汇总。
- 提交时可以携带验收数量，但不依赖前端传入的金额和头部汇总。

### 查询和详情页

销售出库详情、明细列表以及相关引用场景增加验收数量和验收金额展示；列表底部汇总使用明细字段求和。

### 导入

两种销售出库导入模板增加“验收数量”列和前端模板说明。导入不接收“验收金额”作为可编辑输入，金额由后端根据验收数量和销售单价重算。

## 错误处理

- 验收数量格式错误、负数或超出 6 位小数时，沿用现有业务异常和统一 `InvokeResult<T>` 响应规范。
- 业务异常使用 `DefaultClientException`，禁止抛出裸 `RuntimeException`。
- 计算过程不调用外部服务，不新增事务边界；保持现有 Service 事务配置。

## 测试与验收标准

后端测试至少覆盖：

1. 空验收数量和 0 验收数量计算为 0。
2. 验收数量乘销售单价得到 6 位小数金额。
3. 多条明细正确汇总到单据头。
4. 客户端篡改验收金额或头部汇总时，后端仍按明细重算。
5. 两种导入均能读取验收数量，验收金额不作为可信输入。

前端测试至少覆盖：

1. 验收金额列只读。
2. 验收数量/单价变化后的金额计算。
3. 单据头验收数量、验收金额汇总。
4. 导入字段映射。

验收命令：

```bash
cd backend && mvn test
cd backend && mvn clean compile -DskipTests
cd frontend && pnpm run lint
```

## 范围边界

本次只增加销售出库验收字段及其相关链路，不改变销售出库原有数量、销售金额、结算金额、库存扣减和退货数量的业务含义。
