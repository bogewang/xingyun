# 商品信息行内停用按钮设计

## 目标

在 `/product/info` 商品信息列表的操作列，为已启用商品提供行内“停用”操作，缩短单个商品停用路径。

## 交互与权限

- 仅当商品 `available` 为启用状态时显示“停用”按钮；已停用商品不显示该操作。
- 按钮沿用商品修改权限 `base-data:product:info:modify`。
- 点击后展示确认提示，明确商品名称；用户确认后才发起状态更新。

## 数据流与错误处理

- 复用现有 `updateAvailable` API，传入 `{ ids: [row.id], available: false }`。
- 更新成功后提示“停用成功！”并重新加载当前列表。
- 请求失败沿用 HTTP 层已有的错误提示机制，不刷新列表数据。

## 变更范围与测试

- 仅修改 `frontend/src/views/base-data/product/info/index.vue` 的操作项创建逻辑。
- 扩展现有商品状态请求测试，覆盖单个商品停用请求参数。
- 执行对应 Vitest 测试与前端 lint（若项目现状允许）。
