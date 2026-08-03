package com.lframework.xingyun.sc.impl.sale;

import com.lframework.xingyun.sc.entity.SaleOutSheet;
import com.lframework.xingyun.sc.entity.SaleOutSheetDetail;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static com.lframework.starter.common.utils.NumberUtil.*;

/**
 * 销售出库单验收金额计算器。
 */
public final class SaleOutSheetAmtCalculator {

    private static final int LINE_AMOUNT_PRECISION = 2;

    private SaleOutSheetAmtCalculator() {
    }

    /**
     * 按数量和含税单价计算单行金额，并四舍五入到两位小数。
     *
     * @param taxPrice 含税单价
     * @param quantity 数量
     * @return 两位小数的单行金额
     */
    public static BigDecimal calculateLineAmount(BigDecimal taxPrice, BigDecimal quantity) {
        return calculateAmount(taxPrice, quantity)
                .setScale(LINE_AMOUNT_PRECISION, RoundingMode.HALF_UP);
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

                BigDecimal taxAmount = calculateLineAmount(detail.getTaxPrice(), detail.getBusinessNum());
                detail.setTaxAmount(taxAmount);
                totalAmount = totalAmount.add(taxAmount);
                BigDecimal confirmAmt = calculateLineAmount(detail.getTaxPrice(), detail.getConfirmNum());
                detail.setConfirmAmt(confirmAmt);
                totalConfirmAmt = totalConfirmAmt.add(confirmAmt);
            }
        }

        sheet.setTotalNum(getNumber(totalBusinessNum, NUM_PRECISION));
        sheet.setConfirmNum(getNumber(totalConfirmNum, NUM_PRECISION));

        sheet.setTotalAmount(totalAmount.setScale(LINE_AMOUNT_PRECISION, RoundingMode.HALF_UP));
        sheet.setConfirmAmt(totalConfirmAmt.setScale(LINE_AMOUNT_PRECISION, RoundingMode.HALF_UP));

        sheet.setTotalCost(BigDecimal.ZERO);
        sheet.setTotalProfit(BigDecimal.ZERO);
    }
}
