# 采购收货单付款金额合计设计

## 目标

在 `/purchase/receive` 收货单列表的合计行中展示当前页的本单已付金额和未付金额。

## 方案

新增可单测的合计行纯函数，并调整 `frontend/src/views/sc/purchase/receive/components/sheet-list.vue` 调用它：

- 纯函数负责汇总 `totalNum`、`totalAmount`、`paidAmount` 和 `unpaidAmount`，并生成列对应的合计单元格文本。
- 通过 Vitest 覆盖已付和未付金额的汇总与格式化结果。
- 列表组件继续把 vxe-table 的 `columns`、`data` 传入该函数，不改变表格配置。

## 范围与约束

- 汇总范围与现有合计保持一致：仅为当前页已加载的列表数据。
- 不调整后端接口、分页参数、列顺序或金额计算口径。
- 保持 Vue 3、TypeScript、vxe-table 既有实现模式。

## 验收标准

- 列表存在数据时，合计行的“本单已付”和“未付金额”显示对应列的当前页总和。
- 两项金额与“单据总金额”采用一致的金额格式。
- 其他合计项和列表功能保持不变。
