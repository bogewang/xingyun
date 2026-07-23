# 采购收货单付款金额合计设计

## 目标

在 `/purchase/receive` 收货单列表的合计行中展示当前页的本单已付金额和未付金额。

## 方案

仅调整 `frontend/src/views/sc/purchase/receive/components/sheet-list.vue`：

- 在既有 `footerMethod` 中通过 `sumByField` 汇总 `paidAmount` 和 `unpaidAmount`。
- 按现有金额列的规则使用 `formatAmount` 格式化两个汇总结果。
- 将格式化后的结果分别返回到 `paidAmount`（本单已付）和 `unpaidAmount`（未付金额）列的合计单元格。

## 范围与约束

- 汇总范围与现有合计保持一致：仅为当前页已加载的列表数据。
- 不调整后端接口、分页参数、列顺序或金额计算口径。
- 保持 Vue 3、TypeScript、vxe-table 既有实现模式。

## 验收标准

- 列表存在数据时，合计行的“本单已付”和“未付金额”显示对应列的当前页总和。
- 两项金额与“单据总金额”采用一致的金额格式。
- 其他合计项和列表功能保持不变。
