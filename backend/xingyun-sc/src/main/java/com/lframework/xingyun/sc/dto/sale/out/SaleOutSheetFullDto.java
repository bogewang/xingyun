package com.lframework.xingyun.sc.dto.sale.out;

import com.lframework.starter.web.core.dto.BaseDto;
import com.lframework.xingyun.sc.enums.SaleOutSheetStatus;
import com.lframework.xingyun.sc.enums.SettleStatus;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SaleOutSheetFullDto implements BaseDto, Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * ID
   */
  private String id;

  /**
   * 单号
   */
  private String code;

  /**
   * 仓库ID
   */
  private String scId;

  /**
   * 客户ID
   */
  private String customerId;

  /**
   * 销售员ID
   */
  private String salerId;

  /**
   * 订单日期
   */
  private LocalDate orderDate;

  /**
   * 付款日期
   */
  private LocalDate paymentDate;

  /**
   * 商品数量
   */
  private BigDecimal totalNum;

  /**
   * 验收数量，使用交易单位
   */
  private BigDecimal confirmNum;

  /**
   * 赠品数量
   */
  private BigDecimal totalGiftNum;

  /**
   * 出库总金额
   */
  private BigDecimal totalAmount;

  /**
   * 验收金额，根据验收数量和销售单价计算
   */
  private BigDecimal confirmAmt;

  /**
   * 已支付金额
   */
  private BigDecimal paidAmount;

  /**
   * 总成本
   */
  private BigDecimal totalCost;

  /**
   * 总利润
   */
  private BigDecimal totalProfit;

  /**
   * 是否录完所有成本
   */
  private Boolean fillAllCost;

  /**
   * 备注
   */
  private String description;

  /**
   * 创建人
   */
  private String createBy;

  /**
   * 创建时间
   */
  private LocalDateTime createTime;

  /**
   * 修改人
   */
  private String updateBy;

  /**
   * 修改时间
   */
  private LocalDateTime updateTime;

  /**
   * 审核人
   */
  private String approveBy;

  /**
   * 审核时间
   */
  private LocalDateTime approveTime;

  /**
   * 状态
   */
  private SaleOutSheetStatus status;

  /**
   * 拒绝原因
   */
  private String refuseReason;

  /**
   * 销售订单ID
   */
  private String saleOrderId;

  /**
   * 结算状态
   */
  private SettleStatus settleStatus;

  /**
   * 订单明细
   */
  private List<SheetDetailDto> details;

  @Data
  public static class SheetDetailDto implements BaseDto, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 明细ID
     */
    private String id;

    /**
     * 组合商品ID
     */
    private String mainProductId;

    /**
     * 商品ID
     */
    private String productId;

    /**
     * 出库数量
     */
    private BigDecimal orderNum;

    /**
     * 验收数量，使用交易单位
     */
    private BigDecimal confirmNum;
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
     * 折扣（%）
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
    private Boolean manualInputCost;

    /**
     * 总利润
     */
    private BigDecimal totalProfit;

    /**
     * 商品分类名称
     */
    private String categoryName;
  }
}
