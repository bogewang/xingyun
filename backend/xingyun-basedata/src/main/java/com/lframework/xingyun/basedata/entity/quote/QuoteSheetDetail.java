package com.lframework.xingyun.basedata.entity.quote;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lframework.starter.web.core.dto.BaseDto;
import com.lframework.starter.web.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 报价单明细。
 */
@Data
@TableName("tbl_quote_sheet_detail")
public class QuoteSheetDetail extends BaseEntity implements BaseDto {

  private static final long serialVersionUID = 1L;

  /** ID。 */
  private String id;

  /** 报价单ID。 */
  private String quoteSheetId;

  /** 商品ID。 */
  private String productId;

  /** 报价品名快照。 */
  private String productName;
  /** 报价规格，允许明确留空。 */
  private String spec;
  /** 商品交易单位ID。 */
  private String unitId;
  /** 交易单位名称快照。 */
  private String unitName;
  /** 交易单位换算为基础单位的比例。 */
  private BigDecimal conversionRate;


  /** 商品快照（JSON）。 */
  private String productSnapshot;

  /** 销售单价。 */
  private BigDecimal salePrice;

  /** 是否询价。 */
  private Boolean inquiryProduct;

  /** 明细排序号。 */
  private Integer orderNo;

  /** 创建人ID。 */
  @TableField(fill = FieldFill.INSERT)
  private String createById;

  /** 创建人。 */
  @TableField(fill = FieldFill.INSERT)
  private String createBy;

  /** 创建时间。 */
  @TableField(fill = FieldFill.INSERT)
  private LocalDateTime createTime;

  /** 修改人。 */
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private String updateBy;

  /** 修改人ID。 */
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private String updateById;

  /** 修改时间。 */
  @TableField(fill = FieldFill.INSERT_UPDATE)
  private LocalDateTime updateTime;
}
