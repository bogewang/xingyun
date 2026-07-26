package com.lframework.xingyun.settle.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.lframework.starter.web.core.enums.BaseEnum;

/**
 * 客户对账单状态。
 */
public enum CustomerSettleCheckSheetStatus implements BaseEnum<Integer> {
  CONFIRMED(3, "已确认");

  @EnumValue
  private final Integer code;

  private final String desc;

  /**
   * 创建客户对账单状态。
   *
   * @param code 状态编码
   * @param desc 状态描述
   */
  CustomerSettleCheckSheetStatus(Integer code, String desc) {
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
    return code;
  }

  /**
   * 获取状态描述。
   *
   * @return 状态描述
   */
  @Override
  public String getDesc() {
    return desc;
  }
}
