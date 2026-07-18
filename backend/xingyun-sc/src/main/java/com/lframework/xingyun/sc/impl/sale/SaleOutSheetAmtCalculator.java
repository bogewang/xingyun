package com.lframework.xingyun.sc.impl.sale;

import com.lframework.xingyun.sc.entity.SaleOutSheet;
import com.lframework.xingyun.sc.entity.SaleOutSheetDetail;

import java.math.BigDecimal;
import java.util.List;

import static com.lframework.starter.common.utils.NumberUtil.*;

/**
 * 销售出库单验收金额计算器。
 */
public final class SaleOutSheetAmtCalculator {

    private SaleOutSheetAmtCalculator() {
    }

    /**
     * 计算单据头部验收汇总，并覆盖原有头部值。
     *
     * @param sheet   销售出库单
     * @param details 销售出库单明细
     */
    public static void calculateSheet(SaleOutSheet sheet, List<SaleOutSheetDetail> details) {
        if (sheet == null) {
            return;
        }

        BigDecimal totalConfirmNum = BigDecimal.ZERO;
        BigDecimal totalConfirmAmt = BigDecimal.ZERO;
        BigDecimal totalBusinessNum = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;

        if (details != null) {
            for (SaleOutSheetDetail detail : details) {
                if (detail == null) {
                    continue;
                }
                totalBusinessNum = totalBusinessNum.add(getDefaultValue(detail.getBusinessNum()));
                totalConfirmNum = totalConfirmNum.add(getDefaultValue(detail.getConfirmNum()));

                totalAmount = totalAmount.add(getDefaultValue(detail.getTaxAmount()));
                totalConfirmAmt = totalConfirmAmt.add(getDefaultValue(detail.getConfirmAmt()));
            }
        }

        sheet.setTotalNum(getNumber(totalBusinessNum, NUM_PRECISION));
        sheet.setConfirmNum(getNumber(totalConfirmNum, NUM_PRECISION));

        sheet.setTotalAmount(getNumber(totalAmount, AMT_PRECISION));
        sheet.setConfirmAmt(getNumber(totalConfirmAmt, AMT_PRECISION));

        sheet.setTotalCost(BigDecimal.ZERO);
        sheet.setTotalProfit(BigDecimal.ZERO);
    }
}
