package com.lframework.xingyun.sc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lframework.starter.web.core.dto.BaseDto;
import com.lframework.starter.web.core.entity.BaseEntity;
import com.lframework.xingyun.sc.enums.ProductStockBizType;
import com.lframework.xingyun.sc.enums.ProductStockPendingCostStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("tbl_product_stock_pending_cost")
public class ProductStockPendingCost extends BaseEntity implements BaseDto {

  private static final long serialVersionUID = 1L;

  private String id;

  private String scId;

  private String productId;

  private String outBizId;

  private String outBizDetailId;

  private ProductStockBizType outBizType;

  private String lotId;

  private LocalDateTime outTime;

  private BigDecimal pendingNum;

  private BigDecimal settledNum;

  private BigDecimal settledTaxAmount;

  private ProductStockPendingCostStatus status;

  public BigDecimal getSettledNum() {
    return settledNum == null ? BigDecimal.ZERO : settledNum;
  }
}
