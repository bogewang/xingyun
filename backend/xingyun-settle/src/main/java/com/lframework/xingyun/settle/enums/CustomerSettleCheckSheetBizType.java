package com.lframework.xingyun.settle.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.lframework.starter.web.core.enums.BaseEnum;

/**
 * 客户对账业务类型。
 */
public enum CustomerSettleCheckSheetBizType implements BaseEnum<Integer> {
  OUT_SHEET(1, "销售出库单"), SALE_RETURN(2, "销售退货单");

  @EnumValue
  private final Integer code;

  private final String desc;

  /**
   * 创建客户对账业务类型。
   *
   * @param code 业务类型编码
   * @param desc 业务类型描述
   */
  CustomerSettleCheckSheetBizType(Integer code, String desc) {
    this.code = code;
    this.desc = desc;
  }

  /**
   * 获取业务类型编码。
   *
   * @return 业务类型编码
   */
  @Override
  public Integer getCode() {
    return code;
  }

  /**
   * 获取业务类型描述。
   *
   * @return 业务类型描述
   */
  @Override
  public String getDesc() {
    return desc;
  }
}
