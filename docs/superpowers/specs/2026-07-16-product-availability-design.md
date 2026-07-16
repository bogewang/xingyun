# 商品批量启停用设计

## 目标

为商品资料增加批量启用、批量禁用能力，并支持启用/禁用/全部三态查询。禁用商品仍可在历史单据中按商品 ID 正确回显，但不能被用于新建业务单据。

## 已确认的业务规则

1. 批量操作仅作用于商品资料列表中勾选的记录。
2. “全部”筛选下允许混合选择启用和禁用商品，然后统一设置为目标状态。
3. 禁用商品的数据库字段为 `available=0`，启用为 `available=1`。
4. 商品主列表默认查询启用商品，状态筛选提供“启用、禁用、全部”。
5. 商品选择器的分页查询只返回启用商品，避免新建单据选择到禁用商品。
6. 历史单据通过商品 ID 加载商品时不受 `available` 限制，保证明细名称、编号、规格、单位等信息回显。
7. 所有直接接收商品 ID 的新建业务单据入口，后端必须校验商品为启用状态；不能只依赖前端选择器隐藏。
8. 历史单据读取不执行启用状态校验，避免禁用商品无法回显。
9. 商品状态变更使用现有商品修改权限，不新增独立权限码。

## 方案选择

采用一个批量状态接口：

```text
PUT /basedata/product/available
{
  "ids": ["商品ID1", "商品ID2"],
  "available": false
}
```

该接口由服务层使用 MyBatis-Plus `LambdaUpdateWrapper` 一次性更新，避免前端逐条请求导致请求量大或部分成功。使用独立请求对象，避免复用完整商品修改对象覆盖其他商品资料。

## 后端设计

### 商品查询

修改以下文件：

- `backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/vo/product/info/QueryProductVo.java`
- `backend/xingyun-basedata/src/main/resources/mappers/product/ProductMapper.xml`
- `backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/bo/product/info/QueryProductBo.java`

`QueryProductVo` 增加 `Boolean available`。查询 SQL 将当前固定的 `AND g.available = TRUE` 改为：仅当 `vo.available != null` 时追加 `g.available = #{vo.available}`。导出复用同一查询条件，因此导出结果也与状态筛选一致。

`QueryProductBo` 显式增加 `available`，供商品列表状态列使用。`findById` 保持无状态过滤，继续服务于详情和历史单据回显。

### 批量状态接口

新增文件：

- `backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/vo/product/info/UpdateProductAvailableVo.java`

修改文件：

- `backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/controller/ProductController.java`
- `backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/service/product/ProductService.java`
- `backend/xingyun-basedata/src/main/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImpl.java`
- `frontend/src/api/base-data/product/info/index.ts` 及对应请求模型

请求对象约束：

- `ids` 非空；服务层去重后批量更新。
- `available` 非空，只允许 `true` 或 `false`。
- 请求使用商品资料修改权限。
- 服务方法使用 `@Transactional(rollbackFor = Exception.class)`，只更新 `available` 字段。
- 更新完成后清理所有受影响商品的 `Product` 缓存。

控制器只负责参数校验、调用服务和 `InvokeResult` 响应包装；业务异常使用 `DefaultClientException`，不抛出通用运行时异常。

### 新建业务单据校验

在 `ProductService` 增加批量启用校验方法，例如：

```java
/**
 * 校验商品均为启用状态。
 *
 * @param productIds 商品 ID 集合
 */
void assertAvailable(Collection<String> productIds);
```

实现使用一次 `in(id, productIds)` 查询所有商品，再判断是否存在禁用商品；不循环查询数据库。发现禁用商品时抛出：

```text
商品已停用，无法新增业务单据！
```

现有“商品不存在”的行号校验继续保留。该校验接入所有直接接收商品 ID 的新建入口，包括：

- 采购订单、销售订单、零售出库。
- 采购收货、销售出库。
- 采购退货、销售退货、零售退货。
- 调拨单、库存调整单。
- 盘点任务、预先盘点单、盘点单。
- 其他对外新建接口中直接提交商品 ID 的产品明细入口。

历史单据详情、导出、库存变动等读取或既有单据处理链路继续使用不带状态限制的 `findById`，不能为了新增校验而改变历史数据读取行为。

## 前端设计

修改文件：

- `frontend/src/views/base-data/product/info/index.vue`
- `frontend/src/api/base-data/product/info/index.ts`
- `frontend/src/api/base-data/product/info/model/queryProductVo.ts`
- `frontend/src/api/base-data/product/info/model/queryProductBo.ts`
- 新增批量状态请求模型

页面行为：

1. `searchFormData.available` 默认值为 `true`。
2. 状态筛选使用 `AVAILABLE` 枚举，展示“启用、禁用”，并增加“全部”空值选项。
3. 表格增加状态列，使用 `AvailableTag` 展示状态。
4. “更多”菜单增加“批量启用”和“批量禁用”。两者分别打开批量确认窗口，确认后将全部商品 ID 和目标状态一次提交到批量接口，成功后刷新列表并清空勾选。
5. 混合状态勾选不在前端拦截，统一提交目标状态；重复点击同一目标状态由后端幂等处理。
6. `ProductSelector.vue` 的分页查询继续强制 `available: true`；`loadProduct(ids)` 继续按 ID 调用加载接口，不附加启用过滤，保证历史订单回显。批量确认窗口可复用 `BatchHandler` 的表格样式，但处理函数必须支持一次提交整批 ID，不能退化为逐条更新。

## 数据流

```text
商品列表筛选
  -> QueryProductVo.available
  -> ProductMapper.query 条件查询
  -> QueryProductBo.available
  -> 状态列与 AvailableTag

批量启用/禁用
  -> 勾选商品 IDs + 目标 available
  -> PUT /basedata/product/available
  -> ProductService 批量更新 + 清理缓存
  -> 刷新列表

历史单据回显
  -> ProductSelector.loadProduct(ids)
  -> /selector/product/load
  -> ProductService.findById(id)
  -> 返回禁用商品快照信息

新建业务单据
  -> 前端只从启用商品选择
  -> 后端 ProductService.assertAvailable(ids)
  -> 禁用商品直接拒绝保存
```

## 错误处理

- 空商品 ID 列表或空状态由参数校验拒绝。
- 商品不存在继续使用现有各业务服务的行号提示。
- 商品已禁用统一返回“商品已停用，无法新增业务单据！”。
- 批量状态更新失败时事务回滚，不产生部分状态更新。
- 缓存清理在状态更新成功后执行；历史按 ID 查询必须读取最新状态。

## 测试与验证

后端：

- 在 `backend/xingyun-basedata/src/test/java/com/lframework/xingyun/basedata/impl/product/ProductServiceImplTest.java` 增加批量状态校验和禁用商品拒绝测试。
- 增加批量状态更新只修改 `available`、支持混合状态和重复 ID 的测试。
- 增加商品查询 SQL 的启用、禁用、全部三态验证；至少通过商品模块测试和 Maven 编译验证。
- 对采购订单、销售订单及代表性的收货/出库/退货/库存单据新建入口增加禁用商品回归测试，确认后端不能绕过选择器提交。

前端：

- 执行 `cd frontend && pnpm run lint`。
- 验证商品列表默认启用、三态筛选、状态列和两个批量操作入口。
- 验证历史订单中的禁用商品仍能通过按 ID 加载回显，新的选择器分页不返回禁用商品。

## 非目标

- 不删除商品，不改变商品编号、SKU、名称+规格+单位的唯一性规则。
- 不改变历史单据中已经保存的商品快照字段。
- 不新增独立权限码，不增加数据库迁移；商品表已有 `available` 字段。
