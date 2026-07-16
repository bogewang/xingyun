package com.lframework.xingyun.sc.service.stock;

import com.lframework.starter.web.core.service.BaseMpService;
import com.lframework.xingyun.sc.dto.stock.ProductStockPendingCostResolveDto;
import com.lframework.xingyun.sc.entity.ProductStockPendingCost;
import com.lframework.xingyun.sc.entity.ReceiveSheet;
import com.lframework.xingyun.sc.entity.ReceiveSheetDetail;
import com.lframework.xingyun.sc.enums.ProductStockBizType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ProductStockPendingCostService extends BaseMpService<ProductStockPendingCost> {

    void create(String scId, String productId, String outBizId, String outBizDetailId,
            ProductStockBizType outBizType, String lotId, BigDecimal pendingNum, LocalDateTime outTime);

    /**
     * 结算挂单成本
     * @param receiveSheet
     * @param detail
     * @param inBizType
     * @return
     */
    ProductStockPendingCostResolveDto settle(ReceiveSheet receiveSheet,
                                             ReceiveSheetDetail detail,
                                             ProductStockBizType inBizType);

    /**
     * 撤销挂单成本
     * @param inBizDetailId
     * @param inBizType
     * @return
     */
    BigDecimal rollback(String inBizDetailId, ProductStockBizType inBizType);
}
