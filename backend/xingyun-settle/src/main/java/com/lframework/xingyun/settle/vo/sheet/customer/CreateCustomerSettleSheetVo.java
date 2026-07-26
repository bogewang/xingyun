package com.lframework.xingyun.settle.vo.sheet.customer;

import com.lframework.starter.common.exceptions.impl.DefaultClientException;
import com.lframework.starter.common.utils.StringUtil;
import com.lframework.starter.web.core.vo.BaseVo;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCustomerSettleSheetVo implements BaseVo, Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * 客户ID
   */
  @ApiModelProperty(value = "客户ID", required = true)
  @NotNull(message = "客户ID不能为空！")
  private String customerId;

  /**
   * 项目
   */
  @ApiModelProperty(value = "项目", required = true)
  @NotEmpty(message = "项目不能为空！")
  private List<CustomerSettleSheetItemVo> items;

  /**
   * 确认结算金额。
   */
  @ApiModelProperty(value = "确认结算金额", required = true)
  @NotNull(message = "确认结算金额不能为空！")
  private BigDecimal settleAmount;

  /**
   * 备注
   */
  @ApiModelProperty("备注")
  private String description;

  public void validate() {

    int orderNo = 1;
    for (CustomerSettleSheetItemVo item : this.items) {
      if (StringUtil.isBlank(item.getBizId())) {
        throw new DefaultClientException("第" + orderNo + "行业务单据不能为空！");
      }

      if (item.getBizType() == null || (item.getBizType() != 1 && item.getBizType() != 2)) {
        throw new DefaultClientException("第" + orderNo + "行业务类型不正确！");
      }

      orderNo++;
    }
  }
}
