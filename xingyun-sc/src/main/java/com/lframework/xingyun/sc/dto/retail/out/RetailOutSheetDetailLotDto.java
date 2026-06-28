package com.lframework.xingyun.sc.dto.retail.out;

import com.lframework.starter.web.core.dto.BaseDto;
import com.lframework.xingyun.sc.enums.SettleStatus;
import com.lframework.xingyun.sc.enums.StockCostStatus;
import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class RetailOutSheetDetailLotDto implements BaseDto, Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * ID
   */
  private String id;

  /**
   * 明细ID
   */
  private String detailId;

  /**
   * 出库数量
   */
  private BigDecimal orderNum;

  /**
   * 已退货数量
   */
  private BigDecimal returnNum;

  /**
   * 含税成本金额
   */
  private BigDecimal costTaxAmount;

  /**
   * 已回算数量
   */
  private BigDecimal settledCostNum;

  /**
   * 成本状态
   */
  private StockCostStatus costStatus;

  /**
   * 结算状态
   */
  private SettleStatus settleStatus;

  /**
   * 排序编号
   */
  private Integer orderNo;
}
