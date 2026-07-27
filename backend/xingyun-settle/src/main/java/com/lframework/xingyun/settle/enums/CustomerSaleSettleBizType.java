package com.lframework.xingyun.settle.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.lframework.starter.web.core.enums.BaseEnum;

/**
 * 客户销售结算业务类型。
 */
public enum CustomerSaleSettleBizType implements BaseEnum<Integer> {
  OUT_SHEET(1, "销售出库单"), SALE_RETURN(2, "销售退单");

  @EnumValue
  private final Integer code;

  private final String desc;

  CustomerSaleSettleBizType(Integer code, String desc) {

    this.code = code;
    this.desc = desc;
  }

  @Override
  public Integer getCode() {

    return this.code;
  }

  @Override
  public String getDesc() {

    return this.desc;
  }
}
