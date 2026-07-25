# 月底成本重算功能设计

**日期**: 2026-07-24
**作者**: bogewang
**状态**: 待实现

---

## 1. 背景

### 1.1 现状

系统当前成本价格计算采用"最近一次采购价"模式：

- 销售出库单创建/刷新时，取该商品近 6 个月内**最近一次采购收货单价**作为成本价（`SaleOutSheetDetail.costPrice`）
- 库存表 `tbl_product_stock` 已按**移动加权平均法**维护 `tax_price`（每次采购入库实时重算）
- 系统参数 `sale_out_cost_price_use_stock_price` 可切换成本来源（最近采购价 / 库存均价）
- 现有 `refreshCostPrice()` 方法位于 `SaleOutSheetServiceImpl`（第 2042-2098 行）

### 1.2 目标

新增**月末一次加权平均法**成本重算功能：

1. 每月月底，用当月所有采购收货数据计算月加权均价，覆盖当月销售出库单的成本价和利润
2. 支持定时自动执行 + 三个页面手动触发
3. 当月无采购的商品回退到最近一次采购价

---

## 2. 核心算法

### 2.1 月加权均价计算

```
月加权均价 = SUM(当月采购总金额) / SUM(当月采购总数量)

条件:
- 时间范围: 用户指定的 [beginDate, endDate]
- 过滤：is_gift = 0（排除赠品）
- 金额/数量均为基本单位（conversion_rate 换算后）
```

### 2.2 成本价回填规则

```
对每条销售出库单明细:
  当月有采购 → costPrice = 月加权均价
  当月无采购 → costPrice = 最近一次采购价（回退现有逻辑 getCostPriceMapFromReceiveSheet）
  totalProfit  = resolveConfirmAmt(saleDetail) - (costPrice × resolveCostNum(saleDetail))
```

`resolveConfirmAmt()`: `confirmAmt` 为空或 ≤0 时回退到 `taxAmount`（出库金额）
`resolveCostNum()`: 返回 `orderNum`（基本单位数量）

### 2.3 与现有流程的关系

```
现有流程（保持不变）:
  销售出库创建/修改 → refreshCostPrice() → 取【最近采购价】 → 写入 costPrice

新增流程（月底触发）:
  手动/定时触发 → monthEndRecalculate() → 取【月加权均价】 → 覆盖 costPrice
```

修改 `getCostPriceMap()` 方法，增加月加权模式分支。

### 2.4 核心 SQL（新增）

```sql
-- 计算时间范围内每个商品的月加权均价
SELECT d.product_id,
       SUM(d.tax_amount) / NULLIF(SUM(d.order_num), 0) AS tax_price
FROM tbl_receive_sheet_detail d
INNER JOIN tbl_receive_sheet s ON d.sheet_id = s.id
WHERE s.order_date >= #{beginDate}
  AND s.order_date <= #{endDate}
  AND d.is_gift = 0
GROUP BY d.product_id
```

---

## 3. 后端设计

### 3.1 API

**`POST /sc/sale/out/sheet/month-end/recalculate`**

请求：
```json
{
  "beginDate": "2026-07-01",
  "endDate": "2026-07-24",
  "scId": "xxx"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| beginDate | LocalDate | 是 | 采购时间范围起 |
| endDate | LocalDate | 是 | 采购时间范围止 |
| scId | String | 否 | 仓库ID，为空则全仓库 |

响应：
```json
{
  "updatedSheetCount": 15,
  "updatedDetailCount": 86,
  "notFilledCount": 3
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| updatedSheetCount | int | 更新的销售出库单数 |
| updatedDetailCount | int | 更新的明细行数 |
| notFilledCount | int | 无采购价未填充的商品数 |

### 3.2 改动文件

| 文件 | 改动 |
|------|------|
| `ReceiveSheetDetailMapper.java` | 新增 `getMonthWtdAvgCostPriceList(@Param("beginDate") LocalDate beginDate, @Param("endDate") LocalDate endDate)` |
| `ReceiveSheetDetailMapper.xml` | 新增月加权均价 SQL（见 2.4） |
| `SaleOutSheetServiceImpl.java` | 新增 `getCostPriceMapFromMonthWtdAvg()`、`monthEndRecalculate()`；修改 `getCostPriceMap()` 增加月加权模式 |
| `MonthEndRecalculateVo.java` | **新增** VO: beginDate, endDate, scId |
| `MonthEndRecalculateResult.java` | **新增** 结果 DTO: updatedSheetCount, updatedDetailCount, notFilledCount |
| `SaleOutSheetController.java` | 新增 `POST /month-end/recalculate` 接口 |

### 3.3 Service 核心逻辑

```java
// SaleOutSheetServiceImpl

/**
 * 月底成本重算 - 使用月加权平均法
 */
public MonthEndRecalculateResult monthEndRecalculate(MonthEndRecalculateVo vo) {
    LocalDate beginDate = vo.getBeginDate();
    LocalDate endDate = vo.getEndDate();

    // 1. 计算月加权均价
    Map<String, QueryReceiveSheetDetailDto> monthWtdAvgMap =
            getCostPriceMapFromMonthWtdAvg(beginDate, endDate);

    // 2. 回退方案：当月无采购的用最近采购价
    Map<String, QueryReceiveSheetDetailDto> fallbackMap =
            getCostPriceMapFromReceiveSheet(endDate);

    // 3. 查询时间范围内所有销售出库单
    List<SaleOutSheet> sheets = getBaseMapper().selectList(
        Wrappers.lambdaQuery(SaleOutSheet.class)
            .ge(SaleOutSheet::getOrderDate, beginDate)
            .le(SaleOutSheet::getOrderDate, endDate)
            .eq(vo.getScId() != null, SaleOutSheet::getScId, vo.getScId()));

    // 4. 逐单据更新成本
    int detailCount = 0, notFilled = 0;
    for (SaleOutSheet sheet : sheets) {
        // 使用月加权均价 + 最近采购价兜底
        // 核心逻辑复用 refreshCostPrice 内部循环
        // ...
    }

    return new MonthEndRecalculateResult(sheets.size(), detailCount, notFilled);
}

/**
 * 计算月加权均价
 */
private Map<String, QueryReceiveSheetDetailDto> getCostPriceMapFromMonthWtdAvg(
        LocalDate beginDate, LocalDate endDate) {
    List<QueryReceiveSheetDetailDto> list =
        receiveSheetDetailMapper.getMonthWtdAvgCostPriceList(beginDate, endDate);
    return toCostPriceMap(list);
}
```

### 3.4 Controller

```java
@PostMapping("/month-end/recalculate")
public InvokeResult<MonthEndRecalculateResult> monthEndRecalculate(
        @Valid @RequestBody MonthEndRecalculateVo vo) {
    try {
        return InvokeResult.success(
            saleOutSheetService.monthEndRecalculate(vo));
    } catch (Exception e) {
        log.error("月底成本重算失败", e);
        return InvokeResultBuilder.fail(e.getMessage());
    }
}
```

---

## 4. 定时任务

### 4.1 Job 类

**新增** `MonthEndCostRecalculateJob.java`：

```java
package com.lframework.xingyun.sc.job;

/**
 * 月底成本重算定时任务
 * 每月最后一天 23:00 自动执行
 */
public class MonthEndCostRecalculateJob implements Job {

    @Autowired
    private SaleOutSheetService saleOutSheetService;

    @Override
    public void execute(JobExecutionContext context) {
        LocalDate now = LocalDate.now();
        MonthEndRecalculateVo vo = new MonthEndRecalculateVo();
        vo.setBeginDate(now.withDayOfMonth(1));  // 本月1日
        vo.setEndDate(now);                       // 当天（月底最后一天）
        saleOutSheetService.monthEndRecalculate(vo);
    }
}
```

### 4.2 任务配置

通过系统现有 Quartz 管理页面 `/platform/development/qrtz` 手动配置：

| 配置项 | 值 |
|--------|-----|
| Job Class | `com.lframework.xingyun.sc.job.MonthEndCostRecalculateJob` |
| Job Group | `COST_RECALCULATE` |
| Trigger Type | `CRON` |
| Cron 表达式 | `0 0 23 L * ?`（每月最后一天 23:00） |
| 描述 | 月底成本重算 - 月加权平均法 |

---

## 5. 前端设计

### 5.1 按钮位置

| 页面 | 路由 | 按钮位置 |
|------|------|----------|
| 销售出库列表 | `/sc/sale/out` | 工具栏区域，与"导出"等按钮并列 |
| 销售利润报表(商品) | `/report/sale-profit/product` | 筛选区域上方 |
| 销售利润报表(单据) | `/report/sale-profit/sheet` | 筛选区域上方 |

### 5.2 交互流程

```
点击"重算成本" → 弹出时间范围选择弹窗
  - 日期范围选择器: 默认 本月1日 ~ 今天，可自定义
  - 仓库: 下拉选，默认全部
  - [确认] [取消]
→ 确认 → 调用 POST /sc/sale/out/sheet/month-end/recalculate
→ 成功 → message.success("重算完成：更新单据X条，明细Y条")
→ 自动刷新当前页面数据
→ 失败 → message.error(错误信息)
```

### 5.3 弹窗组件结构

- `a-modal` 弹窗，标题"月底成本重算"
- `a-range-picker` 日期范围选择器
- 仓库选择器（可选，默认全部）
- 确认按钮 loading 态防重复提交

### 5.4 改动文件

| 文件 | 改动 |
|------|------|
| `frontend/src/api/sc/sale/out/index.ts` | 新增 `monthEndRecalculate()` API 函数 |
| `frontend/src/views/sc/sale/out/index.vue` 或相关组件 | 新增"重算成本"按钮 + 弹窗 |
| `frontend/src/views/report/sale-profit/product.vue` | 新增"重算成本"按钮 + 弹窗 |
| `frontend/src/views/report/sale-profit/sheet.vue` | 新增"重算成本"按钮 + 弹窗 |

### 5.5 API 函数

```typescript
// frontend/src/api/sc/sale/out/index.ts
export function monthEndRecalculate(params: {
  beginDate: string;
  endDate: string;
  scId?: string;
}) {
  return defHttp.post<{
    updatedSheetCount: number;
    updatedDetailCount: number;
    notFilledCount: number;
  }>({
    url: '/sc/sale/out/sheet/month-end/recalculate',
    params,
  });
}
```

---

## 6. 边界与错误处理

| 场景 | 处理方式 |
|------|----------|
| 时间范围内无采购数据 | 全部商品回退到最近一次采购价（现有逻辑） |
| 某商品当月无采购 | 该商品回退到最近一次采购价 |
| 时间范围内无销售出库单 | 返回 `updatedSheetCount=0`，不报错 |
| 采购数量为 0（除零保护） | SQL 中 `NULLIF(SUM(order_num), 0)` |
| 赠品过滤 | SQL 中 `d.is_gift = 0` |
| 并发重算 | Controller 层不做特殊处理，多次执行幂等（结果相同） |
| 定时任务执行失败 | Quartz 自身有 misfire 策略，日志记录异常 |
| 翻页/大数据量 | 通过 `scId` 拆分，按仓库分批执行 |

---

## 7. 注意事项

1. **不改变现有出库流程**：销售出库单创建时仍用最近采购价，月底重算时覆盖
2. **不改变库存表成本**：月加权均价只影响销售出库单的 `costPrice`/`totalProfit`，不改 `tbl_product_stock.tax_price`
3. **定时任务配置**：Job 类写好后，需人工到 `/platform/development/qrtz` 页面添加 Job 和 Trigger
4. **前端已有的三个页面**：`sale-profit/product.vue` 和 `sheet.vue` 在 `fix/0724` 分支已有修改，需注意合并
