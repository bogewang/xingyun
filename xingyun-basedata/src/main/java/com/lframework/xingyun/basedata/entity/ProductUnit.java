package com.lframework.xingyun.basedata.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lframework.starter.web.core.dto.BaseDto;
import com.lframework.starter.web.core.entity.BaseEntity;
import java.math.BigDecimal;
import lombok.Data;

/** 商品计量单位；库存始终以主单位（换算率为 1）核算。 */
@Data
@TableName("base_data_product_unit")
public class ProductUnit extends BaseEntity implements BaseDto {

  private String id;
  private String productId;
  private String unitName;
  private BigDecimal conversionRate;
  private Boolean baseUnit;
  private Boolean available;
  private Integer sortNo;
}
