package com.lframework.xingyun.sc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lframework.starter.web.core.dto.BaseDto;
import com.lframework.starter.web.core.entity.BaseEntity;
import com.lframework.xingyun.sc.enums.SettleStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * <p>
 *
 * </p>
 *
 * @author zmj
 * @since 2021-10-26
 */
@Data
@TableName("tbl_sale_out_sheet_detail")
public class SaleOutSheetDetail extends BaseEntity implements BaseDto {

  private static final long serialVersionUID = 1L;

  /**
   * ID
   */
  private String id;

  /**
   * 出库单ID
   */
  private String sheetId;

  /**
   * 商品ID
   */
  private String productId;

  /**
   * 出库数量(主单位数量)
   */
  private BigDecimal orderNum;

  /**
   * 验收数量，使用交易单位
   */
  private BigDecimal confirmNum;

  /** 交易单位ID及快照；orderNum 始终为主单位数量。 */
  private String unitId;
  private String unitName;
  private BigDecimal conversionRate;
  private BigDecimal businessNum;

  /**
   * 原价
   */
  private BigDecimal oriPrice;

  /**
   * 现价
   */
  private BigDecimal taxPrice;

  /**
   * 验收金额，根据验收数量和销售单价计算
   */
  private BigDecimal confirmAmt;

  /**
   * 折扣率（%）
   */
  private BigDecimal discountRate;

  /**
   * 是否赠品
   */
  private Boolean isGift;

  /**
   * 税率（%）
   */
  private BigDecimal taxRate;

  /**
   * 备注
   */
  private String description;

  /**
   * 排序编号
   */
  private Integer orderNo;

  /**
   * 结算状态
   */
  private SettleStatus settleStatus;

  /**
   * 销售订单明细ID
   */
  private String saleOrderDetailId;

  /**
   * 已退货数量
   */
  private BigDecimal returnNum;

  /**
   * 组合商品原始明细ID
   */
  private String oriBundleDetailId;

  /**
   * 总金额
   */
  private BigDecimal taxAmount;

  /**
   * 成本单价
   */
  private BigDecimal costPrice;

  /**
   * 是否手动录入成本
   */
  @Deprecated
  private Boolean manualInputCost;

  /**
   * 总利润
   */
  private BigDecimal totalProfit;

  /**
   * 实际日期
   */
  private LocalDate actualDate;

  /**
   * 计划日期
   */
  private LocalDate planDate;

  /**
   * 供应商ID
   */
  private String supplierId;
}
