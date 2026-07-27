package com.lframework.xingyun.settle.vo.sheet;

import com.lframework.starter.common.exceptions.impl.InputErrorException;
import com.lframework.starter.common.utils.Assert;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.vo.BaseVo;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

@Data
public class CreateSettleSheetVo implements BaseVo, Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * 供应商ID
   */
  @ApiModelProperty(value = "供应商ID", required = true)
  @NotNull(message = "供应商ID不能为空！")
  private String supplierId;

  /**
   * 项目
   */
  @ApiModelProperty(value = "项目", required = true)
  @NotEmpty(message = "项目不能为空！")
  private List<SettleSheetItemVo> items;

  /**
   * 起始日期
   */
  @ApiModelProperty(value = "起始日期")
  // @NotNull(message = "起始日期不能为空！")
  private LocalDate startDate;

  /**
   * 截止日期
   */
  @ApiModelProperty(value = "截止日期")
  // @NotNull(message = "截止日期不能为空！")
  private LocalDate endDate;

  /**
   * 备注
   */
  @ApiModelProperty("备注")
  private String description;

  /**
   * 结算金额
   */
  @ApiModelProperty(value = "结算金额", required = true)
  @NotNull(message = "结算金额不能为空！")
  private BigDecimal settleAmount;

  /**
   * 结算状态：1-部分结算，3-已结算
   */
  @ApiModelProperty("结算状态：1-部分结算，3-已结算")
  private Integer settleStatus;

  public void validate() {

    Assert.notNull(settleAmount, "结算金额不能为空！");
    Assert.isTrue(CollectionUtils.isNotEmpty(items), "请选择单据！");

    int orderNo = 1;
    for (SettleSheetItemVo item : this.items) {
      if (StringUtil.isBlank(item.getId())) {
        throw new InputErrorException("第" + orderNo + "行结算单不能为空！");
      }

      if (item.getUnSettleAmount() == null) {
        throw new InputErrorException("第" + orderNo + "行未结算不能为空！");
      }

      // if (item.getDiscountAmount() == null) {
      //   throw new InputErrorException("第" + orderNo + "行优惠金额不能为空！");
      // }

      orderNo++;
    }
  }
}
