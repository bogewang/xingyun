package com.lframework.xingyun.basedata.enums.quote;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.lframework.starter.web.core.enums.BaseEnum;

/**
 * 报价单状态。
 */
public enum QuoteSheetStatus implements BaseEnum<Integer> {
  DISABLED(0, "停用"), ENABLED(1, "启用");

  @EnumValue
  private final Integer code;

  private final String desc;

  QuoteSheetStatus(Integer code, String desc) {

    this.code = code;
    this.desc = desc;
  }

  /**
   * 获取状态编码。
   *
   * @return 状态编码
   */
  @Override
  public Integer getCode() {

    return this.code;
  }

  /**
   * 获取状态描述。
   *
   * @return 状态描述
   */
  @Override
  public String getDesc() {

    return this.desc;
  }
}
