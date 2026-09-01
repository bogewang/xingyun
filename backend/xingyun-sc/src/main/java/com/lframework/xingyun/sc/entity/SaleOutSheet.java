package com.lframework.xingyun.sc.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lframework.starter.web.core.dto.BaseDto;
import com.lframework.starter.web.core.entity.BaseEntity;
import com.lframework.xingyun.sc.enums.SaleOutSheetStatus;
import com.lframework.xingyun.sc.enums.SettleStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 *
 * </p>
 *
 * @author zmj
 * @since 2021-10-26
 */
@Data
@TableName("tbl_sale_out_sheet")
public class SaleOutSheet extends BaseEntity implements BaseDto {

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
   * 销售单ID
   */
  private String saleOrderId;

  /**
   * 报价单ID。
   */
  private String quoteSheetId;

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
   * 出库金额
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
   * 总成本成本
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
   * 是否已送货
   */
  private Boolean delivered;

  /**
   * 备注
   */
  private String description;

  /**
   * 创建人ID 新增时赋值
   */
  @TableField(fill = FieldFill.INSERT)
  private String createById;

  /**
   * 创建人 新增时赋值
   */
  @TableField(fill = FieldFill.INSERT)
  private String createBy;

  /**
   * 创建时间 新增时赋值
   */
  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createTime;

  /**
   * 修改人 新增和修改时赋值
   */
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private String updateBy;

  /**
   * 修改人ID 新增和修改时赋值
   */
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private String updateById;

  /**
   * 修改时间 新增和修改时赋值
   */
  @TableField(fill = FieldFill.INSERT_UPDATE)
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
   * 结算状态
   */
  private SettleStatus settleStatus;

  /**
   * 结算版本号
   */
  private Long settleVersion;

  /**
   * 结算交易占用ID
   */
  private String txId;
}
