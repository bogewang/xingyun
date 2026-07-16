package com.lframework.xingyun.sc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lframework.starter.web.core.dto.BaseDto;
import com.lframework.starter.web.core.entity.BaseEntity;
import com.lframework.xingyun.sc.enums.ProductStockBizType;
import java.math.BigDecimal;
import lombok.Data;

@Data
@TableName("tbl_product_stock_pending_cost_settle")
public class ProductStockPendingCostSettle extends BaseEntity implements BaseDto {

  private static final long serialVersionUID = 1L;

  private String id;

  private String pendingId;

  private String inBizId;

  private String inBizDetailId;

  private ProductStockBizType inBizType;

  private BigDecimal settleNum;

  private BigDecimal settleTaxAmount;
}
