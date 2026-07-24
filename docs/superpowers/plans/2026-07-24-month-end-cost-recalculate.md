# 月底成本重算功能 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增月末一次加权平均法成本重算功能，支持手动触发（三页面）和定时自动执行。

**Architecture:** 在 `SaleOutSheetServiceImpl` 中新增"月加权均价"模式，与现有"最近采购价""库存均价"并列；新增 Controller 接口、Quartz Job、前端按钮和弹窗。

**Tech Stack:** Java 8 / Spring Boot 2.2.2 / MyBatis-Plus 3.4.2 / Quartz / Vue 3 + ant-design-vue 4

## Global Constraints

- Controller 只做参数校验和响应包装，不写业务逻辑
- Service 承担业务编排，`@Transactional` 只放 Service 层
- 对外响应统一使用 `InvokeResult<T>`
- 业务异常使用 `DefaultClientException`
- 不要循环调用 DB，优先批量操作
- 不要直接写 SQL，使用 MyBatis-Plus 的 QueryWrapper（月加权 SQL 是聚合查询例外）
- 所有代码注释和解释使用中文

---

### Task 1: 新建 VO 和 Result DTO

**Files:**
- Create: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/vo/sale/out/MonthEndRecalculateVo.java`
- Create: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/dto/sale/out/MonthEndRecalculateResult.java`

**Interfaces:**
- Produces: `MonthEndRecalculateVo` (beginDate, endDate, scId), `MonthEndRecalculateResult` (updatedSheetCount, updatedDetailCount, notFilledCount)

- [ ] **Step 1: 创建 MonthEndRecalculateVo**

```java
package com.lframework.xingyun.sc.vo.sale.out;

import com.lframework.starter.web.core.vo.BaseVo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 月底成本重算请求参数
 */
@Data
public class MonthEndRecalculateVo implements BaseVo, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 采购时间范围起
     */
    @ApiModelProperty(value = "开始日期", required = true)
    @NotNull(message = "开始日期不能为空！")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate beginDate;

    /**
     * 采购时间范围止
     */
    @ApiModelProperty(value = "结束日期", required = true)
    @NotNull(message = "结束日期不能为空！")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * 仓库ID，为空则全仓库
     */
    @ApiModelProperty("仓库ID")
    private String scId;
}
```

- [ ] **Step 2: 创建 MonthEndRecalculateResult**

```java
package com.lframework.xingyun.sc.dto.sale.out;

import com.lframework.starter.web.core.dto.BaseDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 月底成本重算结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthEndRecalculateResult implements BaseDto, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 更新的销售出库单数
     */
    private int updatedSheetCount;

    /**
     * 更新的明细行数
     */
    private int updatedDetailCount;

    /**
     * 无采购价未填充的商品数
     */
    private int notFilledCount;
}
```

- [ ] **Step 3: 编译验证**

```bash
cd backend && mvn clean compile -DskipTests -pl xingyun-sc -am
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/vo/sale/out/MonthEndRecalculateVo.java backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/dto/sale/out/MonthEndRecalculateResult.java
git commit -m "feat: 新增月底成本重算VO和Result DTO"
```

---

### Task 2: 新增 Mapper 方法及 SQL

**Files:**
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/mappers/ReceiveSheetDetailMapper.java`
- Modify: `backend/xingyun-sc/src/main/resources/mappers/purchase/ReceiveSheetDetailMapper.xml`

**Interfaces:**
- Produces: `List<QueryReceiveSheetDetailDto> getMonthWtdAvgCostPriceList(@Param("beginDate") LocalDate beginDate, @Param("endDate") LocalDate endDate)`
- Depends on: `QueryReceiveSheetDetailDto` (already exists in `dto/purchase/receive/`)

- [ ] **Step 1: 在 ReceiveSheetDetailMapper.java 末尾新增方法签名**

在接口 `ReceiveSheetDetailMapper` 的最后一个方法 `getLatestCostPriceList` 之后添加：

```java
    /**
     * 获取时间范围内每个商品的月加权均价
     *
     * @param beginDate 开始日期
     * @param endDate   结束日期
     * @return 商品月加权均价列表
     */
    List<QueryReceiveSheetDetailDto> getMonthWtdAvgCostPriceList(
            @Param("beginDate") LocalDate beginDate,
            @Param("endDate") LocalDate endDate);
```

- [ ] **Step 2: 在 ReceiveSheetDetailMapper.xml 末尾新增 SQL**

在 `</mapper>` 结束标签之前添加：

```xml
    <select id="getMonthWtdAvgCostPriceList"
            resultMap="QueryReceiveSheetDetailDto">
        SELECT d.product_id,
               SUM(d.tax_amount) / NULLIF(SUM(d.order_num), 0) AS tax_price
        FROM tbl_receive_sheet_detail d
                 INNER JOIN tbl_receive_sheet s ON d.sheet_id = s.id
        WHERE s.order_date <![CDATA[ >= ]]> #{beginDate}
          AND s.order_date <![CDATA[ <= ]]> #{endDate}
          AND d.is_gift = 0
        GROUP BY d.product_id
    </select>
```

- [ ] **Step 3: 编译验证**

```bash
cd backend && mvn clean compile -DskipTests -pl xingyun-sc -am
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/mappers/ReceiveSheetDetailMapper.java backend/xingyun-sc/src/main/resources/mappers/purchase/ReceiveSheetDetailMapper.xml
git commit -m "feat: 新增月加权均价SQL查询方法"
```

---

### Task 3: 新增 Service 月加权重算逻辑

**Files:**
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetServiceImpl.java`
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/service/sale/SaleOutSheetService.java` (interface)

**Interfaces:**
- Consumes: `MonthEndRecalculateVo`, `MonthEndRecalculateResult` (Task 1), `ReceiveSheetDetailMapper.getMonthWtdAvgCostPriceList` (Task 2)
- Produces: `MonthEndRecalculateResult monthEndRecalculate(MonthEndRecalculateVo vo)`, `Map<String, QueryReceiveSheetDetailDto> getCostPriceMapFromMonthWtdAvg(LocalDate, LocalDate)` (private)

- [ ] **Step 1: 在 SaleOutSheetService 接口中新增方法签名**

在 `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/service/sale/SaleOutSheetService.java` 中新增：

```java
    /**
     * 月底成本重算 - 使用月加权平均法
     *
     * @param vo 重算参数（beginDate, endDate, scId）
     * @return 重算结果
     */
    MonthEndRecalculateResult monthEndRecalculate(MonthEndRecalculateVo vo);
```

需要新增 import：
```java
import com.lframework.xingyun.sc.vo.sale.out.MonthEndRecalculateVo;
import com.lframework.xingyun.sc.dto.sale.out.MonthEndRecalculateResult;
```

- [ ] **Step 2: 在 SaleOutSheetServiceImpl 中实现核心逻辑**

在 `SaleOutSheetServiceImpl` 类中，`getCostPriceMapFromReceiveSheet` 方法之后，`toCostPriceMap` 之前，新增以下方法：

```java
    /**
     * 月底成本重算 - 使用月加权平均法
     * <p>
     * 1. 计算时间范围内的月加权均价（采购总金额 / 采购总数量）
     * 2. 当月无采购的商品回退到最近一次采购价
     * 3. 遍历时间范围内所有销售出库单，更新 costPrice / totalProfit
     *
     * @param vo 重算参数
     * @return 重算结果
     */
    @Transactional(rollbackFor = Exception.class)
    public MonthEndRecalculateResult monthEndRecalculate(MonthEndRecalculateVo vo) {
        LocalDate beginDate = vo.getBeginDate();
        LocalDate endDate = vo.getEndDate();

        log.info("月底成本重算开始, beginDate: {}, endDate: {}, scId: {}", beginDate, endDate, vo.getScId());

        // 1. 计算月加权均价
        Map<String, QueryReceiveSheetDetailDto> monthWtdAvgMap =
                getCostPriceMapFromMonthWtdAvg(beginDate, endDate);

        // 2. 回退方案：当月无采购的商品用最近一次采购价
        Map<String, QueryReceiveSheetDetailDto> fallbackMap =
                getCostPriceMapFromReceiveSheet(endDate);

        // 3. 查询时间范围内所有销售出库单
        LambdaQueryWrapper<SaleOutSheet> queryWrapper = Wrappers.lambdaQuery(SaleOutSheet.class)
                .ge(SaleOutSheet::getOrderDate, beginDate)
                .le(SaleOutSheet::getOrderDate, endDate);
        if (StringUtils.isNotBlank(vo.getScId())) {
            queryWrapper.eq(SaleOutSheet::getScId, vo.getScId());
        }
        List<SaleOutSheet> sheets = getBaseMapper().selectList(queryWrapper);

        if (CollectionUtils.isEmpty(sheets)) {
            log.info("月底成本重算：时间范围内无销售出库单");
            return new MonthEndRecalculateResult(0, 0, 0);
        }

        // 4. 逐单据更新成本
        int detailCount = 0;
        int notFilledCount = 0;
        for (SaleOutSheet sheet : sheets) {
            List<SaleOutSheetDetail> details = saleOutSheetDetailService.getBySheetId(sheet.getId());
            if (CollectionUtils.isEmpty(details)) {
                continue;
            }

            boolean fillAllCost = true;
            BigDecimal totalCostAmount = BigDecimal.ZERO;
            for (SaleOutSheetDetail detail : details) {
                // 优先月加权均价，无则回退到最近采购价
                QueryReceiveSheetDetailDto costDto = monthWtdAvgMap.get(detail.getProductId());
                if (costDto == null) {
                    costDto = fallbackMap.get(detail.getProductId());
                }

                if (costDto == null) {
                    fillAllCost = false;
                    notFilledCount++;
                    detail.setCostPrice(null);
                    detail.setTotalProfit(null);
                    saleOutSheetDetailService.saveOrUpdateAllColumn(detail);
                    continue;
                }

                BigDecimal detailCostAmount = NumberUtil.calculateAmount(
                        NumberUtil.getDefaultValue(costDto.getTaxPrice()), resolveCostNum(detail));
                BigDecimal detailTotalProfit = NumberUtil.getNumber(
                        NumberUtil.sub(resolveConfirmAmt(detail), detailCostAmount),
                        NumberUtil.AMT_PRECISION);
                totalCostAmount = NumberUtil.add(totalCostAmount, detailCostAmount);

                detail.setCostPrice(costDto.getTaxPrice());
                detail.setTotalProfit(detailTotalProfit);
                detail.setSupplierId(costDto.getSupplierId());
                saleOutSheetDetailService.saveOrUpdateAllColumn(detail);
                detailCount++;
            }

            // 汇总单据总利润
            BigDecimal totalProfit = details.stream()
                    .map(item -> NumberUtil.getDefaultValue(item.getTotalProfit()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            LambdaUpdateWrapper<SaleOutSheet> updateWrapper = Wrappers.lambdaUpdate(SaleOutSheet.class)
                    .set(SaleOutSheet::getTotalCost, totalCostAmount)
                    .set(SaleOutSheet::getTotalProfit, totalProfit)
                    .set(SaleOutSheet::getFillAllCost, fillAllCost)
                    .eq(SaleOutSheet::getId, sheet.getId());
            this.update(updateWrapper);
        }

        log.info("月底成本重算完成, 单据数: {}, 明细数: {}, 未填充数: {}",
                sheets.size(), detailCount, notFilledCount);
        return new MonthEndRecalculateResult(sheets.size(), detailCount, notFilledCount);
    }

    /**
     * 计算月加权均价
     * <p>
     * 月加权均价 = SUM(采购总金额) / SUM(采购总数量)
     * 已过滤赠品（is_gift = 0），数据在 SQL 中已汇总
     *
     * @param beginDate 采购时间范围起
     * @param endDate   采购时间范围止
     * @return productId -> 月加权均价
     */
    private Map<String, QueryReceiveSheetDetailDto> getCostPriceMapFromMonthWtdAvg(
            LocalDate beginDate, LocalDate endDate) {
        List<QueryReceiveSheetDetailDto> list =
                receiveSheetDetailMapper.getMonthWtdAvgCostPriceList(beginDate, endDate);
        return toCostPriceMap(list);
    }
```

需要新增 import：
```java
import com.lframework.xingyun.sc.vo.sale.out.MonthEndRecalculateVo;
import com.lframework.xingyun.sc.dto.sale.out.MonthEndRecalculateResult;
import org.apache.commons.lang3.StringUtils;
```

- [ ] **Step 3: 编译验证 + 运行已有测试**

```bash
cd backend && mvn clean compile -DskipTests -pl xingyun-sc -am
```

Expected: BUILD SUCCESS

```bash
cd backend && mvn test -pl xingyun-sc -Dtest=SaleOutSheetServiceImplTest
```

Expected: 已有测试全部通过

- [ ] **Step 4: Commit**

```bash
git add backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/service/sale/SaleOutSheetService.java backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/impl/sale/SaleOutSheetServiceImpl.java
git commit -m "feat: 新增月底月加权平均法成本重算Service逻辑"
```

---

### Task 4: 新增 Controller 接口

**Files:**
- Modify: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/controller/sale/SaleOutSheetController.java`

**Interfaces:**
- Consumes: `SaleOutSheetService.monthEndRecalculate()` (Task 3), `MonthEndRecalculateVo`, `MonthEndRecalculateResult` (Task 1)
- Produces: `POST /sale/out/sheet/month-end/recalculate` → `InvokeResult<MonthEndRecalculateResult>`

- [ ] **Step 1: 在 SaleOutSheetController 中新增接口**

在类末尾 `}` 之前（`refreshCostPrice` 方法之后）新增：

```java
    /**
     * 月底成本重算 - 月加权平均法
     */
    @ApiOperation("月底成本重算 - 月加权平均法")
    @HasPermission({"report:sale-profit:query"})
    @PostMapping("/month-end/recalculate")
    public InvokeResult<MonthEndRecalculateResult> monthEndRecalculate(
            @Valid @RequestBody MonthEndRecalculateVo vo) {

        try {
            MonthEndRecalculateResult result = saleOutSheetService.monthEndRecalculate(vo);
            return InvokeResult.success(result);
        } catch (Exception e) {
            log.error("月底成本重算失败", e);
            return InvokeResultBuilder.fail(e.getMessage());
        }
    }
```

新增 import：
```java
import com.lframework.xingyun.sc.vo.sale.out.MonthEndRecalculateVo;
import com.lframework.xingyun.sc.dto.sale.out.MonthEndRecalculateResult;
```

注意：`@Valid @RequestBody` 需要确认 Controller 已有 `@Validated` 注解（类级别已有），`@RequestBody` 的 import 也已存在。

- [ ] **Step 2: 编译验证**

```bash
cd backend && mvn clean compile -DskipTests -pl xingyun-sc -am
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/controller/sale/SaleOutSheetController.java
git commit -m "feat: 新增月底成本重算Controller接口"
```

---

### Task 5: 新建 Quartz Job 类

**Files:**
- Create: `backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/job/MonthEndCostRecalculateJob.java`

**Interfaces:**
- Consumes: `SaleOutSheetService.monthEndRecalculate()` (Task 3)
- Produces: Quartz Job 类，供 `/platform/development/qrtz` 页面配置使用

- [ ] **Step 1: 创建 Quartz Job 类**

```java
package com.lframework.xingyun.sc.job;

import com.lframework.starter.web.core.components.qrtz.QrtzJob;
import com.lframework.xingyun.sc.dto.sale.out.MonthEndRecalculateResult;
import com.lframework.xingyun.sc.service.sale.SaleOutSheetService;
import com.lframework.xingyun.sc.vo.sale.out.MonthEndRecalculateVo;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

/**
 * 月底成本重算定时任务
 * <p>
 * 每月最后一天 23:00 自动执行，使用月加权平均法重算当月所有销售出库单成本。
 * <p>
 * Job 配置（通过 /platform/development/qrtz 页面手工添加）：
 * <ul>
 *   <li>Job Class: com.lframework.xingyun.sc.job.MonthEndCostRecalculateJob</li>
 *   <li>Job Group: COST_RECALCULATE</li>
 *   <li>Trigger Type: CRON</li>
 *   <li>Cron 表达式: 0 0 23 L * ?（每月最后一天 23:00）</li>
 *   <li>描述: 月底成本重算 - 月加权平均法</li>
 * </ul>
 */
@Slf4j
public class MonthEndCostRecalculateJob extends QrtzJob {

    @Autowired
    private SaleOutSheetService saleOutSheetService;

    @Override
    public void onExecute(JobExecutionContext context) {
        LocalDate now = LocalDate.now();

        MonthEndRecalculateVo vo = new MonthEndRecalculateVo();
        vo.setBeginDate(now.withDayOfMonth(1));  // 本月1日
        vo.setEndDate(now);                       // 当天（月底最后一天）

        log.info("月底成本重算定时任务开始, beginDate: {}, endDate: {}", vo.getBeginDate(), vo.getEndDate());
        MonthEndRecalculateResult result = saleOutSheetService.monthEndRecalculate(vo);
        log.info("月底成本重算定时任务完成, 单据数: {}, 明细数: {}, 未填充数: {}",
                result.getUpdatedSheetCount(), result.getUpdatedDetailCount(), result.getNotFilledCount());
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd backend && mvn clean compile -DskipTests -pl xingyun-sc -am
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/xingyun-sc/src/main/java/com/lframework/xingyun/sc/job/MonthEndCostRecalculateJob.java
git commit -m "feat: 新增月底成本重算Quartz定时任务Job"
```

---

### Task 6: 前端 API 函数

**Files:**
- Modify: `frontend/src/api/sc/sale/out/index.ts`

**Interfaces:**
- Produces: `monthEndRecalculate(params)` API 函数
- Depends on: 后端接口 `POST /sale/out/sheet/month-end/recalculate` (Task 4)

- [ ] **Step 1: 在 index.ts 中新增 API 函数**

在 `refreshCostPrice` 函数之后添加：

```typescript
/**
 * 月底成本重算 - 月加权平均法
 */
export function monthEndRecalculate(params: {
  beginDate: string;
  endDate: string;
  scId?: string;
}): Promise<{
  updatedSheetCount: number;
  updatedDetailCount: number;
  notFilledCount: number;
}> {
  return defHttp.post<{
    updatedSheetCount: number;
    updatedDetailCount: number;
    notFilledCount: number;
  }>(
    {
      url: baseUrl + '/month-end/recalculate',
      data: params,
    },
    {
      region,
      contentType: ContentTypeEnum.JSON,
    },
  );
}
```

- [ ] **Step 2: 前端编译验证**

```bash
cd frontend && pnpm run lint
```

Expected: 无新增 lint 错误

- [ ] **Step 3: Commit**

```bash
git add frontend/src/api/sc/sale/out/index.ts
git commit -m "feat: 新增月底成本重算前端API函数"
```

---

### Task 7: 修改 sale-profit/product.vue — 成本重算按钮

**Files:**
- Modify: `frontend/src/views/report/sale-profit/product.vue`

**Interfaces:**
- Consumes: `monthEndRecalculate` API (Task 6)

> **说明**: product.vue 已有"成本重算"按钮和弹窗（调用 `refreshCostPrice`），本任务将其改为调用新的 `monthEndRecalculate` API，并调整弹窗以支持仓库选择（可选）。保留原有的日期弹窗组件，将 API 调用替换即可。

- [ ] **Step 1: 修改 import — 引入新 API 函数**

在 `<script>` 区域找到 `import` 部分，在 `refreshCostPrice` 的 import 旁边新增/替换：

```typescript
import { monthEndRecalculate } from '@/api/sc/sale/out';
```

（保留 `refreshCostPrice` 的 import 如果其他地方还需要用到，但 product.vue 中 `refreshCostPrice` 应该可以移除——检查只有 `executeCostRefresh` 调用它）

- [ ] **Step 2: 修改 executeCostRefresh 方法 — 改用新 API**

找到 `executeCostRefresh` 方法（约第 490 行），替换为：

```typescript
    executeCostRefresh() {
      const [beginDate, endDate] = this.costRefreshDateRange || [];
      if (!beginDate || !endDate) {
        return;
      }
      this.costRefreshLoading = true;
      monthEndRecalculate({
        beginDate,
        endDate,
      })
        .then((res) => {
          createSuccess(
            `重算完成：更新单据 ${res.updatedSheetCount} 条，明细 ${res.updatedDetailCount} 条` +
              (res.notFilledCount > 0 ? `，${res.notFilledCount} 条未填充` : ''),
          );
          this.costRefreshVisible = false;
          this.search();
        })
        .finally(() => {
          this.costRefreshLoading = false;
        });
    },
```

- [ ] **Step 3: 前端 lint 检查**

```bash
cd frontend && pnpm run lint
```

Expected: 无新增 lint 错误

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/report/sale-profit/product.vue
git commit -m "feat: 商品销售利润报表改用月加权平均法成本重算"
```

---

### Task 8: 修改 sale-profit/sheet.vue — 成本重算按钮

**Files:**
- Modify: `frontend/src/views/report/sale-profit/sheet.vue`

**Interfaces:**
- Consumes: `monthEndRecalculate` API (Task 6)

> **说明**: 与 Task 7 相同的改动模式，将 `refreshCostPrice` 替换为 `monthEndRecalculate`。

- [ ] **Step 1: 修改 import — 引入新 API 函数**

在 `<script>` 区域找到 `import` 部分，新增：

```typescript
import { monthEndRecalculate } from '@/api/sc/sale/out';
```

- [ ] **Step 2: 修改 executeCostRefresh 方法 — 改用新 API**

找到 `executeCostRefresh` 方法（约第 610 行），替换为：

```typescript
    executeCostRefresh() {
      const [beginDate, endDate] = this.costRefreshDateRange || [];
      if (!beginDate || !endDate) {
        return;
      }
      this.costRefreshLoading = true;
      monthEndRecalculate({
        beginDate,
        endDate,
      })
        .then((res) => {
          createSuccess(
            `重算完成：更新单据 ${res.updatedSheetCount} 条，明细 ${res.updatedDetailCount} 条` +
              (res.notFilledCount > 0 ? `，${res.notFilledCount} 条未填充` : ''),
          );
          this.costRefreshVisible = false;
          this.search();
        })
        .finally(() => {
          this.costRefreshLoading = false;
        });
    },
```

- [ ] **Step 3: 前端 lint 检查**

```bash
cd frontend && pnpm run lint
```

Expected: 无新增 lint 错误

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/report/sale-profit/sheet.vue
git commit -m "feat: 单据销售利润报表改用月加权平均法成本重算"
```

---

### Task 9: 销售出库列表页新增"重算成本"按钮

**Files:**
- Modify: `frontend/src/views/sc/sale/out/components/sheet-list.vue`

**Interfaces:**
- Consumes: `monthEndRecalculate` API (Task 6)

> **说明**: sheet-list.vue 目前没有成本重算按钮。需要新增按钮、弹窗（日期范围选择器），以及对应的 data/methods。

- [ ] **Step 1: 新增弹窗模板**

在 `sheet-list.vue` 的 `<template>` 区域，`</page-wrapper>` 之前（即最后一个 `</template>` 之前）新增弹窗：

```vue
          <!-- 月底成本重算弹窗 -->
          <a-modal
            v-model:visible="costRecalculateVisible"
            title="月底成本重算"
            :confirm-loading="costRecalculateLoading"
            @ok="executeCostRecalculate"
          >
            <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
              <a-form-item label="时间范围">
                <a-range-picker
                  v-model:value="costRecalculateDateRange"
                  value-format="YYYY-MM-DD"
                  :placeholder="['开始日期', '结束日期']"
                />
              </a-form-item>
            </a-form>
          </a-modal>
```

- [ ] **Step 2: 在工具栏新增"重算成本"按钮**

在工具栏按钮区域（`<template #toolbar_buttons>` 内的 `<a-space>` 中）添加按钮。建议放在"买菜汇总2"按钮后面、"标签打印"按钮前面：

```vue
              <a-button
                v-permission="['sale:out:query']"
                :icon="h(SyncOutlined)"
                @click="openCostRecalculate"
                >重算成本
              </a-button>
```

需要在 `<script>` 顶部 import 区域添加 `SyncOutlined` 图标（检查是否已有）。

- [ ] **Step 3: 新增 data 属性**

在 `data()` 返回的对象中新增：

```javascript
        // 月底成本重算
        costRecalculateVisible: false,
        costRecalculateLoading: false,
        costRecalculateDateRange: [],
```

- [ ] **Step 4: 新增 methods**

在 `methods` 中新增：

```javascript
      /**
       * 打开成本重算弹窗，默认时间范围为月初到今天
       */
      openCostRecalculate() {
        const now = new Date();
        const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
        const formatDate = (d) => {
          const year = d.getFullYear();
          const month = String(d.getMonth() + 1).padStart(2, '0');
          const day = String(d.getDate()).padStart(2, '0');
          return `${year}-${month}-${day}`;
        };
        this.costRecalculateDateRange = [formatDate(firstDay), formatDate(now)];
        this.costRecalculateVisible = true;
      },
      /**
       * 执行月底成本重算
       */
      executeCostRecalculate() {
        const [beginDate, endDate] = this.costRecalculateDateRange || [];
        if (!beginDate || !endDate) {
          return;
        }
        this.costRecalculateLoading = true;
        api
          .monthEndRecalculate({ beginDate, endDate })
          .then((res) => {
            createSuccess(
              `重算完成：更新单据 ${res.updatedSheetCount} 条，明细 ${res.updatedDetailCount} 条` +
                (res.notFilledCount > 0 ? `，${res.notFilledCount} 条未填充` : ''),
            );
            this.costRecalculateVisible = false;
            this.search();
          })
          .finally(() => {
            this.costRecalculateLoading = false;
          });
      },
```

- [ ] **Step 5: 修改 import — 引入 API 和图标**

在 `<script>` 顶部：
- 找到 `import * as api from '@/api/sc/sale/out';`（或类似的 api import），确认 `monthEndRecalculate` 会通过 `api` 对象访问
- 确认 `SyncOutlined` 已从 `@ant-design/icons-vue` 导入。如果没有，添加：
  ```typescript
  import { SyncOutlined } from '@ant-design/icons-vue';
  ```
- 确认 `createSuccess` 已从 utils 导入（检查 `import { createSuccess } from '/@/utils/message';` 或类似路径）

- [ ] **Step 6: 前端 lint 检查**

```bash
cd frontend && pnpm run lint
```

Expected: 无新增 lint 错误

- [ ] **Step 7: Commit**

```bash
git add frontend/src/views/sc/sale/out/components/sheet-list.vue
git commit -m "feat: 销售出库列表页新增月加权平均法成本重算按钮"
```

---

### Task 10: 集成验证与端到端测试

- [ ] **Step 1: 启动后端**

```bash
cd backend/xingyun-api && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

- [ ] **Step 2: 启动前端**

```bash
cd frontend && pnpm run dev
```

- [ ] **Step 3: 验证接口**

```bash
# 调用月加权重算接口
curl -X POST 'http://localhost:8080/sale/out/sheet/month-end/recalculate' \
  -H 'Content-Type: application/json' \
  -d '{"beginDate":"2026-07-01","endDate":"2026-07-24"}'
```

Expected: 返回 JSON，包含 `updatedSheetCount`, `updatedDetailCount`, `notFilledCount`

- [ ] **Step 4: 验证三个页面的"重算成本"按钮**

1. `/sc/sale/out` → 点击"重算成本" → 弹窗 → 确认 → 提示成功数量
2. `/report/sale-profit/product` → 点击"成本重算" → 弹窗 → 确认 → 提示成功数量
3. `/report/sale-profit/sheet` → 点击"成本重算" → 弹窗 → 确认 → 提示成功数量

- [ ] **Step 5: 验证 Quartz Job 可配置**

访问 `/platform/development/qrtz`，确认可以找到并添加 `MonthEndCostRecalculateJob` 类

- [ ] **Step 6: Commit（如有修复）**

```bash
git add -A && git commit -m "fix: 集成验证修复"
```
