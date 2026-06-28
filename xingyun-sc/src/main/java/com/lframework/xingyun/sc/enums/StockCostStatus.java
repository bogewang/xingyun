package com.lframework.xingyun.sc.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.lframework.starter.web.core.enums.BaseEnum;

public enum StockCostStatus implements BaseEnum<Integer> {
  PENDING(0, "待回算"), PARTIAL(1, "部分回算"), FINAL(2, "已回算");

  @EnumValue
  private final Integer code;

  private final String desc;

  StockCostStatus(Integer code, String desc) {

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
