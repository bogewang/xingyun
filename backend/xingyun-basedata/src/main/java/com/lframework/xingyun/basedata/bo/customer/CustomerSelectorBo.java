package com.lframework.xingyun.basedata.bo.customer;

import com.lframework.starter.web.core.bo.BaseBo;
import com.lframework.xingyun.basedata.entity.Customer;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 客户选择器返回数据。
 */
@Data
public class CustomerSelectorBo extends BaseBo<Customer> {

  /**
   * 客户ID
   */
  @ApiModelProperty("客户ID")
  private String value;

  /**
   * 客户名称
   */
  @ApiModelProperty("客户名称")
  private String label;

  /**
   * 客户备注
   */
  @ApiModelProperty("客户备注")
  private String description;

  /**
   * 构造客户选择器返回数据。
   *
   * @param customer 客户数据
   */
  public CustomerSelectorBo(Customer customer) {
    super(customer);
    this.value = customer.getId();
    this.label = customer.getName();
    this.description = customer.getDescription();
  }
}
