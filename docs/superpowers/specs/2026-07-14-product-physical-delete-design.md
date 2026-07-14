# 商品物理删除设计

## 目标

将 `ProductService#deleteById` 从把商品状态标记为不可用改为直接删除商品记录。

## 方案

1. 删除前查询商品记录，作为删除事件的实体载荷。
2. 通过 `ProductMapper#deleteById` 物理删除 `base_data_product` 中对应记录。
3. 发布物理删除语义的数据变更事件，并清除商品缓存，避免读取到已删除商品。
4. 不变更商品单位、商品属性关系及其他关联表；该范围未包含在本次需求内，避免影响历史业务数据。

## 错误与事务

删除操作仍由 Service 层的 `@Transactional(rollbackFor = Exception.class)` 包裹。删除或事件发布失败时回滚数据库事务。

## 验证

新增回归测试，验证删除路径调用 Mapper 的 `deleteById`，不再执行将 `available` 更新为 `false` 的逻辑删除操作；执行 `ProductServiceImplTest` 验证。
