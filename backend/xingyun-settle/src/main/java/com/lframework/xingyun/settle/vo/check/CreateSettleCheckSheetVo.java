package com.lframework.xingyun.settle.vo.check;

import com.lframework.starter.common.exceptions.impl.InputErrorException;
import com.lframework.starter.common.utils.Assert;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.vo.BaseVo;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateSettleCheckSheetVo implements BaseVo, Serializable {

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
  private List<SettleCheckSheetItemVo> items;

  /**
   * 对账金额
   */
  @ApiModelProperty(value = "对账金额", required = true)
  @NotNull(message = "对账金额不能为空！")
  private BigDecimal checkAmt;

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

  public void validate() {

    Assert.notNull(checkAmt, "对账金额不能为空！");
    Assert.isTrue(CollectionUtils.isNotEmpty(items), "请选择单据！");

    int orderNo = 1;
    for (SettleCheckSheetItemVo item : this.items) {
      if (StringUtil.isBlank(item.getId())) {
        throw new InputErrorException("第" + orderNo + "行业务单据不能为空！");
      }

      if (item.getBizType() == null) {
        throw new InputErrorException("第" + orderNo + "行业务类型不能为空！");
      }

      // if (item.getPayAmount() == null && this.totalPayAmount == null) {
      //   throw new InputErrorException("第" + orderNo + "行应付金额不能为空！");
      // }

      orderNo++;
    }
  }
}
