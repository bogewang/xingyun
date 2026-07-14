# 销售出库商品明细组装规则

## 目标

创建销售出库单时，已选择商品的表格行必须进入请求 `products`。行是否有效仅由 `productId` 是否存在决定；数量为空或为零不应使该行被过滤。

## 范围

修改以下两个新增页面的 `buildParams()`：

- `frontend/src/views/sc/sale/out/add-un-require.vue`（非订单销售出库）
- `frontend/src/views/sc/sale/out/add-require.vue`（需订单销售出库）

不修改编辑、退货、后端服务或接口字段定义。

## 数据流

1. 用户在表格中选择商品，页面行得到 `productId`。
2. 保存时，`buildParams()` 过滤出所有 `productId` 非空的行，并逐行映射为 `SaleOutProductVo` 请求对象。
3. `outNum` 原样映射为 `orderNum`：用户未填写时保持空值，用户填写 `0` 时保持零值。
4. 后端已允许 `orderNum` 为 `null` 或 `0`，并以 `products` 非空和 `productId` 非空为必需条件。

## 实现方式

移除两个页面中 `products` 构建链路的 `isFloatGtZero(t.outNum)` 二次过滤；保留前置的 `productId` 过滤。保留现有数量、价格的格式、精度和非负数校验。

## 回归验证

为两个页面的请求组装逻辑增加可执行测试或等效的最小单元测试，覆盖：

- 有 `productId` 且 `outNum` 为空：请求包含该商品；
- 有 `productId` 且 `outNum` 为 `0`：请求包含该商品；
- 无 `productId`：请求不包含该行；
- 正数数量的既有请求结构保持不变。

## 不采用的方案

不把空数量转换为 `0`，以保留“未填写”和“显式填零”的语义差异；不抽取跨页面公共函数，避免对本次行为修复引入额外重构范围。
