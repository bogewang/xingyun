package com.lframework.xingyun.sc.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.lframework.starter.web.core.enums.BaseEnum;

public enum ProductStockPendingCostStatus implements BaseEnum<Integer> {
  PENDING(0, "待回算"), PARTIAL(1, "部分回算"), FINISHED(2, "已完成");

  @EnumValue
  private final Integer code;

  private final String desc;

  ProductStockPendingCostStatus(Integer code, String desc) {

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
