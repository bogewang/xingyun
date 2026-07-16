package com.lframework.xingyun.basedata.service.product;

import com.lframework.starter.web.core.service.BaseMpService;
import com.lframework.xingyun.basedata.entity.ProductUnit;
import java.util.List;

public interface ProductUnitService extends BaseMpService<ProductUnit> {
  List<ProductUnit> getByProductId(String productId);
  List<ProductUnit> getAvailableByProductId(String productId);
  ProductUnit getAvailableById(String productId, String unitId);
  ProductUnit getAvailableByUnitName(String productId, String unitName);
}
