package com.lframework.xingyun.sc.job;

import com.lframework.starter.web.core.components.qrtz.QrtzJob;
import com.lframework.xingyun.sc.dto.sale.out.MonthEndRecalculateResult;
import com.lframework.xingyun.sc.service.sale.SaleOutSheetService;
import com.lframework.xingyun.sc.vo.sale.out.MonthEndRecalculateVo;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
@Service
public class MonthEndCostRecalculateJob extends QrtzJob {

    @Autowired
    private SaleOutSheetService saleOutSheetService;

    @Override
    public void onExecute(JobExecutionContext context) {
        LocalDate now = LocalDate.now();

        MonthEndRecalculateVo vo = new MonthEndRecalculateVo();
        // 本月1日
        vo.setBeginDate(now.withDayOfMonth(1));
        // 当天（月底最后一天）
        vo.setEndDate(now);

        log.info("月底成本重算定时任务开始, beginDate: {}, endDate: {}", vo.getBeginDate(), vo.getEndDate());
        MonthEndRecalculateResult result = saleOutSheetService.monthEndRecalculate(vo);
        log.info("月底成本重算定时任务完成, 单据数: {}, 明细数: {}, 未填充数: {}",
                result.getUpdatedSheetCount(), result.getUpdatedDetailCount(), result.getNotFilledCount());
    }
}
