# 采购入库打印功能设计

## 1. 背景与目标

销售出库页面已经支持列表操作菜单和详情弹窗打印。采购入库当前后端已有独立的打印数据接口和业务类型，但前端缺少完整的打印入口，同时打印模板字段说明接口只暴露销售出库字段。

本次目标是：

- 在采购入库列表操作菜单增加“打印”。
- 在采购入库详情弹窗底部增加“打印”。
- 复用采购入库专属打印接口和 `RECEIVE_SHEET` 业务类型。
- 让模板设计器按模板业务类型加载正确的字段说明，便于用户自行设计采购入库模板。

本次不修改打印模板视觉内容，模板由用户在打印模板管理中设计。

## 2. 现状与边界

### 2.1 后端打印数据不可直接复用销售出库 DTO

销售出库接口 `/sale/out/sheet/print` 返回 `PrintSaleOrderBo`，其字段面向客户、销售员和出库业务；采购入库接口 `/purchase/receive/sheet/print` 返回独立的 `PrintReceiveSheetBo`，其字段面向供应商、采购员、采购订单、到货日期和收货业务。

采购入库明细字段包括 `receiveNum`、`purchasePrice`、`receiveAmount` 等，与销售出库明细不相同。因此不能把采购入库响应转换成 `PrintSaleOrderBo`，也不修改销售出库接口的响应结构。

### 2.2 可以复用的部分

- 前端 `api.print(id)` 请求模式。
- `printMix.vgPrintPreview` 打印预览流程。
- `PRINT_TYPE.RECEIVE_SHEET` 业务类型值 `2`。
- 打印模板列表查询、模板切换和预览弹窗。
- 采购入库现有 `PrintReceiveSheetBo` 后端组装逻辑。

### 2.3 当前字段说明缺口

`GET /basedata/print/template/fieldDesc` 当前硬编码反射 `PrintSaleOrderBo`，无论模板的 `bizType` 是什么都返回销售出库字段。采购入库模板因此无法在字段说明中看到 `supplierCode`、`purchaserName`、`purchaseOrderCode`、`receiveDate`、`details[].receiveNum` 等字段。

## 3. 方案

### 3.1 字段说明按业务类型返回

将字段说明接口扩展为接收 `bizType` 查询参数：

- `bizType=2`：反射 `PrintReceiveSheetBo`，返回采购入库主表和 `OrderDetailBo` 明细字段。
- `bizType=7`：反射 `PrintSaleOrderBo`，保持销售出库字段说明行为。
- 未传 `bizType`：保持现有销售出库字段行为，兼容旧调用方。
- 其他业务类型：返回明确的业务类型不支持错误，不静默返回错误字段。

字段说明仍由后端 DTO 的字段名和 `@ApiModelProperty` 生成，示例值由业务类型对应的示例数据映射提供，未配置的字段使用类型默认示例值。

### 3.2 模板设计器传递当前业务类型

模板设置接口响应补充当前模板的 `bizType`，前端设置页加载模板后将其传给 `PrintDesigner`。设计器打开“模板字段说明”时调用 `getFieldDesc({ bizType })`，从而展示当前模板对应的字段。

这样不会把销售和采购字段混在同一份字段列表中，也不会要求用户手动区分重复字段名。

### 3.3 采购入库前端打印入口

在 `frontend/src/views/sc/purchase/receive/components/sheet-list.vue` 中：

- 引入 `printMix`、`PRINT_TYPE`。
- 增加列表行打印方法，调用 `api.print(row.id)` 后执行 `vgPrintPreview(PRINT_TYPE.RECEIVE_SHEET.code, res)`。
- 在 `createActions(row)` 的“导出明细”后增加“打印”。

在 `frontend/src/views/sc/purchase/receive/detail.vue` 中：

- 保留现有的采购入库 `print()` 方法和打印预览组件。
- 在详情弹窗底部“导出明细”与“关闭”之间增加“打印”按钮。
- 使用已有 `api.print(this.id)` 和 `PRINT_TYPE.RECEIVE_SHEET`，不复制销售出库字段转换逻辑。

两个入口均使用 `purchase:receive:query` 权限，与后端打印接口一致。

## 4. 数据流

```text
采购入库列表/详情
        |
        | GET /purchase/receive/sheet/print?id=...
        v
PrintReceiveSheetBo
        |
        | bizType = 2
        v
打印模板查询 -> 读取采购入库模板配置 -> 打印预览
```

模板设计字段流：

```text
模板设置加载 -> 获取模板 bizType -> PrintDesigner
        |
        | GET /basedata/print/template/fieldDesc?bizType=2
        v
采购入库字段说明与示例值
```

## 5. 错误处理

- 采购入库打印接口继续返回统一 `InvokeResult<PrintReceiveSheetBo>`。
- 查询不到采购入库单时，后端返回业务异常 `DefaultClientException("采购收货单不存在！")`，具体文案按现有服务层行为统一处理。
- 当前业务类型没有打印模板时，前端继续通过 `printMix` 提示“未找到当前业务类型的打印模板”。
- 字段说明业务类型不支持时，后端抛出 `DefaultClientException`，控制器按现有统一响应包装返回。
- 不在数据库事务中增加打印预览或模板读取以外的外部调用；本次不引入新的外部调用。

## 6. 测试与验证

### 后端

- 为字段说明业务类型选择逻辑增加单元测试：业务类型 2 返回采购入库字段，业务类型 7 返回销售出库字段，未传参数保持兼容。
- 验证采购入库打印 DTO 的主表和明细字段能够对应 `PrintReceiveSheetBo`，并执行 `cd backend && mvn test` 或至少执行受影响模块测试。

### 前端

- 增加可测试的打印业务类型/字段说明请求行为验证，确保采购入库入口使用 `RECEIVE_SHEET`，而不是 `SALE_OUT`。
- 执行 `cd frontend && pnpm run lint`。
- 通过代码检查确认列表和详情入口均调用采购入库 `api.print`，且不修改销售出库打印逻辑。

## 7. 非目标

- 不修改 `backend/xingyun-sc/src/main/resources/print/receive-sheet.ftl` 的模板样式。
- 不合并销售出库和采购入库的打印 DTO。
- 不重构现有打印预览组件或模板设计器整体架构。
- 不增加批量采购入库打印；本次只参照销售出库已有的单据打印入口。
